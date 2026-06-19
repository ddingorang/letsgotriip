검토 범위 내 `frontend/src/stores/community.js`는 존재하지 않음.

**HIGH**
1. `[high] 핫플 등록 실패를 성공처럼 위장`  
`frontend/src/views/HotplaceRegisterView.vue:287`  
왜: `create()` 실패 시 로컬 배열에 가짜 핫플을 push하고 `registrationSuccess=true`, `router.back()` 처리. 서버 미저장인데 성공 UI가 됨.  
수정: catch에서 가짜 저장 제거, 오류 표시 후 화면 유지.

2. `[high] API 실패를 목업 데이터로 덮음`  
`frontend/src/stores/posts.js:71`, `frontend/src/stores/posts.js:84`, `frontend/src/stores/posts.js:95`, `frontend/src/stores/posts.js:123`, `frontend/src/stores/hotplace.js:101`, `frontend/src/stores/hotplace.js:115`  
왜: 게시글/댓글/핫플 조회·댓글작성 실패가 mock/seed로 대체되어 BE 장애와 미구현이 정상처럼 보임.  
수정: mock fallback은 dev flag로 제한하고, 운영 흐름은 error/empty 상태를 노출.

3. `[high] 게시글 목록 좋아요 상태 계약 누락`  
`BE/src/main/java/com/trip/community/dto/PostSummaryResponse.java:9`, `BE/src/main/java/com/trip/community/controller/CommunityController.java:45`, `frontend/src/components/community/PostCard.vue:41`  
왜: 목록 DTO에 `likedByMe`가 없고 목록 API도 principal을 받지 않는데 FE는 `post.likedByMe`로 하트 상태를 그림. 이미 좋아요한 글도 미좋아요처럼 보이고 클릭 시 toggle로 좋아요가 해제될 수 있음.  
수정: 목록 API에 인증 사용자 optional 반영, `PostSummaryResponse.likedByMe` 추가. “좋아요한 글 보기” API도 별도 제공.

4. `[high] 댓글 nested URL의 postId 무시`  
`BE/src/main/java/com/trip/community/controller/CommunityController.java:113`, `BE/src/main/java/com/trip/community/controller/CommunityController.java:131`, `BE/src/main/java/com/trip/community/controller/CommunityController.java:141`, `BE/src/main/java/com/trip/community/service/CommunityService.java:168`  
왜: `/posts/{postId}/comments/{commentId}`인데 서비스는 `commentId`만 사용. 다른 게시글의 댓글 ID를 끼워 넣어도 like/update/delete 대상 검증이 URL과 불일치.  
수정: `postId`를 서비스로 전달하고 `findByIdAndPostIdAndDeletedFalse`로 검증.

5. `[high] 비공개 핫플 상세 노출 가능`  
`BE/src/main/java/com/trip/global/config/SecurityConfig.java:77`, `BE/src/main/java/com/trip/community/service/HotPlaceService.java:40`, `BE/src/main/java/com/trip/community/service/HotPlaceService.java:147`  
왜: public GET 상세가 `findById`만 사용해 `PENDING/REJECTED`도 ID를 알면 조회 가능.  
수정: 공개 상세는 `status=APPROVED`만 조회, 작성자/관리자 조회는 별도 권한 분기.

**MED**
6. `[med] 팔로우 UI만 있고 기능 없음`  
`frontend/src/components/community/PostCard.vue:30`, `frontend/src/views/PostDetailView.vue:79`  
왜: `@click.stop` 또는 버튼만 있고 핸들러/API/store가 없음. 전체 검색상 follow 구현도 없음.  
수정: 팔로우 API/store 연결 또는 버튼 제거.

7. `[med] 북마크 UI가 Favorite API와 분리됨`  
`frontend/src/api/index.js:60`, `frontend/src/components/community/PostCard.vue:54`, `frontend/src/views/CommunityView.vue:48`, `frontend/src/views/PostDetailView.vue:223`, `frontend/src/views/HotplaceDetailView.vue:87`  
왜: Favorite API는 있으나 게시글/핫플 북마크 버튼은 미연결 또는 로컬 토글뿐. 새로고침/마이페이지와 상태 불일치.  
수정: `favoriteApi.toggle('POST'|'HOTPLACE', id)` 연결, 초기 favorited 상태 조회.

8. `[med] 댓글 좋아요/답글 버튼 사망`  
`frontend/src/api/index.js:253`, `frontend/src/views/PostDetailView.vue:125`, `frontend/src/views/PostDetailView.vue:131`  
왜: API는 `likeComment`가 있는데 store/view에서 호출하지 않음. 답글 버튼은 핸들러·BE 모델 모두 없음.  
수정: 댓글 좋아요 store/action 추가, 답글은 구현 전까지 UI 제거.

9. `[med] 게시글 수정/삭제 메뉴 소유자 UI 검증 없음`  
`frontend/src/views/PostDetailView.vue:30`, `frontend/src/views/PostDetailView.vue:41`, `frontend/src/views/PostDetailView.vue:49`, `BE/src/main/java/com/trip/community/dto/PostResponse.java:10`  
왜: 상세 DTO에 `authorId/isOwner`가 없어 FE가 소유자 판단 불가인데 메뉴는 항상 노출. BE는 막지만 UX상 실패 버튼.  
수정: DTO에 `authorId` 또는 `isOwner` 추가, FE에서 소유자만 메뉴 표시.

10. `[med] 페이지네이션/필터 계약 미사용`  
`BE/src/main/java/com/trip/community/controller/CommunityController.java:47`, `frontend/src/stores/posts.js:65`, `frontend/src/views/CommunityView.vue:268`, `frontend/src/stores/hotplace.js:95`, `frontend/src/views/CommunityView.vue:310`  
왜: BE는 category/page를 받지만 FE는 첫 페이지를 받아 클라이언트 필터만 수행. 댓글도 `frontend/src/stores/posts.js:91`에서 첫 Page content만 사용. 많은 데이터가 접근 불가.  
수정: 필터 변경 시 서버 파라미터로 재조회, 댓글/핫플 load-more 구현.

11. `[med] 핫플 사진 업로드 UI만 존재`  
`frontend/src/views/HotplaceRegisterView.vue:102`, `frontend/src/views/HotplaceRegisterView.vue:278`  
왜: 사진 추가 버튼에 핸들러가 없고 payload는 항상 `imageUrls: []`.  
수정: 업로드 플로우 연결 또는 사진 UI 제거.

12. `[med] 핫플 승인 플로우가 자기모순`  
`BE/src/main/java/com/trip/community/entity/HotPlace.java:49`, `BE/src/main/java/com/trip/community/service/HotPlaceService.java:51`, `BE/src/main/java/com/trip/community/controller/HotPlaceController.java:71`  
왜: 엔티티 기본은 `PENDING`, 관리자 pending/approve API도 있는데 등록 서비스가 즉시 `APPROVED`로 저장. 승인 기능이 사실상 죽음.  
수정: 승인제를 쓸 거면 등록은 `PENDING`; 아니면 pending/approve UI/API 제거.

**LOW**
13. `[low] 핫플 상세 CTA 일부 미구현`  
`frontend/src/views/HotplaceDetailView.vue:54`, `frontend/src/views/HotplaceDetailView.vue:182`  
왜: 길찾기 버튼은 핸들러가 없고, “내 일정에 추가”는 실제 추가 없이 `/plan` 이동만 함.  
수정: 지도 URL/계획 추가 API 연결 또는 문구 변경.