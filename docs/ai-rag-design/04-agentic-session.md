# 04 — 에이전트형 여행설계: 단계수집(LangGraph식) · 세션 컨텍스트 · 챗봇 통합

사용자 요청 요약:
- LangGraph처럼 **단계적으로 수집**(무엇을 모아야 하는지 명시적으로).
- 그 수집물을 **챗봇이 전부 조회**할 수 있어야 함 → **세션처럼** 쓰는 사용자 여행 컨텍스트를 저장.
- **챗봇(AssistantService)도** 그 컨텍스트를 쓰도록 개조.
- 서버에서 직접 써보며 **체크리스트로 고쳐나가고** codex 적대적 리뷰. 끝나면 UI 병렬 테스트.

---

## 1. 핵심 개념 — TravelContext "세션"

대화/추천이 공유하는 **상태 객체**(LangGraph의 state에 해당). userId(+conversationId)로 키잉해
**Redis에 JSON으로 저장**(TTL 1일). 프론트는 조회 API로 받아 localStorage에 미러(=사용자 로컬 저장).

```
TravelContext {
  areaCode, areaName        // 목적지
  startDate, endDate        // 기간
  companions, budget        // 동행·예산
  themes[]                  // 테마
  gathered: [               // 단계수집된 후보(도구가 누적)
    { contentId, category, title, sigungu, lat, lng }
  ]
  notes                     // 자유 메모(취향·제약)
  updatedAt
}
```

왜 서버(Redis)인가: **챗봇(서버)이 조회**해야 하므로 서버 권위 저장 + 프론트 미러. 세션처럼 TTL.

---

## 2. 단계(stages) — 무엇을 언제 수집하나 (LangGraph식)

| 단계 | 이름 | 하는 일 | 도구/소스 |
|---|---|---|---|
| S1 | resolveTrip | 목적지·기간·동행·예산·테마 확정(부족하면 사용자에 질문) | setTripContext |
| S2 | gatherPlaces | 테마별 관광지/문화/음식점 후보 수집 | searchPlaces(type,keyword) |
| S3 | gatherEvents | 기간 내 축제/행사 | listFestivals |
| S4 | gatherContext | 날씨(실내/야외 균형), (확장) 사용자 찜·기존계획 | getWeather, getMyFavorites |
| S5 | compose | 동선 클러스터링 + 하루 골격으로 일정 구성(CoT) | (LLM 추론) |
| S6 | validate | 후보 외 제거·중복·보충·상태판정 | 결정론적(기존 로직) |

→ S2~S4가 "단계수집". 각 도구는 결과를 **TravelContext.gathered에 누적**(세션) → 챗봇도 같은 걸 조회.

---

## 3. 도구(@Tool) — 추천 에이전트 + 챗봇 공용

읽기/수집:
- `searchPlaces(contentTypeId?, keyword?)` — 후보 조회 + gathered 누적 (✅ 1차 구현됨)
- `listFestivals()` — 기간 축제 (✅ 1차 구현됨)
- `getWeather()` — 목적지 기간 날씨(실내외 균형 힌트)  ⬜
- `getTripContext()` — 현재 세션 컨텍스트 요약 반환  ⬜
- `getMyFavorites()` — 사용자 찜 목록(취향 반영)  ⬜

쓰기:
- `setTripContext(areaCode?, startDate?, endDate?, companions?, budget?, themes?, notes?)` — 세션 갱신  ⬜

상태변경(기존 챗봇 유지): createTravelPlan / addPlaceToPlan / removePlaceFromPlan / createChecklist.

---

## 4. 챗봇(AssistantService) 개조

- 대화 시작 시 `TravelContext` 로드(있으면) → 시스템 프롬프트에 "현재 여행 컨텍스트" 주입.
- 사용자가 조건을 말하면 `setTripContext`로 세션 갱신 → 이후 추천/검색이 그 컨텍스트 사용.
- 추천 생성은 세션 컨텍스트 기반으로 `createTravelPlan` 호출(파라미터를 매번 되묻지 않음).
- 즉, 챗봇 = 같은 세션/도구를 쓰는 대화형 프런트엔드.

---

## 5. 프론트(선택, 후순위)

- `GET /api/assistant/context` → TravelContext 반환, localStorage 미러.
- AssistantView에서 현재 컨텍스트(목적지·기간·테마·담은 후보 수) 칩으로 표시.

---

## 6. 체크리스트 (서버에서 직접 호출하며 검증)

- [x] P1. 추천을 tool-calling 에이전트로 전환(system+CoT+도구) — **실측 SUCCESS(부산 17s)**
- [x] P1. 단위테스트(validate/computeStatus) 통과
- [ ] P2. TravelContextStore(Redis JSON, TTL) + 직렬화
- [ ] P2. setTripContext/getTripContext/getWeather/getMyFavorites @Tool
- [ ] P2. 추천 에이전트가 gathered를 세션에 누적·재사용
- [ ] P3. AssistantService에 컨텍스트 로드/주입 + 공용 도구 장착
- [ ] P3. 챗봇으로 "부산 8/1-2 커플 미식" → 컨텍스트 누적 → 추천까지 대화 검증
- [ ] P4. (선택) FE 컨텍스트 조회/미러·칩 표시
- [ ] P5. codex 적대적 리뷰(설계·구현) 반영
- [ ] P6. usecases.md·button-plan.md 기반 Playwright 병렬 테스트

검증 방식: 각 단계마다 `curl`로 서버 직접 호출 + 로그 확인. 실패 시 고치고 재호출.
