**Findings**

1. [high] `replacePlaces`가 잘못된 배열을 DB 제약까지 흘려 500을 낼 수 있음  
   근거: `PlacesReplaceRequestDto`는 `places` null만 막음(`BE/src/main/java/com/trip/plan/dto/PlacesReplaceRequestDto.java:7`), `PlaceItemDto.seq`/중복 검증 없음(`BE/src/main/java/com/trip/plan/dto/PlaceItemDto.java:9`), 서비스가 `item.seq()` 그대로 저장(`BE/src/main/java/com/trip/plan/service/PlanService.java:239`). DB는 `(day_id, seq)`, `(day_id, attraction_id)` unique 및 `seq nullable=false`(`BE/src/main/java/com/trip/plan/entity/TripPlace.java:17`, `BE/src/main/java/com/trip/plan/entity/TripPlace.java:38`).  
   재현/수정: 같은 `seq` 또는 같은 장소 2개 PUT. DTO에 `@Valid`, `@NotNull`, `@Size`, 중복 사전검증 추가 후 400/409로 반환.

2. [high] 기존 장소 순서 변경이 외부 TourAPI 장애에 묶임  
   근거: FE `replacePlaces`가 기존 `place.id`를 버리고 `contentId/contentType`만 보냄(`frontend/src/stores/plan.js:148`), BE는 이 경우 `upsertSnapshot`으로 외부 상세조회 수행(`BE/src/main/java/com/trip/plan/service/PlanService.java:217`). `placeId` 재사용 경로는 따로 있음(`BE/src/main/java/com/trip/plan/service/PlanService.java:219`). 이동/리포트 적용은 이 store를 사용함(`frontend/src/views/PlanView.vue:270`, `frontend/src/views/PlanReportView.vue:418`).  
   재현/수정: TourAPI 상세 장애 상태에서 장소 위/아래 이동 또는 동선 적용. FE는 기존 장소면 `placeId`를 보내고, BE는 같은 plan/day 소속까지 검증.

3. [high] AI 초안 저장이 장소 누락을 조용히 성공 처리함  
   근거: `createFromDraft`가 각 장소 저장 중 `upsertSnapshot` 호출(`BE/src/main/java/com/trip/plan/service/PlanService.java:333`) 실패를 `catch (Exception)`으로 전부 무시(`BE/src/main/java/com/trip/plan/service/PlanService.java:352`)하고 정상 응답 반환(`BE/src/main/java/com/trip/plan/service/PlanService.java:360`).  
   재현/수정: 추천 결과 contentId 중 상세조회 실패가 나면 `save-plan`은 200이지만 계획 장소가 빠짐. 최소 장소 수 검증, 실패 로깅, 부분 저장 상태 반환 또는 트랜잭션 실패 처리 필요.

4. [med] 계획 상세/동선 리포트에서 attraction N+1 발생  
   근거: 상세 조회는 plan-days만 fetch join(`BE/src/main/java/com/trip/plan/repository/PlanRepository.java:32`), `TripPlace.attraction`은 LAZY(`BE/src/main/java/com/trip/plan/entity/TripPlace.java:34`), 응답 변환에서 attraction 필드를 반복 접근(`BE/src/main/java/com/trip/plan/dto/PlaceResponseDto.java:15`, `BE/src/main/java/com/trip/plan/dto/RouteReportResponseDto.java:44`).  
   재현/수정: 장소 N개인 plan 상세/리포트 호출 시 attraction 조회 N회. attraction batch size, entity graph, DTO query로 보강.

5. [med] 좌표 없는 장소가 동선 최적화에서 거리 0으로 취급됨  
   근거: 좌표 null이면 `haversineKm`이 0 반환(`BE/src/main/java/com/trip/plan/util/RouteCalculator.java:23`), nearest-neighbor는 그 값을 최소거리로 선택(`BE/src/main/java/com/trip/plan/util/RouteCalculator.java:71`). 주석의 “좌표 없는 장소는 뒤로” 정책과도 불일치(`BE/src/main/java/com/trip/plan/util/RouteCalculator.java:57`).  
   재현/수정: 좌표 없는 장소가 섞이면 추천 순서가 앞으로 당겨질 수 있음. 좌표 없는 항목은 제외/후순위 penalty 처리.

6. [med] FE는 8일 AI 여행을 허용하지만 BE는 7일까지만 허용  
   근거: FE `nights` 최대 7박(`frontend/src/views/AiPlanInputView.vue:77`, `frontend/src/views/AiPlanInputView.vue:81`), endDate는 start + nights(`frontend/src/views/AiPlanInputView.vue:235`). BE는 날짜 차이 `<= 6`만 허용(`BE/src/main/java/com/trip/recommend/dto/RecommendRequestDto.java:30`).  
   재현/수정: 7박 선택 후 생성하면 400. FE 최대 6박으로 맞추거나 BE 정책 변경.

7. [med] 실패 추천이 최신 이력으로 로드되면 결과 화면에서 저장 버튼이 활성화됨  
   근거: 실패도 저장 가능(`BE/src/main/java/com/trip/recommend/service/RecommendService.java:207`), history는 status 필터 없음(`BE/src/main/java/com/trip/recommend/service/RecommendService.java:229`), 결과 화면은 최신 이력을 무조건 로드(`frontend/src/views/AiResultView.vue:197`)하고 저장 버튼은 status/draft를 보지 않음(`frontend/src/views/AiResultView.vue:151`). BE는 `resultJson == null`이면 422(`BE/src/main/java/com/trip/recommend/service/RecommendService.java:259`).  
   재현/수정: 최신 추천이 FAILED인 상태로 `/ai/result` 진입 후 저장. SUCCESS/PARTIAL만 bootstrap하거나 FAILED 저장 버튼 숨김.

8. [med] 후보 0건 AI 실패는 “실패 저장” 정책과 다르게 이력에 남지 않음  
   근거: 주석은 실패 저장을 명시(`BE/src/main/java/com/trip/recommend/service/RecommendService.java:54`), 후보 0건은 `RecommendHandler`를 던짐(`BE/src/main/java/com/trip/recommend/service/RecommendService.java:153`), 해당 catch는 저장 없이 재던짐(`BE/src/main/java/com/trip/recommend/service/RecommendService.java:201`).  
   재현/수정: TourAPI/festival 후보 0건 지역 요청. `RECO_IN_PROGRESS` 외 `RecommendHandler`도 실패 row 저장하거나 실패 저장 경로를 통일.

9. [med] AI 요청 입력이 프롬프트 비용/크기 제한 없이 LLM으로 들어감  
   근거: `RecommendRequestDto`는 `areaCode`/`budget`/`themes`에 크기·패턴·최소값 검증 없음(`BE/src/main/java/com/trip/recommend/dto/RecommendRequestDto.java:13`), unknown theme는 원문 그대로 프롬프트에 포함(`BE/src/main/java/com/trip/recommend/service/RecommendService.java:404`), budget도 그대로 포맷(`BE/src/main/java/com/trip/recommend/service/RecommendService.java:418`).  
   재현/수정: 매우 긴 themes 배열 또는 음수 budget POST. `@Size`, `@Pattern`, `@Min`, 허용 theme whitelist 추가.

10. [low] 결과 화면 status 계약 불일치  
   근거: BE status는 `SUCCESS/PARTIAL/FAILED`(`BE/src/main/java/com/trip/recommend/entity/RecommendStatus.java:3`)인데 FE는 존재하지 않는 `DONE`과 비교(`frontend/src/views/AiResultView.vue:69`).  
   재현/수정: 정상 성공도 `SUCCESS` 배지가 노출됨. `SUCCESS` 기준으로 숨기고 FAILED/PARTIAL만 표시.

11. [low] 계획 목록 카드가 BE에 없는 필드를 렌더링함  
   근거: FE가 `plan.destination`, `plan.spots` 사용(`frontend/src/views/PlanView.vue:53`, `frontend/src/views/PlanView.vue:55`), BE summary DTO에는 해당 필드 없음(`BE/src/main/java/com/trip/plan/dto/PlanSummaryResponseDto.java:10`).  
   재현/수정: 목록 카드에 빈 목적지/스팟 UI. FE에서 제거·파생하거나 BE DTO에 명시 추가.

코드는 수정하지 않았고, 리뷰 범위에 맞춰 정적 검토만 수행했습니다.