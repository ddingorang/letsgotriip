리뷰만 했고 코드 수정은 없습니다. 심각도순입니다.

1. [high] 대화 메모리가 사용자별로 격리되지 않음  
   근거: `AssistantController.java:34-36`, `AssistantService.java:78-80`  
   왜: `conversationId`가 클라이언트 입력 그대로이고 `ChatMemory` 키도 `conversationId`뿐이다. ID가 유출/재사용되면 다른 사용자의 대화 맥락이 섞일 수 있다.  
   수정: 메모리 키를 `userId:conversationId`로 네임스페이스화하고, 서버 발급 conversation만 허용/소유권 검증.

2. [high] 문서 삭제 실패 시 벡터가 잔존해 삭제한 문서를 계속 RAG가 참조함  
   근거: `AssistantService.java:69-76`, `DocumentService.java:92-99`, `IngestionService.java:67-75`  
   왜: 벡터 삭제 실패를 `IngestionService`가 삼키고, DB 문서는 삭제된다. 이후 RAG 검색은 `userId`만 필터링하므로 삭제된 문서 청크가 답변에 남는다.  
   수정: 벡터 삭제 실패를 상위로 전파하거나 outbox/retry/tombstone 처리. 삭제 완료 전까지 문서 상태와 벡터 상태를 분리 관리.

3. [high] RAG 문서 프롬프트 인젝션이 상태 변경 Tool 호출로 이어질 수 있음  
   근거: `AssistantService.java:82-87`, `AssistantService.java:132-158`  
   왜: 사용자 문서를 RAG 컨텍스트로 넣는 같은 호출에 `createTravelPlan` Tool을 노출하고, Tool이 곧바로 `savePlan()`까지 실행한다. 악성 문서가 “일정 생성 도구 호출”을 유도하면 영속 데이터가 생성된다.  
   수정: Tool은 초안 생성까지만 허용하고 저장은 별도 사용자 확인 API로 분리. 상태 변경 Tool은 RAG 문서 지시와 분리하거나 서버 측 intent 검증 추가.

4. [high] PDF/텍스트 추출 실패가 성공(INGESTED)으로 저장됨  
   근거: `DocumentService.java:57-74`, `DocumentService.java:143-152`  
   왜: PDF 리더 실패, 깨진 PDF, non-UTF-8 텍스트는 `extractText()`가 `null`로 삼키고 `markIngested(0)` 처리된다. 사용자는 완료로 보지만 실제 RAG에는 아무것도 안 들어간다.  
   수정: 추출 실패와 “지원 안 하는 타입”을 구분해 `FAILED` 또는 `UNSUPPORTED`로 저장하고 클라이언트에 실패 사유 노출.

5. [med] 업로드 타입 검증이 사실상 없음  
   근거: `DocumentController.java:53-59`, `DocumentService.java:104-117`, `DocumentService.java:148`, `WebConfig.java:42-45`  
   왜: 서버는 빈 파일/20MB만 검사하고 `OTHER`도 저장한다. 문서 저장 루트는 `/uploads/**`로 공개 매핑되어 있어 불필요한 임의 파일 저장면이 열린다.  
   수정: 서버에서 PDF/TXT 등 실제 지원 타입만 whitelist. RAG 문서는 공개 정적 루트와 분리해 private storage 사용.

6. [med] RAG/LLM 장애가 전부 500으로 전파됨  
   근거: `AssistantService.java:82-89`, `GlobalExceptionHandler.java:167-179`, `application.yaml:67-79`  
   왜: VectorStore/OpenAI 호출에 도메인별 catch, fallback, timeout 분류가 없다. Redis Vector나 OpenAI 장애가 챗봇 전체 500으로 보인다.  
   수정: Spring AI/Redis 예외를 502/503으로 매핑하고, timeout 설정 및 “RAG 없이 답변” 같은 제한 fallback을 명시.

7. [med] 재인덱싱이 기존 벡터를 지우지 않아 중복/ stale 청크가 누적됨  
   근거: `UserDataIndexer.java:33-44`, `UserDataIndexer.java:51-59`, `IngestionService.java:58`  
   왜: 주석은 재인덱싱이지만 실제로는 `vectorStore.add()`만 한다. 계획 수정 후 재색인하면 예전 계획 내용도 검색될 수 있다.  
   수정: `ingest` 전 `deleteByDoc()` 실행 또는 deterministic chunk id 기반 upsert.

8. [med] 여행계획 인덱싱 N+1 가능성  
   근거: `PlanRepository.java:27`, `UserDataIndexer.java:37-44`, `UserDataIndexer.java:75-90`  
   왜: plan 목록만 조회한 뒤 lazy `days`, `places`, `attraction`을 루프에서 접근한다. 최대 50개 계획이면 쿼리가 급증한다.  
   수정: 인덱싱 전용 fetch join/entity graph/projection 쿼리 추가.

9. [med] 파일명 검증 누락으로 500 및 파일 찌꺼기 가능  
   근거: `DocumentController.java:53-59`, `DocumentService.java:47-55`, `TripDocument.java:24-25`  
   왜: `originalFilename` null/blank/255자 초과를 막지 않고 DB `filename nullable=false length=255`에 저장한다. DB 실패 전 파일은 이미 저장된다.  
   수정: 파일명 nonblank/길이 검증, 안전한 표시명 fallback, DB 저장 후 파일 저장 실패 보상 처리.

10. [low] FE가 `PENDING`을 완료로 표시함  
   근거: `DocumentStatus.java:3-7`, `DocumentsView.vue:148-162`  
   왜: BE 상태는 `PENDING/INGESTED/FAILED`인데 FE는 `PENDING`을 별도 처리하지 않아 default “완료”로 표시한다.  
   수정: `PENDING -> 처리 중`, `INGESTED -> 완료`, `FAILED -> 실패`로 정확히 매핑.

테스트는 실행하지 않았습니다. 요청이 리뷰 전용이고 현재 세션도 읽기 전용이라 파일 생성/빌드 산출물을 만들지 않았습니다.