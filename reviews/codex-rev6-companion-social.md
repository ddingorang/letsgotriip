수정 없이 코드 리뷰만 했습니다.

1. [high] 리뷰 화면이 정적 시드로 성공처럼 보임  
파일: `frontend/src/views/PlaceDetailView.vue:196`, `frontend/src/views/PlaceDetailView.vue:203`, `frontend/src/views/PlaceDetailView.vue:330`, `frontend/src/api/index.js:47`  
왜: BE 리뷰 API가 있는데 상세 화면은 `STATIC_REVIEWS`만 렌더링한다. 실제 작성/수정/삭제/목록 조회 UI가 없다.  
수정: `reviewApi.list/create/update/remove`를 상세 화면에 연결하고 정적 리뷰 제거.

2. [high] “내 리뷰”는 기능이 아니라 안내 시트뿐  
파일: `frontend/src/views/MyPageView.vue:219`, `frontend/src/views/MyPageView.vue:385`, `frontend/src/views/MyPageView.vue:562`  
왜: 내 리뷰 버튼은 실제 리뷰 목록을 조회하지 않고 “관광지 상세에서 확인” 안내만 보여준다.  
수정: BE에 내 리뷰 목록 API를 만들거나 FE가 실제 리뷰 데이터를 조회해 표시.

3. [high] 찜 버튼들이 DB에 저장되지 않음  
파일: `frontend/src/views/PlaceDetailView.vue:26`, `frontend/src/views/PlaceDetailView.vue:251`, `frontend/src/views/CommunityView.vue:158`, `frontend/src/views/PostDetailView.vue:223`  
왜: 버튼은 로컬 boolean 토글이거나 핸들러가 없다. `favoriteApi.toggle`이 실제 화면에 연결되지 않았다.  
수정: 공용 favorite store/composable로 `favoriteApi.toggle/remove/list` 연결.

4. [high] 동행 신청 중복 레이스 방어가 거짓임  
파일: `BE/src/main/java/com/trip/companion/entity/CompanionApplication.java:12`, `BE/src/main/java/com/trip/companion/service/CompanionService.java:160`  
왜: 서비스는 DB unique 충돌을 기대하지만 `companion_applications`에 `(post,user,status)` 유니크 제약이 없다. 동시 신청 두 건이 모두 저장될 수 있다.  
수정: DB 제약 추가 또는 post+applicant 락으로 중복 신청을 원자적으로 차단.

5. [high] 정원 찬 동행에도 신청 CTA가 열려 있음  
파일: `BE/src/main/java/com/trip/companion/service/CompanionService.java:157`, `BE/src/main/java/com/trip/companion/service/CompanionService.java:209`, `frontend/src/views/CompanionDetailView.vue:173`  
왜: BE 신청 단계는 상태만 보고 정원은 승인 단계에서만 본다. FE도 남은 자리 0이어도 신청 버튼을 노출한다.  
수정: 신청 단계와 FE CTA 모두 정원 초과를 차단하고, 정원 도달 시 모집 상태를 닫기.

6. [med] 동행 상세 실패가 가짜 상세 화면으로 보임  
파일: `frontend/src/views/CompanionDetailView.vue:217`, `frontend/src/views/CompanionDetailView.vue:367`, `frontend/src/stores/companion.js:123`  
왜: 상세 조회 실패 시 기본 객체 `동행 모집`을 렌더링한다. 404/500도 정상 상세처럼 보인다.  
수정: 로딩/에러/404 상태를 분리하고 실패 시 CTA 비활성화.

7. [med] 신청자 관리 403/실패가 빈 화면으로 위장됨  
파일: `frontend/src/views/CompanionApplicantsView.vue:23`, `frontend/src/views/CompanionApplicantsView.vue:66`, `frontend/src/stores/companion.js:199`  
왜: store가 신청자 조회 실패를 삼키고, 화면은 에러/빈상태/권한없음 표시 없이 v-for만 렌더링한다.  
수정: 상세 조회로 소유자 확인, 403/empty/loading 상태 표시, 실패를 화면에 전달.

8. [med] 리뷰 수정/삭제가 URL의 `contentId`를 무시함  
파일: `BE/src/main/java/com/trip/review/controller/ReviewController.java:44`, `BE/src/main/java/com/trip/review/controller/ReviewController.java:54`, `BE/src/main/java/com/trip/review/service/ReviewService.java:72`  
왜: `/attractions/{contentId}/reviews/{reviewId}` 계약인데 서비스는 `reviewId + userId`만 검증한다. 다른 관광지 경로로도 자기 리뷰를 수정/삭제할 수 있다.  
수정: `findByIdAndUserIdAndContentId`로 경로 리소스 일치 검증.

9. [med] 리뷰/찜 목록에 페이지네이션 없음  
파일: `BE/src/main/java/com/trip/review/controller/ReviewController.java:28`, `BE/src/main/java/com/trip/review/service/ReviewService.java:32`, `BE/src/main/java/com/trip/favorite/service/FavoriteService.java:62`  
왜: 리뷰와 찜 모두 전체 List를 반환한다. 데이터가 커지면 화면과 API가 같이 느려진다.  
수정: `Pageable`/cursor 응답과 FE load-more 상태 추가.

10. [med] 동행 목록은 BE cursor를 FE가 버림  
파일: `BE/src/main/java/com/trip/companion/service/CompanionService.java:114`, `frontend/src/stores/companion.js:103`, `frontend/src/views/CommunityView.vue:205`  
왜: BE는 `nextCursor/hasNext`를 주지만 store는 `content`만 저장하고 다음 페이지 상태가 없다. 화면도 empty/error/loading이 없다.  
수정: `nextCursor`, `hasMore`, `loadMore`, 빈상태/에러상태 구현.

11. [med] 팔로우 UI만 있고 기능 없음  
파일: `frontend/src/components/community/PostCard.vue:30`, `frontend/src/views/PostDetailView.vue:79`  
왜: 팔로우 버튼에 실제 핸들러/API가 없다. 소셜 기능이 있는 것처럼 보인다.  
수정: follow API/store를 만들거나 버튼 제거.

12. [low] 죽은 컨트롤이 남아 있음  
파일: `frontend/src/views/CommunityView.vue:8`, `frontend/src/views/CommunityView.vue:200`, `frontend/src/components/community/PostCard.vue:47`  
왜: 검색, 최신순 정렬, 댓글 아이콘 버튼이 클릭 가능해 보이지만 동작이 없다.  
수정: 실제 핸들러 연결 또는 비활성/제거.