요청 범위만 리뷰했습니다. 수정 없음.

1. [high] 문서 삭제 후 RAG 벡터가 남아 개인정보가 계속 검색될 수 있음  
왜: 벡터 삭제 실패를 삼키고 DB/파일 삭제를 계속 진행합니다. 삭제된 문서 내용이 Assistant RAG에 남을 수 있습니다.  
근거: `BE/src/main/java/com/trip/rag/IngestionService.java:67`, `:71`, `:73-75`, `BE/src/main/java/com/trip/document/service/DocumentService.java:143`, `:149`, `:153-154`, `BE/src/main/java/com/trip/assistant/AssistantService.java:184-190`  
수정: `deleteByDoc` 실패를 전파하거나 outbox/retry/tombstone으로 삭제 완료 전까지 문서를 삭제 완료 처리하지 마세요.

2. [high] STT/카톡 원본 파일이 마스킹 없이 디스크에 남음  
왜: DB에는 마스킹된 `rawText`를 저장하지만, 원본 업로드 파일은 `temp_uploads`에 저장 후 삭제하지 않습니다. 통화·카톡 원문 PII가 그대로 잔존합니다.  
근거: `BE/src/main/java/com/trip/preprocessing/service/PreprocessingService.java:38`, `:59-61`, `:66-72`, `:80`, `:90`, `:74`  
수정: 처리 성공/실패 모두에서 원본 파일 삭제 또는 암호화·보존기간·접근제어가 있는 저장소로 분리하세요.

3. [high] `IOException` 발생 시 분석 row가 rollback되지 않을 수 있음  
왜: `@Transactional` 기본 rollback 대상은 unchecked 예외입니다. 카카오 파일 UTF-8 읽기 실패 같은 `IOException`이 `save()` 이후 발생하면 `rawText` 없는 `UserAnalysisData`가 커밋될 수 있습니다.  
근거: `BE/src/main/java/com/trip/preprocessing/service/PreprocessingService.java:52-53`, `:70`, `:72`, `:77-79`, `BE/src/main/java/com/trip/preprocessing/controller/PreprocessingController.java:28`, `:37`  
수정: `@Transactional(rollbackFor = IOException.class)` 또는 `IOException`을 `GeneralException` 같은 런타임 예외로 변환하세요.

4. [high] 외부 STT/LLM/RAG 호출을 DB 트랜잭션 안에서 실행함  
왜: Whisper, vector ingest, ChatClient 호출 동안 DB 트랜잭션이 열린 상태입니다. 지연 시 커넥션 점유가 길고, 커밋 전 외부 색인 성공 후 DB 커밋 실패 시 RAG orphan이 생깁니다. FE timeout도 BE 최악 시간보다 짧아 재시도 중복이 가능합니다.  
근거: `PreprocessingService.java:52`, `:82`, `:99`, `:108`, `DocumentService.java:47`, `:103`, `:120`, `WhisperSTTManager.java:28-30`, `frontend/src/api/index.js:426-428`, `:513-515`  
수정: 짧은 TX로 `PENDING` 저장 후 AFTER_COMMIT/outbox worker에서 STT·색인·취향병합을 수행하고 idempotency key/status polling을 두세요.

5. [medium] `preprocessing` 업로드 입력검증이 없음  
왜: 빈 파일, 잘못된 타입, 확장자, magic byte를 서버에서 검사하지 않고 바로 저장/읽기/STT로 보냅니다. 사용자 입력 오류가 500 또는 성공 `dataId`로 오염됩니다.  
근거: `PreprocessingController.java:24-39`, `PreprocessingService.java:59-61`, `:79`, `:82`, `application.yaml:8-10`  
수정: `file.isEmpty`, 크기, MIME, 확장자, magic byte를 endpoint별로 검증하고 400/413/415/422로 분리하세요.

6. [medium] 문서 텍스트 판정이 UTF-8 검증 없이 성공 처리됨  
왜: `isText()`는 NUL/제어문자만 보고 true를 반환합니다. 이후 UTF-8 읽기 실패는 `null`로 삼켜지고 `INGESTED(0)`으로 성공 처리됩니다.  
근거: `DocumentService.java:170-176`, `:277-290`, `:348-357`, `:85-87`  
수정: `CharsetDecoder`를 `REPORT`로 실제 UTF-8 검증하고, 추출 실패는 `FAILED` 또는 400/422로 처리하세요.

7. [medium] 오디오 문서 판정이 헤더/확장자만으로 public 업로드 루트에 저장됨  
왜: magic byte 실패 후에도 `Content-Type: audio/*` 또는 `.mp3` 등 확장자만 맞으면 AUDIO로 저장됩니다. 저장 위치는 `/uploads/**` 공개 정적 루트입니다.  
근거: `DocumentService.java:189-198`, `:293-302`, `WebConfig.java:42-45`, `SecurityConfig.java:88-89`  
수정: 실제 오디오 디코딩/ffprobe 등으로 검증하고, 사용자 문서는 공개 정적 루트가 아닌 인증된 다운로드 경로에 두세요.

8. [medium] 취향 병합 lost update 가능  
왜: 동시 업로드 2건이 같은 기존 `preferredInterests`를 읽고 각각 합집합 저장하면 마지막 커밋이 앞선 추가분을 덮을 수 있습니다. 락/버전이 없습니다.  
근거: `UserService.java:101-122`, `:160`, `UserRepository.java:8-11`, `User.java:23`, `:62-63`, `:141-143`  
수정: optimistic `@Version`, pessimistic lock query, 또는 DB 단위 upsert/정규화된 preference 테이블로 병합하세요.

9. [medium] 문서 STT 실패를 HTTP 성공처럼 FE가 처리함  
왜: BE는 전사 실패 시 `FAILED` 문서를 반환하지만 controller는 항상 `201 Created`, FE는 상태 확인 없이 “업로드했어요”를 표시합니다.  
근거: `DocumentService.java:103-108`, `:112-116`, `DocumentController.java:33-34`, `frontend/src/views/DocumentsView.vue:116-117`  
수정: 실패 응답은 4xx/5xx로 반환하거나 FE가 `status === FAILED`면 성공 토스트 대신 실패 메시지를 표시하세요.