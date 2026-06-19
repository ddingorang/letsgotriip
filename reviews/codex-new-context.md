지정 범위 정적 리뷰만 했고, 코드 수정은 하지 않았습니다.

[high] 필수 `lat/lng` 누락이 400이 아니라 500으로 처리될 수 있음  
왜 문제인지: `ContextController`는 필수 `@RequestParam double`을 받습니다: `BE/src/main/java/com/trip/context/controller/ContextController.java:40-41`, `:51-52`. 그런데 전역 핸들러에는 `MissingServletRequestParameterException` 처리가 없고, 남은 예외는 catch-all에서 500을 반환합니다: `BE/src/main/java/com/trip/global/error/exception/handler/GlobalExceptionHandler.java:167-179`. `?lat=...` 또는 `?lng=...` 누락은 명백한 클라이언트 입력 오류라 400이어야 합니다.  
수정 방향: `MissingServletRequestParameterException` 또는 `ServletRequestBindingException` 전용 핸들러를 추가해 `_BAD_REQUEST`로 반환.

[high] 좌표 검증이 불완전함: `NaN/Infinity` 통과, EV 엔드포인트는 범위 검증 없음  
왜 문제인지: 날씨는 단순 비교만 합니다: `BE/src/main/java/com/trip/context/service/WeatherService.java:28-29`. `NaN`은 비교식이 모두 false라 통과하고, 이후 Open-Meteo 요청 파라미터로 들어갑니다: `BE/src/main/java/com/trip/context/client/OpenMeteoClient.java:55-56`. 실패하면 500으로 변환됩니다: `:75-77`. EV는 아예 검증 없이 `nearby(lat, lng)`에서 좌표를 생성합니다: `BE/src/main/java/com/trip/context/service/EvStationService.java:41-50`, 그래서 `lat=999&lng=999` 같은 값도 200 + 가짜 좌표가 됩니다.  
수정 방향: 공통 좌표 validator로 `Double.isFinite`, 위도 `[-90,90]`, 경도 `[-180,180]`을 날씨/EV 모두에 적용하고 실패 시 400.

[med] Open-Meteo 장애/타임아웃을 내부 서버 오류 500으로 둔갑시킴  
왜 문제인지: 타임아웃은 설정되어 있지만 `connect 4s/read 5s`: `BE/src/main/java/com/trip/context/client/OpenMeteoClient.java:35-37`, 외부 호출 실패는 전부 `_INTERNAL_SERVER_ERROR`로 던집니다: `:75-77`. 외부 API 장애는 서버 내부 버그가 아니라 upstream 실패이므로 클라이언트/모니터링/재시도 정책이 잘못 판단합니다.  
수정 방향: `ResourceAccessException`, `RestClientResponseException` 등을 구분해 502/503/504 계열의 context용 응답 코드로 매핑. 잘못된 좌표성 upstream 4xx는 내부 검증으로 400 처리.

[med] Place 상세 라우트 재사용 시 이전 장소의 context 위젯이 남을 수 있음  
왜 문제인지: `PlaceDetailView`는 `onMounted`에서 한 번만 `route.params.id`를 읽고 context를 로드합니다: `frontend/src/views/PlaceDetailView.vue:390-408`. `watch`도 import/사용하지 않습니다: `:237`. 같은 컴포넌트 인스턴스에서 `/place/:id`만 바뀌면 새 장소 fetch/context reload가 보장되지 않고, 이전 weather/EV 상태가 남을 수 있습니다.  
수정 방향: `route.params.id`를 watch해서 `weather=null`, `evStations=[]` 초기화 후 재조회하고, 늦게 끝난 이전 요청은 request token/AbortController로 무시.

[low] 유효한 `0` 좌표를 “좌표 없음”으로 취급함  
왜 문제인지: `hasCoords`가 truthy 검사입니다: `frontend/src/views/PlaceDetailView.vue:263`. 지도/좌표 표시도 같은 방식입니다: `:115`, `:121`. 위도나 경도가 0인 유효 좌표는 context 위젯과 지도 표시가 꺼집니다.  
수정 방향: `Number.isFinite(Number(place.value?.lat)) && Number.isFinite(Number(place.value?.lng))` 기준으로 판정.

[low] 뉴스 데모 URL `#`가 실제 링크처럼 렌더링됨  
왜 문제인지: BE가 모든 뉴스 URL을 `"#"`로 내려줍니다: `BE/src/main/java/com/trip/context/service/NewsService.java:19`, `:27-50`. FE는 truthy URL이면 `href`, `_blank`, 화살표를 붙입니다: `frontend/src/views/HomeView.vue:77-89`. 클릭 시 의미 없는 `#` 링크가 새 탭으로 열립니다.  
수정 방향: 데모 URL은 `null`로 내려주거나, FE에서 `http://`/`https://`만 링크로 렌더링.

[low] 공개 context 보안 매처가 HTTP 메서드 제한 없이 열려 있음  
왜 문제인지: 현재 컨트롤러는 GET만 제공합니다: `BE/src/main/java/com/trip/context/controller/ContextController.java:38`, `:49`, `:60`. 하지만 보안 설정은 `/api/context/**` 전체 메서드를 `permitAll`로 엽니다: `BE/src/main/java/com/trip/global/config/SecurityConfig.java:59`. 지금 당장 권한 우회는 아니지만, 같은 prefix에 쓰기 엔드포인트가 추가되면 자동 공개됩니다.  
수정 방향: `.requestMatchers(HttpMethod.GET, "/api/context/**").permitAll()`로 좁히고 나머지는 기본 인증/거부 정책에 맡기기.

테스트는 실행하지 않았습니다. 요청대로 리뷰 전용으로 정적 근거만 확인했습니다.