수정 없이 코드만 리뷰했습니다.

**실데이터 판정**
- `지금 뜨는 여행지`: 실시간 인기/트렌딩 아님. `HomeView.vue:45`, `HomeView.vue:116-117`은 `attractionStore.attractions` 앞 10개를 보여주고, 실패 시 `attraction.js:319-320` 목업을 넣습니다.
- 뉴스/충전소: 데모 표시 있음. `HomeView.vue:68`, `PlaceDetailView.vue:173`, BE도 `ContextController.java:22-23`에서 데모로 명시.

**Findings**
- [high] 목업 관광지가 API 실패를 성공 화면처럼 대체
  - 왜: `attraction.js:316-320`에서 실패 시 `MOCK_ATTRACTIONS`를 실제 목록에 주입. `HomeView.vue:49`는 에러/목업 구분 없이 카드 렌더.
  - 수정: 실패 시 목업 주입 제거. demo 플래그를 분리하고 Home/Explore/Detail에 명확한 error/empty 상태 표시.

- [high] 상세 페이지도 실패 시 seed/null을 정상 상세처럼 표시
  - 왜: `attraction.js:400-404`가 실패 시 mock seed 또는 `null` 반환. `PlaceDetailView.vue:396-399`는 에러를 띄우면서도 받은 값을 계속 화면에 사용.
  - 수정: 상세 실패는 not-found/error 상태로 분기. seed fallback은 개발 모드 전용으로 격리.

- [high] 리뷰 섹션은 실제 리뷰 API가 있는데 정적 seed 사용
  - 왜: `PlaceDetailView.vue:203`, `PlaceDetailView.vue:331`이 `STATIC_REVIEWS`만 렌더. 실제 API는 `api/index.js:47`, `ReviewController.java:22`, `ReviewController.java:28-30`에 존재.
  - 수정: `reviewApi.list(contentId)`로 로드하고 empty/loading/error 처리.

- [med] 북마크 버튼은 서버 반영 없는 죽은 UI
  - 왜: `PlaceDetailView.vue:26`, `PlaceDetailView.vue:251`은 local boolean만 토글. `HomeView.vue:49`, `HomeView.vue:175`도 store 객체만 바꾸며 `favoriteApi` 호출 없음.
  - 수정: 로그인 확인 후 `favoriteApi.toggle/remove/list` 연동. Home 카드에는 실제 bookmarked prop 바인딩.

- [med] Home 카테고리 칩이 카테고리를 전달하지 않음
  - 왜: `HomeView.vue:37` 모든 칩이 그냥 `/explore`로 이동. 카테고리 정의는 `HomeView.vue:156-170`에 있지만 선택값 전달 없음.
  - 수정: `/explore?category=39` 같은 query 또는 store preselect로 연결.

- [med] 검색 결과 페이지네이션 불가
  - 왜: 검색은 `ExploreView.vue:378`, `ExploreView.vue:384`에서 page 1만 조회하고, load-more sentinel은 `ExploreView.vue:139`에서 `!searchQuery.trim()`일 때만 표시.
  - 수정: 검색 상태에서도 `store.loadMore()` 허용하거나 명시적 페이지네이션 제공.

- [med] `hasMore` 판정이 부정확함
  - 왜: BE는 `AttractionController.java:30`처럼 `List`만 반환하고 total 없음. FE는 `attraction.js:297`, `attraction.js:314`에서 `raw.length > 0`이면 계속 더 있다고 판단.
  - 수정: `raw.length === size`로 임시 판정하거나 BE를 Page 응답으로 변경.

- [med] `places.js`는 계약 불일치 + 미사용 dead store
  - 왜: `places.js:12`, `places.js:17`은 한글 category를 보냄. BE `HotPlaceController.java:29-30`은 category 파라미터를 받지 않고, enum은 `HotPlaceCategory.java:5-10`의 영문 값. `usePlacesStore` 사용처도 자기 파일뿐.
  - 수정: 미사용이면 삭제. 사용할 거면 BE 필터 추가 및 enum 값 매핑.

- [low] Festival 실패/빈 상태가 화면에서 사라짐
  - 왜: `festival.js:23-25`는 실패 시 error 저장 후 빈 배열. `ExploreView.vue:153`은 축제 배열이 있을 때만 섹션 렌더, `ExploreView.vue:500` 호출 실패는 사용자에게 보이지 않음.
  - 수정: festival loading/error/empty 상태를 별도 표시.

- [low] “다시 추천”은 구현 없는 alert
  - 왜: `PlaceDetailView.vue:101` 버튼이 `getRecommendation`, `PlaceDetailView.vue:386-387`은 alert만 실행.
  - 수정: 추천 API로 이동/호출하거나 버튼 제거/비활성화.

- [low] PlaceDetail 미니맵 마커 클릭 이벤트가 버려짐
  - 왜: `PlaceDetailView.vue:114`의 `TripMap`에는 `@select/@detail` 바인딩 없음. `TripMap.vue:119`는 marker click 시 `select` emit.
  - 수정: 미니맵을 비상호작용으로 명시하거나 `@select/@detail` 핸들러 연결.