리뷰만 수행했고 수정은 하지 않았습니다.

[HIGH] 보호 라우트 가드 누락  
왜: `/chat`, `/chat/:id`가 `requiresAuth` 없이 열려 있는데, 진입 즉시 보호 REST/STOMP를 호출합니다. `frontend/src/router/index.js:40-41`, `frontend/src/views/ChatRoomView.vue:517-524`. `/mypage/challenge`, `/mypage/album/:id`도 마이페이지 하위인데 가드가 없습니다. `frontend/src/router/index.js:60-61`  
수정: 해당 라우트에 `requiresAuth`를 붙이고, admin성 화면은 role meta까지 검사.

[HIGH] Explore 로딩 영구 고착  
왜: `ExploreView`가 직접 `store.loading = true`로 만들고, 위치 거부/실패 시 기존 attraction 데이터가 있으면 아무 요청도 하지 않아 `loading=false`로 돌아갈 경로가 없습니다. `frontend/src/views/ExploreView.vue:282-285`, `frontend/src/views/ExploreView.vue:412-413`. Home은 먼저 attraction 데이터를 채울 수 있습니다. `frontend/src/views/HomeView.vue:202-204`  
수정: 수동 `store.loading=true` 제거 또는 denied 분기에서 항상 `loadAttractions()`/`loading=false`.

[HIGH] attraction 목록 요청 레이스  
왜: 캐시 SWR 재조회와 일반 조회 모두 “현재 요청인지” 확인 없이 `applyRaw`로 상태를 덮습니다. `frontend/src/stores/attraction.js:260-267`, `frontend/src/stores/attraction.js:275-277`. 검색/카테고리/복원은 연속으로 `store.list`를 호출합니다. `frontend/src/views/ExploreView.vue:308`, `frontend/src/views/ExploreView.vue:327`, `frontend/src/views/ExploreView.vue:347`  
수정: request id/cache key 비교 또는 AbortController로 최신 요청만 반영.

[HIGH] 댓글 작성 실패를 성공으로 위장  
왜: `createComment` 실패 시 서버 저장 없이 로컬 mock 댓글을 추가합니다. 401/네트워크/500도 사용자는 저장 성공처럼 봅니다. `frontend/src/stores/posts.js:119-131`  
수정: 실패를 throw/상태로 노출하고, 낙관 업데이트를 하려면 pending + 실패 rollback.

[HIGH] 홈 즐겨찾기 버튼 no-op/비영속  
왜: Home은 `PlaceCard`에 `bookmarked` prop을 넘기지 않고 로컬 `place.bookmarked`만 토글합니다. `frontend/src/views/HomeView.vue:49`, `frontend/src/views/HomeView.vue:175-178`. 실제 API는 존재합니다. `frontend/src/api/index.js:60-63`  
수정: `favoriteApi.toggle('ATTRACTION', id)` 호출, 실패 rollback, prop 바인딩.

[MEDIUM] 프로필 이미지 변경 UI 미구현  
왜: 카메라/사진 변경 버튼에 핸들러와 file input이 없습니다. `frontend/src/views/ProfileEditView.vue:22`, `frontend/src/views/ProfileEditView.vue:28`. 저장은 기존 `profileImageUrl`만 PATCH합니다. `frontend/src/views/ProfileEditView.vue:90-94`. 업로드 helper는 별도로 존재합니다. `frontend/src/api/index.js:23-29`  
수정: 파일 선택 → `userApi.uploadProfileImage` → `authStore.fetchMe()` 흐름 연결.

[MEDIUM] 프로필 입력검증이 힌트와 불일치  
왜: UI는 닉네임 `2~12자, 한글·영문·숫자`라고 표시하지만 저장 전 trim/pattern/min 검사가 없습니다. `frontend/src/views/ProfileEditView.vue:35-41`, `frontend/src/views/ProfileEditView.vue:90-93`. BE DTO도 제약이 없습니다. `BE/src/main/java/com/trip/user/dto/UserUpdateRequestDto.java:3-7`  
수정: FE/BE 양쪽에 동일한 길이·문자셋 검증 추가.

[MEDIUM] mock fallback이 여전히 실패를 은폐  
왜: 조회 실패 시 실제 장애 대신 mock/seed를 반환합니다. `frontend/src/stores/attraction.js:278-281`, `frontend/src/stores/hotplace.js:101-104`, `frontend/src/stores/places.js:19-21`, `frontend/src/stores/posts.js:71-74`, `frontend/src/stores/posts.js:84-97`  
수정: demo seed는 명시 라벨로만 쓰고, API 실패는 error state로 노출.

[MEDIUM] SSE fetch helper가 axios refresh/timeout 정책을 우회  
왜: `connectStream`/`chatStream`은 직접 `fetch`를 써서 401 refresh interceptor를 타지 않고, stream timeout도 없습니다. `frontend/src/api/index.js:123-134`, `frontend/src/api/index.js:336-347`; refresh는 axios 인스턴스에만 있습니다. `frontend/src/api/http.js:49-80`  
수정: 공용 authenticated fetch wrapper로 401 refresh/retry와 idle timeout 적용.

[MEDIUM] hotplace category 필터가 실제로 동작하지 않음  
왜: `places.js`는 `category` 파라미터를 보내지만 BE hotplace 목록 컨트롤러는 Pageable만 받고 category를 받지 않습니다. `frontend/src/stores/places.js:17`, `BE/src/main/java/com/trip/community/controller/HotPlaceController.java:28-32`  
수정: BE에 category param 추가하거나 FE 필터 UI/호출 제거.

[LOW] 축제 섹션 라벨과 조회 조건 불일치  
왜: 화면은 “진행중인 축제”라고 쓰지만 status 없이 전체 non-ended를 조회합니다. `frontend/src/views/ExploreView.vue:139`, `frontend/src/views/ExploreView.vue:401`, `BE/src/main/java/com/trip/festival/controller/FestivalController.java:21-29`  
수정: `status=ONGOING` 전달 또는 라벨을 “진행/예정 축제”로 변경.

[LOW] 채팅 날짜 구분자가 하드코딩  
왜: 모든 방/날짜에서 `6월 10일 화요일`이 고정 표시됩니다. `frontend/src/views/ChatRoomView.vue:46-48`  
수정: 메시지 timestamp 기준으로 날짜 그룹을 계산해 렌더.