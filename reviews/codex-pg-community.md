리뷰만 수행했고 코드 수정은 하지 않았습니다. 참고로 `frontend/src/stores/community.js`는 없어서 실제 커뮤니티 스토어인 `frontend/src/stores/posts.js`를 기준으로 봤습니다.

- [high] 수정 시 기존 이미지 파일을 삭제하고 깨진 URL만 다시 저장함 / 근거: `PostWriteView.vue:180`, `PostWriteView.vue:149`, `CommunityService.java:99`, `CommunityService.java:229`, `FileStorageService.java:49` / 왜: 수정 화면이 기존 이미지 URL을 `imageUrls`로 보내면 BE가 기존 파일을 삭제한 뒤 같은 URL을 다시 DB에 저장해 이미지가 깨짐 / 수정: “이미지 유지”는 `imageUrls=null`로 보내거나 BE에서 기존 URL 비교 후 삭제 제외.

- [high] 게시글 작성 이미지 업로드 API를 우회함 / 근거: `PostWriteView.vue:119`, `PostWriteView.vue:127`, `PostWriteView.vue:149`, `api/index.js:99`, `CommunityService.java:216`, `PostImage.java:23` / 왜: 파일을 multipart 업로드하지 않고 base64 data URL을 DB `imageUrl`에 저장한다. `PostImage.imageUrl`은 길이 지정 없는 기본 문자열 컬럼이라 긴 base64는 DB 오류/500 또는 DB 비대화를 유발함 / 수정: `communityApi.uploadImage()`로 먼저 업로드하고 반환된 `/uploads/...` URL만 `imageUrls`에 저장.

- [high] 업로드 파일 검증이 MIME 헤더만 믿고 확장자는 원본 파일명에서 가져옴 / 근거: `FileStorageService.java:34`, `FileStorageService.java:35`, `FileStorageService.java:66`, `FileStorageService.java:75`, `SecurityConfig.java:67`, `WebConfig.java:44` / 왜: 요청자가 `Content-Type: image/png`, 파일명 `x.html`로 보내면 `uuid.html`이 `/uploads/**`에서 같은 origin으로 공개될 수 있음 / 수정: 확장자 allowlist, magic-byte 검증, 서버가 MIME 기준 확장자를 결정, 업로드 정적 리소스에 안전한 content-type/attachment 정책 적용.

- [high] 댓글 수정/삭제/좋아요가 URL의 `postId`를 검증하지 않음 / 근거: `CommunityController.java:113`, `CommunityController.java:119`, `CommunityController.java:131`, `CommunityController.java:138`, `CommunityController.java:141`, `CommunityController.java:147`, `CommunityService.java:156`, `CommunityService.java:199`, `CommunityService.java:208` / 왜: `/posts/{다른글}/comments/{commentId}`로 호출해도 commentId만 기준으로 동작한다. 삭제된 게시글의 댓글도 parent post 검증 없이 좋아요/수정/삭제 가능 / 수정: `commentId + postId + post.deleted=false`로 조회하고, 서비스 메서드에 `postId`를 전달.

- [high] 좋아요/조회수 카운터가 동시성에서 유실됨 / 근거: `CommunityService.java:66`, `CommunityService.java:139`, `CommunityService.java:168`, `Post.java:58`, `Post.java:62`, `Comment.java:48` / 왜: 같은 게시글에 여러 사용자가 동시에 좋아요/조회하면 엔티티 int 증가 방식이라 마지막 커밋이 이전 증가분을 덮을 수 있음 / 수정: 원자적 update 쿼리, pessimistic/optimistic lock, 또는 카운트는 like 테이블 집계로 계산.

- [med] 목록 조회가 size 무제한 + N+1 쿼리 구조 / 근거: `CommunityController.java:49`, `CommunityService.java:38`, `CommunityService.java:53`, `CommunityService.java:54`, `PostSummaryResponse.java:25` / 왜: 사용자가 큰 `size`를 주면 게시글마다 댓글 수, 썸네일, author lazy load가 추가 쿼리로 터짐 / 수정: size 상한 검증, fetch join/projection, 댓글 수·썸네일 batch 조회.

- [med] 게시글/댓글 입력 검증 부재로 400 대신 500 가능 / 근거: `CommunityController.java:66`, `CommunityController.java:126`, `PostCreateRequest.java:8`, `CommentCreateRequest.java:4`, `Post.java:26`, `Comment.java:29`, `GlobalExceptionHandler.java:167` / 왜: `title/content/category/content` null·초과 길이가 DTO에서 걸러지지 않고 DB 제약까지 내려가 generic 500이 될 수 있음 / 수정: `@Valid`, `@NotBlank`, `@Size`, enum 필수 검증, `DataIntegrityViolationException` 400 매핑.

- [med] 실패를 mock/성공 라우팅으로 숨김 / 근거: `posts.js:71`, `posts.js:84`, `posts.js:95`, `posts.js:119`, `PostWriteView.vue:160`, `PostWriteView.vue:163` / 왜: API 실패 시 가짜 게시글/댓글을 보여주거나 작성 실패 후에도 목록으로 이동해 사용자가 저장 성공으로 오인함 / 수정: 개발 mock은 명시 플래그로만 사용하고 운영 경로는 에러 상태/토스트/재시도 제공.

- [med] FE 입력 필드와 BE DTO 계약 불일치 / 근거: `PostWriteView.vue:90`, `PostWriteView.vue:91`, `PostWriteView.vue:145`, `PostCreateRequest.java:8`, `PostDetailView.vue:76`, `PostDetailView.vue:85` / 왜: 작성 화면은 장소·태그를 입력받지만 payload/BE DTO에는 없어 저장되지 않는다. 상세/카드는 `post.location`, `post.tags`를 렌더링함 / 수정: BE 엔티티/DTO에 필드를 추가하거나 FE에서 해당 입력·표시 제거.

- [med] 핫플 관리자 pending GET이 공개 매처에 걸리고 principal NPE 가능 / 근거: `SecurityConfig.java:61`, `HotPlaceController.java:71`, `HotPlaceController.java:76` / 왜: `/community/hotplaces/*` GET permitAll이 `/community/hotplaces/pending`에도 매칭될 수 있고, 컨트롤러는 `principal.userId()`를 바로 호출함 / 수정: `/community/hotplaces/pending`을 인증/ADMIN 매처로 먼저 선언하고 컨트롤러에서도 null 방어.

- [low] 댓글 좋아요 UI가 API와 연결되지 않음 / 근거: `PostDetailView.vue:125`, `PostDetailView.vue:129`, `api/index.js:112`, `CommunityController.java:113` / 왜: 댓글 좋아요 버튼은 렌더링되지만 클릭 핸들러가 없어 기능이 죽어 있음 / 수정: `likeComment(postId, commentId)` 호출과 optimistic/pending/error 상태를 붙임.

TourAPI/OpenAI/Redis Vector는 이 커뮤니티 CRUD/댓글/좋아요/이미지 업로드 경로에서 직접 호출되는 코드는 확인되지 않았습니다.