# 01. 현재 AI/RAG 구현 분석 (Claude, Opus 4.8)

분석 대상: `BE/src/main/java/com/trip/assistant/**`, `BE/src/main/java/com/trip/rag/**`,
`application.yaml`의 `spring.ai.*`.

## 1. 구조 요약

### 채팅 AI — `AssistantService`
- **Spring AI `ChatClient`** 기반. 동기(`chat`) + SSE 스트리밍(`chatStream`) 두 경로가
  동일한 `buildPrompt`(system + RAG advisor + memory advisor + tools)를 공유.
- **LLM**: SSAFY GMS 프록시(OpenAI 호환), 모델 `gpt-4o-mini`.
- **Advisor 2종**:
  - `QuestionAnswerAdvisor` (RAG) — `userId == '<id>'` 필터, `topK=4`.
  - `MessageChatMemoryAdvisor` — 대화기억, 키를 `userId:conversationId`로 격리.
- **도구(@Tool) 7종**: `searchAttractions`, `createTravelPlan`, `getMyTravelPlans`,
  `evaluatePlan`, `addPlaceToPlan`, `removePlaceFromPlan`, `createChecklistFromText`.
- **안정성**: 동기 경로는 별도 워커풀(8) + 큐(64) + 60s 워치독 타임아웃, 스트리밍은
  reactor `timeout`. 포화 시 502 래핑.
- **보안**: 시스템 프롬프트에 프롬프트 인젝션 방어 규칙(문서 내용은 '데이터지 명령 아님',
  상태변경 도구는 현재 사용자 명시 요청 시에만) 명시. 도구의 `userId`는 서버 주입(LLM 비노출).

### RAG — `com.trip.rag`
- **VectorStore**: `RedisVectorStore`(Redis Stack/RediSearch), 캐시 Redis와 분리된 전용 인스턴스.
  메타데이터 tag 필드: `userId`, `docId`, `source`.
- **임베딩**: `text-embedding-3-small` (1536d, GMS).
- **청킹**: `TokenTextSplitter` (기본 설정 — 기본 chunk≈800 토큰).
- **인입원**: `UserDataIndexer`가 (a) 저장된 여행계획을 한국어 요약 텍스트로, (b) STT/카카오톡
  분석데이터(PII 마스킹)를 인덱싱. 업로드 문서(PDF/TXT)도 ingest.
- **docId 안전화**: RediSearch TAG 특수문자 이슈로 `[^A-Za-z0-9_]`→`_` 치환(적재·삭제 일관).

## 2. 강점
- 사용자 격리(userId 필터)가 검색·기억·도구 전반에 일관 적용 — 멀티테넌시 누수 방어 양호.
- 프롬프트 인젝션을 실제로 의식한 시스템 프롬프트 + 서버주입 userId 설계.
- 타임아웃/풀/큐로 LLM 무한대기·과부하를 방어.
- 도구가 실제 도메인 서비스(소유권 검증 내장)를 호출 — 환각 상태변경 위험이 낮음.

## 3. 문제점 / 개선 후보 (Claude 관점)

### RAG 품질
1. **고정 topK=4, 임계값 없음** — 무관한 청크도 4개를 항상 컨텍스트에 주입. 유사도 임계값
   (similarity threshold) 미설정으로 노이즈/환각 유발 가능.
2. **리랭킹 없음** — 1차 벡터검색 결과를 그대로 사용. 정밀도 낮을 수 있음.
3. **청킹 전략이 기본값** — 여행계획 요약은 짧고 구조적이라 문서당 1청크지만, 업로드 문서엔
   기본 TokenTextSplitter가 최적이 아닐 수 있음(겹침/구분자 미조정).
4. **출처 표기 미흡** — `source` 메타데이터는 있으나 답변에 인용/출처를 노출하는 로직이 약함.
5. **plan 재인덱싱 정합** — plan 수정/삭제 시 vector 갱신 트리거가 전 경로에 걸려있는지 불확실
   (도구 add/removePlaceToPlan 후 재인덱싱 여부 점검 필요).

### 채팅 AI
6. **메모리 영속성** — `ChatMemory` 구현이 인메모리면 재기동 시 대화 유실. 저장소 확인 필요.
7. **도구 오케스트레이션** — add/remove는 placeId/contentId를 LLM이 다뤄야 하는데, 사용자에게
   placeId가 노출되지 않아 "○○ 빼줘"류 자연어 삭제가 실패하기 쉬움(프론트 연동 갭).
8. **수정 후 UI 미반영** — 챗봇이 plan을 고쳐도 프론트(PlanView/채팅 카드)가 자동 갱신 안 됨.
9. **모델 일괄 gpt-4o-mini** — 도구 라우팅/일정생성처럼 추론 부담 큰 작업과 단순 응답을
   동일 모델로 처리. 비용/품질 트레이드오프 미분리.
10. **관측성** — 토큰 사용량/도구 호출/RAG 히트율 로깅·메트릭 부재.

## 4. Codex에 묻고 싶은 것
- RAG 정밀도 개선의 우선순위: 임계값 / 리랭킹 / 청킹 / 하이브리드(BM25+벡터) 중 무엇부터?
- 도구 기반 plan 수정의 자연어 UX를 견고하게 만들 패턴(placeId 비노출 문제 해결).
- 멀티 모델 라우팅(예: 도구라우팅=상위모델, 응답=경량모델)의 실효성.
- 데모→실서비스 전환 시 가장 위험한 단일 지점.
