# Triip AI 스택 전략 (Strategy Review)

> 작성일: 2026-06-19 · 범위: AI 추천 / RAG 챗봇 / STT·사용자데이터
> 성격: 읽기 전용 분석 + 구현 계획(코드 미수정). file:line은 현재 워크트리 기준.

---

## 0. 현재 구성 인벤토리 (코드 기준)

| 컴포넌트 | 핵심 클래스 | 모델/저장소 | 상태 |
|---|---|---|---|
| AI 추천 | `recommend/service/RecommendService.java` | `gpt-4o-mini` (`MODEL_NAME` L71), TourAPI+festivals 후보 → ChatClient → `BeanOutputConverter<ItineraryDraft>` → 검증 → DB | 동작. 동기 1-shot. |
| RAG 챗봇 | `assistant/AssistantService.java`, `assistant/AssistantConfig.java` | ChatClient + `QuestionAnswerAdvisor`(userId 필터) + `MessageChatMemoryAdvisor`(in-memory 20개) + `@Tool` 2종 | 동작. 비스트리밍. |
| 벡터스토어 | `rag/RagConfig.java`, `rag/IngestionService.java`, `rag/UserDataIndexer.java` | RedisVectorStore(:6380), `text-embedding-3-small`(1536), `TokenTextSplitter` 기본값 | **문서만 실적재. 플랜 인덱서는 호출처 없음(데드코드).** |
| 문서 업로드 | `document/service/DocumentService.java` | PDF(`PagePdfDocumentReader`)/TEXT 추출 → ingest | 동작. IMAGE/OCR 미구현(L148). |
| STT/사용자데이터 | `preprocessing/service/PreprocessingService.java`, `client/impl/WhisperSTTManager.java` | Whisper(`whisper-1`, lang=ko) / KakaoTalk txt → `UserAnalysisData.rawText` + PII 마스킹 | **저장만 하고 어디서도 읽지 않음(데드엔드).** |
| FE | `frontend/src/views/AssistantView.vue`, `stores/assistant.js`, `views/AnalysisUploadView.vue`, `api/index.js` | 단발 POST chat / 멀티파트 업로드 | 동작. |

### 즉시 드러난 3대 구조적 공백 (근거)
1. **STT/Kakao 분석 데이터가 죽은 데이터다.** `UserAnalysisData.rawText`는 `PreprocessingService`(쓰기)·`UserProfileResponseDto`·`RepositoryConfig` 외 어떤 추천/챗봇 경로에서도 소비되지 않는다. 업로드 → 마스킹 저장에서 파이프라인이 끝난다 (`PreprocessingService.java:67-82`).
2. **여행 플랜이 RAG에 실제로 색인되지 않는다.** `UserDataIndexer.indexPlan/indexUserPlans`(`UserDataIndexer.java:35,53`)는 BE 전체에서 호출되지 않는다(`grep` 결과: 정의 3건뿐, 호출 0건). `PlanService.createFromDraft`(`PlanService.java:369`)도 인덱서를 부르지 않는다. 즉 챗봇이 "내 지난 여행"을 안다는 docstring은 현재 거짓. **문서 업로드(`DocumentService.upload`)만이 유일한 실적재 경로.**
3. **추천과 챗봇이 사용자 취향을 거의 모른다.** 추천은 `User.preferredInterests`(온보딩 콤마 문자열)만 fallback으로 본다(`RecommendService.resolveThemes` L407-419). STT/Kakao에서 추출 가능한 취향·동행·예산 신호는 전혀 반영되지 않는다.

---

## A. 무엇이 필요한지 / 무엇을 더 고칠지

### A-1. 모델 선택 (Med, 비용·품질 트레이드오프)
- 추천(`MODEL_NAME="gpt-4o-mini"` L71)·챗봇·임베딩이 모두 GMS 프록시 경유. 추천은 **구조화 JSON 1-shot**이라 `gpt-4o-mini`로 충분; 굳이 상위 모델로 올릴 가치는 낮다(비용↑, 후보 제약이 강해 환각 여지 적음).
- 챗봇은 **툴 콜링 정확도**가 체감 품질을 좌우한다. 툴이 늘어나면(섹션 C-2) `gpt-4o-mini`의 멀티툴 라우팅이 약점이 될 수 있어 **챗봇만 상위 모델로 분리**하는 옵션을 검토(설정 단계에서 `ChatClient.Builder`에 `OpenAiChatOptions.model(...)` override). 단, GMS 지원 모델 목록 확인이 선결.
- **High-value 아님.** 모델 교체보다 아래 A-2~A-5가 ROI 높음.

### A-2. 스트리밍 응답(SSE) — **High / S~M, 퀵윈**
- 현재 챗봇은 완전 동기. `AssistantService.chat`이 `.call().content()`로 블로킹하고(L82-89), FE는 60s 타임아웃 단발 POST(`api/index.js` `assistantApi.chat` L153-154, `stores/assistant.js` `send`)다. 툴 콜링까지 끼면 응답까지 수 초 침묵 → 체감 매우 나쁨.
- 처방:
  - BE: 스트리밍 메서드 추가(파일 미수정 원칙상 *계획*).
    ```java
    // AssistantService
    Flux<String> chatStream(Long userId, String conversationId, String message);
    // 내부: chatClientBuilder.build().prompt()...stream().content()
    ```
  - Controller: `@GetMapping(produces=TEXT_EVENT_STREAM_VALUE)` 또는 POST+SSE.
  - FE: `assistant.js`에 `fetch`+`ReadableStream`(또는 `EventSource`) 누적 렌더.
- 효과 대비 노력 가장 큼 → 퀵윈.

### A-3. 대화 영속성(세션 간 유지) — **High / M**
- `AssistantConfig.chatMemory()`는 `MessageWindowChatMemory`(in-memory, 20개, L20-24). **앱 재시작·스케일아웃 시 전부 소실**, conversationId도 FE 메모리에만 존재(`stores/assistant.js` `conversationId` ref) → 새로고침하면 대화 끊김.
- 처방: `ChatMemoryRepository`를 JDBC/Redis 구현으로 교체(Spring AI `spring-ai-starter-model-chat-memory-repository-jdbc` 또는 직접 Redis 구현). conversationId를 사용자별로 발급·목록 조회 API 추가해 FE가 과거 대화를 재개.
- 부가: 대화 목록/타이틀 자동요약은 Low(나중에).

### A-4. 임베딩/벡터 설정 견고화 — **Med / S**
- `IngestionService`의 `TokenTextSplitter`가 **전부 기본값**(`new TokenTextSplitter()` L28). 플랜 요약·전사처럼 짧은 한국어 문서엔 청크 크기/overlap 튜닝 여지. 최소 `chunkSize`/`minChunkSizeChars` 명시 권장.
- `RedisVectorStore`는 `topK=4` 고정(`AssistantService.TOP_K` L50), similarity threshold 미설정 → 무관한 청크가 항상 4개 주입될 수 있음. `SearchRequest.similarityThreshold(...)` 추가로 노이즈 컷.
- 메타데이터에 `type`(plan/doc/analysis), `createdAt` tag 추가하면 출처 가중·기간 필터 가능(섹션 C-1과 함께).

### A-5. 에러/타임아웃/재시도/비용 — **Med / S~M**
- **타임아웃 일원화 부재.** 추천은 "spring.ai 기본 30s로 충분"이라 주석(`RecommendService.java:58-59`)하지만 명시 설정이 없고, Whisper는 `RestClientConfig.restClient()`(L16-21)가 **TourAPI baseUrl로 만든 공용 RestClient를 재사용**한다(`WhisperSTTManager`가 주입받아 `apiUrl` 절대경로로 덮어씀). 타임아웃/커넥션풀 미설정 → 대용량 음성에서 행 위험. **STT 전용 RestClient(긴 read timeout) 분리** 권장.
- **재시도 없음.** GMS 프록시 5xx/일시 오류에 대한 `RetryTemplate`/`spring.ai.retry.*` 설정 부재. 추천은 실패 시 곧장 FAILED 저장(`RecommendService.java:210-224`). 임베딩·챗 호출에 1~2회 백오프 재시도 도입은 저비용 고효과.
- **비용 관측 부재.** 추천만 `latencyMs`/`model`을 DB에 남김(`Recommendation`). 챗봇·임베딩·STT는 토큰/호출 로깅 없음. 최소한 호출 수·실패율 카운터(Micrometer) 추가.

### A-6. 프롬프트 견고성 — **Med / S**
- 추천 프롬프트는 잘 제약돼 있고(후보 contentId만, JSON 포맷, `validate()` 후처리 L448-502) 견고. **테마 매핑 표(THEME_LABELS L75-82)가 FE 키와 수동 동기화**라 drift 위험 — 주석에도 명시됨(L73-74). 단일 enum/공유 상수화 권장(Low).
- 챗봇 시스템 프롬프트(`SYSTEM_PROMPT` L41-48)는 환각 억제·도구 사용 가이드가 적절. 툴 확장 시(C-2) "행동 전 확인" 정책 문구 보강 필요.

### A-7. 평가(Evaluation) — **Med / M, 지속 투자**
- 현재 자동 평가 전무. 추천은 `validate()`로 형식 검증만 한다. 처방:
  - 추천: 골든셋(지역×테마×기간) 20~30케이스로 "후보 외 ID 0건 / 일자 충족률 / 동선 합리성(LLM-judge)" 회귀 측정.
  - 챗봇: 툴 라우팅 정확도(의도→호출 툴) + RAG 인용 적합성 LLM-as-judge.

---

## B. STT를 어떻게 할지 (End-to-End)

**문제:** `convertSpeechToText` → `maskPii` → `UserAnalysisData.rawText` 저장에서 끝(`PreprocessingService.java:71-81`). 전사가 제품의 어디에도 닿지 않는다. FE도 "취향 분석에 자동 반영돼요"라고 안내하지만(`AnalysisUploadView.vue:122-124`) 실제 반영 코드는 없다.

### B-1. 목표 가치
원천(음성통화·카톡)에서 **여행 취향 프로필**을 추출해 (1) 추천 개인화, (2) 챗봇이 사용자를 "기억", (3) 플랜 자동 채움의 연료로 쓴다.

### B-2. 제안 파이프라인
```
업로드(voice/kakao)
  └─ STT/텍스트 추출 + PII 마스킹           [기존: PreprocessingService]
       └─ (NEW) AnalysisExtractionService.extract(rawText)
            └─ LLM 구조화 추출 → TravelPreferenceProfile (JSON)
                 ├─ (NEW) UserPreferenceProfile 엔티티로 upsert  ─→ 추천 파이프라인(C-3)
                 └─ IngestionService.ingest(userId, "analysis:{id}", "내 대화 분석", 요약텍스트) ─→ RAG(C-1)
```

### B-3. 추출 결과 형태 (구조화)
```java
// recommend 또는 user 도메인에 신설 (BeanOutputConverter 대상 record)
public record TravelPreferenceProfile(
    List<String> interests,      // THEME_LABELS 키 어휘로 정규화: sea/food/history...
    List<String> mentionedPlaces,// 대화에 등장한 지명/장소
    String companionHint,        // SOLO/COUPLE/FAMILY/FRIENDS 추정
    String budgetHint,           // low/mid/high
    List<String> avoid,          // 회피 신호(예: "사람 많은 곳 싫어")
    String summary               // 한 줄 자연어 요약(임베딩용)
) {}
```

### B-4. 변경/신설 클래스 (계획)
- **신설** `preprocessing/service/AnalysisExtractionService` — `rawText`를 받아 `ChatClient`+`BeanOutputConverter<TravelPreferenceProfile>`로 추출. 긴 전사는 청크 요약 후 추출(map-reduce).
- **변경** `PreprocessingService.processData`(L67-82) 끝에서 추출 서비스 호출(트랜잭션 분리: 외부 LLM 호출은 저장 커밋 후 비동기 `@Async`/이벤트로 — 업로드 응답 지연 방지).
- **신설** `UserPreferenceProfile` 엔티티(userId 1:1, 최신 프로필 누적 머지) + 리포지토리.
- **변경(연결)** `IngestionService.ingest`(기존 메서드 그대로) 호출로 RAG 적재 — **신규 메서드 불필요**, docId 네임스페이스만 `analysis:{id}`로 추가.
- **변경** `AnalysisUploadView.vue` 결과 카드에 추출된 취향 칩(관심사/동행/예산) 표시 → 사용자에게 "분석됐다"는 실체 제공(현재 L122 문구의 약속을 코드로 충족).

### B-5. surfacing(사용자 노출)
- 마이페이지 "내 여행 취향" 섹션에 프로필 표시 + 수정 토글(추출이 틀릴 수 있으니 사용자 승인/편집).
- 추천 입력 화면(`AiPlanInputView`)에서 테마 미선택 시 이 프로필을 **기본 프리필**.

---

## C. 챗봇과 어떻게 연계할지

### C-1. UserAnalysisData/플랜을 벡터스토어에 함께 색인 — **High / M**
두 가지를 동시에 메운다: (a) **죽어있는 플랜 인덱서를 살리고**, (b) 분석 데이터를 추가한다. `IngestionService`는 그대로 재사용 가능(범용 `ingest(userId, docId, source, text)`).

- **(a) 플랜 인덱싱 활성화 (퀵윈, S):** `PlanService.createFromDraft`(`PlanService.java:369`) 및 수동 플랜 저장/수정 경로에서 `userDataIndexer.indexPlan(userId, planId)` 호출. 부팅 시 또는 최초 챗 진입 시 `indexUserPlans`로 백필. 현재 호출처 0건이므로 **연결만 하면 즉시 "내 지난 여행" 기능이 진짜로 동작**.
  > 트랜잭션 주의: 인덱싱은 커밋 후(`@TransactionalEventListener(AFTER_COMMIT)`)로 분리해 임베딩 호출이 플랜 저장 트랜잭션을 늘리지 않게.
- **(b) 분석 데이터 인덱싱:** B-2의 요약 텍스트를 `ingest(userId, "analysis:{id}", "내 대화 분석", summary)`로 적재. 메타 `source` 라벨로 출처 구분(이미 RagConfig가 `source` tag 인덱싱 L57-59).
- **메타데이터 보강:** `RagConfig.metadataFields`에 `type` tag 추가(plan/doc/analysis) → 검색 시 가중·필터. `AssistantService`의 `SearchRequest`(L69-72)에 필요 시 `type` 필터·`similarityThreshold` 추가.

효과: 챗봇이 "지난 부산 여행 어땠지?", "내 취향에 맞는 일정" 류 질의에 실데이터로 응답.

### C-2. @Tool 함수 확장 — **High(핵심 일부)·Med(나머지) / M~L**
현재 툴은 `searchAttractions`/`createTravelPlan` 2종(`AssistantService.AssistantTools` L96-166). 대화형 비서가 "행동"하도록 확장. 패턴은 동일(요청별 `userId` 캡처, 백엔드 서비스는 읽기전용 의존).

우선순위 제안:
| 툴 | 효과 | 노력 | 비고 |
|---|---|---|---|
| `addPlaceToPlan(planId, contentId, dayNo, visitTime)` | High | S | 기존 PlanService place 추가 재사용. "이 장소 2일차에 넣어줘" |
| `recommendItinerary(...)` (생성만, 저장 안 함) | High | S | `createTravelPlan`이 곧장 저장하는데(L157-161), 미리보기→확인 2단계 UX가 안전 |
| `getMyPlans()` / `getPlanDetail(planId)` | High | S | 후속 행동 툴의 선행 컨텍스트 |
| `saveStory(planId, text)` / 여행기 | Med | M | 커뮤니티/스토리 도메인 연계 |
| `createChecklist(planId)` | Med | M | LLM이 일정 기반 준비물 생성 |
| `joinGroup()/동행` | Med | M | companion 도메인 연계, 권한·검증 주의 |

**가드레일(필수):** 쓰기형 툴은 시스템 프롬프트에 "실행 전 사용자에게 1줄 확인" 정책 명시 + 툴 자체에서 소유권 검증(`userId` 일치). 현재 `createTravelPlan`은 확인 없이 즉시 저장·실패를 문자열로 흡수(L162-165) → 미리보기 분리 권장.

### C-3. 분석 기반 취향을 추천 파이프라인에 주입 — **High / S**
- 현재 `resolveThemes`(`RecommendService.java:407-419`)는 요청 테마 → `User.preferredInterests`만 본다. 여기에 **B-3의 `UserPreferenceProfile`를 2순위 fallback**으로 추가:
  ```
  요청 themes  ▶  UserPreferenceProfile.interests  ▶  User.preferredInterests  ▶  "미지정"
  ```
  단, 캐시 해시(`serializeRequest` L540-554)는 요청 원본만 쓰므로 프로필 변경이 캐시를 오염시키지 않음(현 설계와 일관). 다만 프로필이 갱신되면 동일요청 캐시가 옛 취향을 반환할 수 있어, 프로필 버전을 해시에 포함할지 결정 필요(Low).
- `companionHint`/`budgetHint`도 요청이 비었을 때 프리필 소스로 사용(현재 `describeCompanions`/`describeBudget`은 미지정 처리만).

---

## D. 로드맵 (우선순위 · 순서 · 노력)

노력: S(≤0.5d) / M(1~3d) / L(>3d). 우선순위: High=체감·구조 임팩트 큼.

### High (먼저, 대부분 퀵윈)
1. **플랜 RAG 인덱싱 연결 (C-1a)** — S. `createFromDraft`/플랜저장에 `indexPlan` 호출 + 부팅 백필. *데드코드를 살리는 즉효 퀵윈.*
2. **챗봇 SSE 스트리밍 (A-2)** — S~M. 체감 응답성 최대 개선. BE 스트림 메서드 + FE EventSource.
3. **분석 취향을 추천에 주입 (C-3)** — S. `resolveThemes` fallback 체인에 프로필 추가. (B의 프로필 산출이 선행)
4. **STT/Kakao → 취향 추출 파이프라인 (B-2~B-4)** — M. `AnalysisExtractionService` 신설 + 비동기 추출 + 프로필 저장. *죽은 데이터 활성화의 핵심.*
5. **분석/플랜 통합 색인 + 메타 type (C-1b, A-4 일부)** — M. 챗봇이 사용자를 "안다".
6. **핵심 행동 툴 3종 (C-2: addPlaceToPlan, recommendItinerary-미저장, getMyPlans)** — M. 비서의 행동성 확보 + 저장 전 확인 UX.

### Med (다음)
7. **대화 영속화 (A-3)** — M. ChatMemoryRepository(JDBC/Redis) + 대화 목록 API.
8. **STT 전용 RestClient + 재시도/타임아웃 (A-5)** — S~M. 공용 RestClient 분리, `spring.ai.retry` 또는 백오프.
9. **임베딩 튜닝 + similarityThreshold (A-4)** — S. 청크/overlap 명시, 노이즈 컷.
10. **나머지 행동 툴 (saveStory/createChecklist/joinGroup) (C-2)** — M~L. 도메인 연계·권한 검증.
11. **취향 프로필 마이페이지 노출·편집 (B-5)** — M. 사용자 신뢰·교정 루프.
12. **관측성(토큰/실패율 메트릭) (A-5)** — S. Micrometer 카운터.

### Low (여유 시)
13. **챗봇 전용 상위 모델 분리 검토 (A-1)** — S(설정). GMS 모델 목록 확인 후.
14. **테마 매핑 단일 소스화 (A-6)** — S. FE/BE drift 제거.
15. **자동 평가 골든셋 (A-7)** — M. 추천·챗봇 회귀 측정.
16. **대화 자동 타이틀/요약 (A-3 부가)** — S.
17. **이미지 문서 OCR (DocumentService L148)** — M. 현재 IMAGE 미색인.

### 의존 순서 요약
- B(취향 추출) → C-3(추천 주입) · C-1b(색인) 선행.
- High 1·2는 독립 퀵윈 → 먼저 착수 가능.
- C-2 행동 툴은 SSE(2) 이후 붙이면 UX 일관.

---

## 부록: 핵심 근거 file:line
- 추천 모델/락/파이프라인: `RecommendService.java:71`, `:118-229`, `:344-364`, `:407-419`, `:448-502`
- 챗봇 RAG/메모리/툴: `AssistantService.java:50`, `:67-90`, `:96-166` · `AssistantConfig.java:20-24`
- 벡터스토어/적재: `RagConfig.java:51-61` · `IngestionService.java:38-61` · `UserDataIndexer.java:35,53`(**호출처 0**)
- 문서 적재(유일 실동작): `DocumentService.java:45-78`, OCR 미구현 `:148`
- STT 데드엔드: `PreprocessingService.java:67-82` · `WhisperSTTManager.java:36-72`(공용 RestClient 재사용)
- 설정: `application.yaml:67-80`(모델), `:129-133`(Whisper) · `build.gradle:46`(spring-ai-bom 1.1.2)
- FE: `AssistantView.vue`, `stores/assistant.js`(비스트리밍), `AnalysisUploadView.vue:122-124`(미충족 약속), `api/index.js:153,239`
