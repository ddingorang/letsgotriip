# 03. 종합 의사결정 (Claude × Codex GPT-5.5)

[01-current-state.md](01-current-state.md)(Claude)와 [02-codex-analysis.md](02-codex-analysis.md)(Codex GPT-5.5 xhigh)를
교차 검토한 결과와 채택 결정.

## 합의 (양측 동일 결론)
- **RAG 검색 게이트 부재가 1순위 결함**: 고정 `topK=4` + 유사도 임계값 없음 → 무관한 컨텍스트를
  "최선 4개"로 강제 주입 → 환각·엉뚱한 일정 참조. → 임계값 도입 + topK 상향 후 리랭크.
- **인덱싱 멱등성/신선도가 생각보다 위험**: `ingest()`가 안정적 ID 없이 Document를 add → 같은 docId
  재인덱싱 시 청크 중복 가능. plan 수정/도구 편집 후 재인덱싱 트리거도 불확실. (Codex가 "stated보다 나쁨"으로 격상)
- **placeId 기반 삭제는 도구 계약 결함**: 사용자·LLM이 placeId를 못 봄 → 자연어 삭제 실패/오삭제.
- **대화기억 영속성 불명**: in-memory면 재기동 시 유실.
- **챗봇 수정 후 프론트 미반영**, **출처 표기 약함**, **관측성 부재** 모두 동의.

## Codex가 추가로 짚은 것 (Claude 분석 보강)
1. **string-concat 필터식이 크로스유저 누수 위험**: `"userId == '"+id+"'"` 대신
   `FilterExpressionBuilder().eq("userId", safeId)` + TAG 정규화 사용.
2. **후속질문 검색 실패**: "그 일정 첫날은?"류는 현재 메시지만으로 검색 → 대화이력 기반
   query rewrite/compression 필요(`RewriteQueryTransformer`/`CompressionQueryTransformer`).
3. **상태변경 도구는 프롬프트 순종에만 의존**: 서버측 사전조건/확인(preview→save 분리) 필요.
4. **도구 출력이 사람용 문자열뿐** → UI 갱신·감사·재시도·후속추론에 약함. 구조화 이벤트 필요.

## 이견 / 조정
- **모델 선택(gpt-4o-mini)**: Claude는 "멀티모델 라우팅" 후보로 봤으나, Codex는 "데모엔 충분,
  진짜 문제는 라우팅/확인/온도/도구가드 부재"라고 부분 반대. → **채택: 모델 교체보다 도구 가드·
  결정성(낮은 temperature) 우선.** 멀티모델은 P2로 강등.

## 채택 로드맵 (우선순위)

### P0 — 지금 바로 (데모 신뢰성 직결, 대부분 S)
1. **검색 게이트**: `SearchRequest.similarityThreshold(0.65~0.80).topK(8~12)`. (S)
2. **필터식 안전화**: `FilterExpressionBuilder().eq("userId", safeId)` + TAG 정규화 일원화. (S, 보안)
3. **인덱싱 멱등화**: 재인덱싱 전 항상 `deleteByDoc(docId)` → `add`. plan create/update/장소변경에
   `@TransactionalEventListener(AFTER_COMMIT)`로 재인덱싱 트리거. (M)
4. **장소 삭제 UX 수정**: `listPlanPlaces(planId)`(dayNo/placeId/contentId/title) 도구 추가, 또는
   `removePlaceFromPlan`이 title/contentId로 유일 해석. (S/M)
5. **대화기억 영속화**: Redis Stack이 이미 있으니 `MessageWindowChatMemory` +
   `RedisChatMemoryRepository`. (S/M)

### P1 — 다음
6. 출처 인용(컨텍스트에 `[source, docId, chunkNo]` 주입, 인용 강제). (M)
7. 후속질문 query rewrite (`RetrievalAugmentationAdvisor` + transformer). (M)
8. 소스 타입별 청킹 튜닝(plan=일자/장소, PDF=페이지, STT=시간블록). (M)
9. 리랭크/디둡(`DocumentPostProcessor`로 docId 디둡 + 최신 plan 우선, 20→5~6). (M)
10. **챗봇 수정 후 프론트 갱신**: `PlanChangedEvent` 발행 → SSE/WebSocket 또는 구조화 mutation
    메타데이터 반환으로 프론트 refetch. (M) ← 사용자가 겪은 "수정해도 화면 그대로" 직접 해결.
11. 관측성(Spring AI Micrometer + RAG hit rate/empty-context/tool 성공·실패 카운터). (S/M)

### P2 — 이후
12. 하이브리드 검색(BM25/RediSearch text + 벡터 union+rerank) — 한국어 고유명사에 유효. (M/L)
13. preview/save 도구 분리(`previewTravelPlan`/`saveTravelPlan` 확인 후 저장). (S)
14. 모델 라우팅(단순 Q&A 경량, 도구 결정은 결정성↑). (S/M)
15. 골든셋 평가(저장계획/오래된계획/크로스유저격리/무응답/도구변경 20~50문항). (M)

## 가장 큰 단일 리스크 (양측 합의)
> 임계값 없는 검색 + 약한 인용 + 불확실한 재인덱싱 + 즉시 상태변경 도구가 결합하면 —
> **챗봇이 "엉뚱하거나 오래된 일정"을 권위 있는 것처럼 답하거나, 잘못된 plan을 수정하면서도
> 성공한 것처럼 보이는** 실패 모드. → P0 1·3이 이 리스크를 직접 차단.

## 메모
- 본 심의는 설계 합의까지. 코드 적용은 P0부터 별도 작업으로 진행.
- 이번 세션에서 사용자가 겪은 "챗봇 수정이 화면에 반영 안 됨"은 항목 10(P1)에 해당.
