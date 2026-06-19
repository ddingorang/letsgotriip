[high] save-plan이 장소 저장 실패를 삼켜 빈 계획을 “저장됨”으로 확정함  
왜: `RecommendService.savePlan`은 `createFromDraft` 후 바로 `savedPlanId`를 기록합니다(`BE/src/main/java/com/trip/recommend/service/RecommendService.java:278`, `:279`). 그런데 `PlanService.createFromDraft`는 장소별 `upsertSnapshot` 실패를 전부 `catch (Exception)`로 삼키고 계속 진행합니다(`BE/src/main/java/com/trip/plan/service/PlanService.java:326`, `:333`, `:352`). `TripPlace.memo`는 300자 제한인데(`BE/src/main/java/com/trip/plan/entity/TripPlace.java:44`), LLM `reason`은 무제한 DTO이고(`BE/src/main/java/com/trip/recommend/dto/ItineraryDraft.java:20`), 그대로 `memo(p.reason())`에 들어갑니다(`BE/src/main/java/com/trip/plan/service/PlanService.java:349`). 결과적으로 TourAPI 상세 실패나 긴 reason 하나로 장소가 조용히 누락되고, 전부 실패해도 빈 계획이 저장될 수 있습니다.  
수정 방향: 장소 변환 실패를 집계하고 최소 장소 수 미달이면 예외로 롤백. `reason` 길이 검증/절단. 성공적으로 생성된 plan에 실제 장소가 있는지 확인한 뒤에만 `markSavedPlan`.

[high] 외부 API 호출에 명시 timeout이 없어 추천 생성 요청이 서버에서 bounded되지 않음  
왜: TourAPI용 `RestClient`는 `baseUrl`과 header만 설정하고 timeout 설정이 없습니다(`BE/src/main/java/com/trip/global/config/RestClientConfig.java:16`, `:20`). 추천 생성은 이 클라이언트를 동기 루프에서 호출합니다(`BE/src/main/java/com/trip/recommend/service/RecommendService.java:293`, `:295`). LLM도 `ChatClient` 기본 빌더만 사용하고(`BE/src/main/java/com/trip/global/config/AiConfig.java:16`, `:17`), 설정에는 model만 있습니다(`BE/src/main/resources/application.yaml:67`, `:75`). FE timeout은 axios 클라이언트만 끊습니다(`frontend/src/stores/recommend.js:34`, `:36`).  
수정 방향: TourAPI/OpenAI 각각 connect/read/response timeout과 retry/backoff/circuit-breaker를 명시. 서버 작업 timeout 초과 시 실패 Recommendation 저장 후 종료.

[med] 추천 요청 입력 검증이 사실상 기간만 있음  
왜: DTO는 `areaCode`, `startDate`, `endDate`만 필수입니다(`BE/src/main/java/com/trip/recommend/dto/RecommendRequestDto.java:13`, `:27`). 서비스도 `isPeriodValid()`만 확인합니다(`BE/src/main/java/com/trip/recommend/service/RecommendService.java:117`, `:120`). `companions`와 `themes`는 매핑 실패 시 원문을 프롬프트에 넣습니다(`BE/src/main/java/com/trip/recommend/service/RecommendService.java:405`, `:414`).  
수정 방향: `areaCode` 허용값/패턴, `companions` enum, `budget` min/max, `themes` whitelist와 개수/길이 제한을 BE에서 검증. FE 선택값은 신뢰하지 않기.

[med] 실패 추천 이력이 결과 화면의 “최신 추천”으로 로드되고 저장 버튼까지 활성화됨  
왜: 이력 조회는 status 필터 없이 모든 추천을 반환합니다(`BE/src/main/java/com/trip/recommend/service/RecommendService.java:229`, `:233`). 결과 화면은 current가 없으면 `history[0]`을 무조건 로드합니다(`frontend/src/views/AiResultView.vue:197`, `:203`). `FAILED`면 `draft`가 null이라 days는 빈 배열이지만(`frontend/src/views/AiResultView.vue:212`, `:215`), 저장 버튼은 `saveLoading || saveDone`만 봅니다(`frontend/src/views/AiResultView.vue:151`, `:154`). 서버는 그때서야 `resultJson == null`로 실패합니다(`BE/src/main/java/com/trip/recommend/service/RecommendService.java:259`, `:260`).  
수정 방향: 이력 자동 로드는 `SUCCESS/PARTIAL`만 대상으로 하거나, FE에서 `FAILED`/`draft null`이면 결과 UI와 저장 버튼을 숨기고 재생성 CTA만 노출.

[med] savedPlanId가 삭제된 plan을 영구 참조할 수 있음  
왜: `recommendations.saved_plan_id`는 FK도 unique도 아닙니다(`BE/docs/schema.sql:120`). 이미 저장된 추천은 `savedPlanId`가 있으면 무조건 `planService.getDetail`로 반환합니다(`BE/src/main/java/com/trip/recommend/service/RecommendService.java:255`, `:256`). 그런데 plan 삭제는 별도 정리 없이 plan만 삭제합니다(`BE/src/main/java/com/trip/plan/service/PlanService.java:153`, `:157`).  
수정 방향: FK `ON DELETE SET NULL` 또는 plan 삭제 시 recommendation 역참조 정리. `getDetail`이 PLAN404면 `savedPlanId`를 clear하고 재저장 가능하게 처리.

[med] resultJson 파싱 실패를 숨겨 SUCCESS/PARTIAL인데 draft null 응답이 가능함  
왜: 응답 DTO 변환에서 `resultJson` 파싱 예외를 무시하고 draft null로 반환합니다(`BE/src/main/java/com/trip/recommend/dto/RecommendationResponseDto.java:21`, `:28`). FE는 `rec`만 있으면 결과 화면을 렌더링합니다(`frontend/src/views/AiResultView.vue:40`, `:66`).  
수정 방향: 파싱 실패는 로깅하고 5xx/데이터 손상 상태로 노출. 최소한 `status != FAILED && draft == null`은 FE에서 저장 불가 처리.

[low] 캐시 히트에도 HTTP 201 Created를 반환함  
왜: 서비스는 5분 내 동일 SUCCESS 추천을 그대로 반환합니다(`BE/src/main/java/com/trip/recommend/service/RecommendService.java:127`, `:132`). 컨트롤러는 항상 `created(...)`로 응답합니다(`BE/src/main/java/com/trip/recommend/controller/RecommendController.java:33`, `:36`).  
수정 방향: 새 생성/캐시 히트를 구분해서 캐시 히트는 200 OK로 반환.

[low] `/ai` 확인 화면이 history.state에만 의존해 새로고침/직접 진입 시 조건이 사라짐  
왜: 조건은 라우터 state로만 전달됩니다(`frontend/src/views/AiPlanInputView.vue:338`, `:339`). `AiInputView`는 mount 때 `history.state?.conditions`만 읽습니다(`frontend/src/views/AiInputView.vue:142`, `:147`). 없으면 생성 시 에러만 보여줍니다(`frontend/src/views/AiInputView.vue:171`, `:174`).  
수정 방향: 조건을 Pinia/sessionStorage에 저장하거나, `/ai` 직접 진입 시 `/ai/plan`으로 즉시 redirect.

[low] 테스트가 실제 save-plan 생성 실패를 가림  
왜: 추천 테스트는 `PlanService`를 mock으로 둡니다(`BE/src/test/java/com/trip/recommend/service/RecommendServiceTest.java:57`). save-plan 테스트도 이미 `savedPlanId`가 있는 경로만 검증합니다(`BE/src/test/java/com/trip/recommend/service/RecommendServiceTest.java:252`, `:275`). 실제 `createFromDraft`에서 장소가 모두 스킵되는 경로는 안 잡힙니다.  
수정 방향: unsaved recommendation 저장 통합 테스트 추가. TourAPI detail 실패, 긴 reason, 동시 save-plan, corrupt resultJson 케이스를 포함.