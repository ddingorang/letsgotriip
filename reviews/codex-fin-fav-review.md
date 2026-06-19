[HIGH] 리뷰 1인1리뷰 보장이 레이스에 깨짐  
근거: `AttractionReview.java:15`는 `@Table(name = "attraction_reviews")`만 있고 `(user_id, content_id)` unique가 없음. `ReviewService.java:50-58`은 `existsByUserIdAndContentId()` 후 `save()`라 동시 요청 2개가 모두 통과 가능.  
왜: 같은 사용자가 같은 관광지에 리뷰를 중복 생성할 수 있음.  
수정: DB unique `(user_id, content_id)` 추가, 중복키는 409로 변환.

[HIGH] 즐겨찾기 토글은 멱등이 아님 + 동시성 500 가능  
근거: `FavoriteService.java:25-38`은 존재하면 삭제, 없으면 생성. 재시도 1회가 상태를 원복함. 동시에 생성하면 `Favorite.java:18-20` unique 충돌이 나지만 `GlobalExceptionHandler.java:167-175`의 generic 500으로 떨어짐.  
왜: 네트워크 재시도/더블클릭이 “찜 추가”를 “찜 해제”로 바꿈.  
수정: `PUT /favorites/{type}/{id}` 추가, `DELETE` 제거로 상태 지정형 API 사용. 중복키는 성공 또는 409로 명시 처리.

[MEDIUM] 리뷰 수정/삭제가 URL의 `contentId`를 검증하지 않음  
근거: `ReviewController.java:44`, `ReviewController.java:54`에서 `contentId`를 받지만 `ReviewService.java:64-76`은 `reviewId + userId`만 조회. `AttractionReviewRepository.java:17`도 `findByIdAndUserId`뿐.  
왜: `/api/attractions/B/reviews/{A리뷰id}`로 A 관광지 리뷰를 수정/삭제 가능.  
수정: `findByIdAndUserIdAndContentId`로 부모 리소스 일치 검증.

[MEDIUM] 공개 리뷰 응답이 내부 `userId`를 노출함  
근거: GET `/api/attractions/**`는 공개(`SecurityConfig.java:60-61`). `ReviewResponse.java:12`가 `userId`를 포함하고, `ReviewService.java:39-40`이 공개 목록에 그대로 매핑.  
왜: 비로그인 사용자도 내부 사용자 PK를 수집 가능.  
수정: 공개 응답에서 `userId` 제거, 필요하면 별도 public author id 사용.

[MEDIUM] 존재하지 않는 대상에도 리뷰/즐겨찾기 저장 가능  
근거: `ReviewService.java:53-56`은 path `contentId`를 그대로 저장. `FavoriteService.java:32-35`도 `targetType/targetId`를 그대로 저장. 대상 조회/공개 상태 검증이 없음.  
왜: 삭제/비공개/없는 관광지·게시글·핫플에 데이터가 쌓임.  
수정: target type별 존재 및 공개 가능 상태를 검증하고 실패 시 404/403.

[MEDIUM] 길이 검증 누락으로 사용자 입력이 500을 만들 수 있음  
근거: `FavoriteToggleRequest.java:12-13`은 `@NotNull/@NotBlank`뿐인데 `Favorite.java:40`은 `target_id length=100`. `ReviewController.java:35`의 `contentId`도 검증 없고 `AttractionReview.java:29`는 `length=50`.  
왜: 초과 길이 입력은 검증 400이 아니라 DB 예외 후 500 가능.  
수정: DTO/PathVariable에 `@Size(max=...)`, 컨트롤러 `@Validated` 적용.

[MEDIUM] FE가 새 리뷰 API를 실제 상세 화면에 사용하지 않음  
근거: API 래퍼는 있음(`frontend/src/api/index.js:47-53`). 하지만 상세 화면은 `STATIC_REVIEWS` 렌더링(`frontend/src/views/PlaceDetailView.vue:203`, `:330-331`).  
왜: BE 리뷰 작성/평균 계산이 사용자 화면에 반영되지 않음.  
수정: 상세 화면에서 `reviewApi.list/create/update/remove` 연결.

[LOW] HTTP 상태가 REST 계약과 어긋남  
근거: 리뷰 생성은 `ResponseEntity.ok`(`ReviewController.java:39`), 삭제도 `ok`(`ReviewController.java:59`). 중복 리뷰는 `_BAD_REQUEST` 400(`ReviewService.java:51`).  
왜: 생성은 201, 삭제는 204, 중복은 409가 더 정확함.  
수정: `created(...)`, `noContent()`, conflict 전용 `ResponseCode` 사용.

[LOW] 리뷰 수정 응답의 `updatedAt`이 구값일 수 있음  
근거: `ReviewService.java:67-68`은 엔티티 수정 직후 DTO를 만들고, `BaseEntity.java:22-23`의 `@LastModifiedDate`는 flush/pre-update 시점에 갱신됨.  
왜: 응답 본문 `updatedAt`이 실제 커밋 후 값과 다를 수 있음.  
수정: flush 후 매핑하거나 수정 API는 204로 단순화.