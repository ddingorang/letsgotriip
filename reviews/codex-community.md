리뷰만 수행했고 코드는 수정하지 않았습니다. 범위 밖 파일은 보안/배포 근거 확인용으로만 봤습니다.

1. [high] 업로드 검증 우회로 임의 확장자 공개 파일 저장 가능  
근거: `CommunityController.java:34-40`, `FileStorageService.java:22,34-36,66,71-75`, `SecurityConfig.java:65`  
왜 문제: MIME만 보고 통과시키고 확장자는 원본 파일명에서 그대로 가져옵니다. `file.html`을 `Content-Type: image/png`으로 올리면 `/uploads/community/*.html`이 공개됩니다.  
재현/수정: multipart의 파일명/Content-Type을 조작해 업로드. 실제 magic-byte 검증, 확장자 allowlist 강제, 저장 확장자는 감지된 타입에서만 결정.

2. [high] 게시글 이미지 업로드 계약이 깨져 base64가 DB로 들어감  
근거: `PostWriteView.vue:122-127,145-149`, `CommunityController.java:34-40`, `frontend/src/api/index.js:96-108`, `PostImage.java:23-24`  
왜 문제: FE는 `/community/images` 업로드 API를 호출하지 않고 `FileReader.readAsDataURL()` 결과를 `imageUrls`로 보냅니다. BE는 이를 URL로 간주해 그대로 저장합니다. 이미지 URL 컬럼은 TEXT/length 지정도 없습니다.  
재현/수정: 글 작성에서 사진 선택 후 등록. `communityApi.uploadImage(FormData)`를 추가해 반환된 `/uploads/community/...`만 저장하고, BE DTO에서도 `data:`/외부 URL 정책을 검증.

3. [high] 핫플 승인 플로우가 우회됨  
근거: `HotPlace.java:48-49`, `HotPlaceService.java:51-62`, `HotPlaceController.java:71-84`  
왜 문제: 엔티티 기본값과 관리자 pending/approve API는 승인 대기 모델인데, 일반 등록은 즉시 `APPROVED`로 저장됩니다. 정상 등록분은 관리자 승인 대기 목록에 절대 들어가지 않습니다.  
재현/수정: 일반 사용자로 `POST /community/hotplaces` 후 바로 목록 노출 확인. 등록은 `PENDING`, 공개 목록은 `APPROVED`만, 관리자 승인 후 노출로 맞추기.

4. [high] 비승인/반려 핫플 상세가 공개 ID로 조회됨  
근거: `HotPlaceController.java:35-37`, `HotPlaceService.java:40-42,147-149`, `SecurityConfig.java:61`  
왜 문제: 목록은 승인 핫플만 조회하지만 상세는 `findById`만 합니다. GET 상세가 비회원 공개라 `PENDING`/`REJECTED`도 ID만 알면 볼 수 있습니다.  
재현/수정: 관리자 반려 후 `GET /community/hotplaces/{id}` 호출. 공개 상세는 `status=APPROVED`만 허용하고, submitter/admin 예외는 별도 권한 분기.

5. [high] FE가 생성 실패를 성공처럼 처리함  
근거: `PostWriteView.vue:159-164`, `HotplaceRegisterView.vue:283-302`  
왜 문제: 게시글 등록 실패 시 그냥 `/community`로 이동하고, 핫플 등록 실패 시 로컬 store에 가짜 항목을 넣고 `registrationSuccess=true`로 처리합니다. 실제 서버 저장 실패를 사용자가 성공으로 오인합니다.  
재현/수정: 토큰 만료/서버 500 상태에서 등록. catch에서 성공 처리 금지, 오류 표시 후 재시도 가능 상태 유지.

6. [med] 좋아요/조회수 카운터가 동시성에 취약함  
근거: `CommunityService.java:66,131-139,160-168`, `Post.java:58-67`, `Comment.java:48-53`, `PostLike.java:9-11`  
왜 문제: 엔티티 int를 읽고 `++/--` 하는 방식이고 락/버전/원자 update가 없습니다. 동시 좋아요는 unique 충돌 500 또는 lost update가 날 수 있고 조회수도 누락됩니다.  
재현/수정: 같은 게시글에 동시 좋아요/조회 요청. DB 원자 update, 낙관락, unique 충돌 409 처리 중 하나로 정리.

7. [med] 입력 검증이 거의 없어 DB 예외/쓰레기 데이터가 들어감  
근거: `PostCreateRequest.java:8-12`, `CommentCreateRequest.java:4-5`, `HotPlaceCreateRequest.java:10-16`, `CommunityController.java:63-78,122-138`, `HotPlaceController.java:42-57`  
왜 문제: 게시글/댓글은 `@Valid`도 제약도 없고, 핫플은 category만 `@NotNull`입니다. null/blank/과길이/좌표 범위 오류가 서비스까지 들어갑니다.  
재현/수정: 빈 제목, null category, 1000자 초과 제목, 위도 999 전송. DTO에 `@NotBlank`, `@Size`, 좌표 범위, 이미지 개수/URL 제약 추가하고 컨트롤러에 `@Valid`.

8. [med] 목록/댓글 조회에 명확한 N+1 쿼리 패턴  
근거: `CommunityService.java:51-57,146-152`, `PostSummaryResponse.java:21-31`, `CommentResponse.java:19-25`, `HotPlaceService.java:35-37,142-144`  
왜 문제: 게시글마다 댓글 count, 썸네일, lazy author를 개별 조회합니다. 댓글도 각 댓글마다 liked 여부와 author 접근이 추가됩니다. 핫플도 항목마다 사진 전체 조회 후 첫 장만 사용합니다.  
재현/수정: 10개 목록 조회 시 SQL 로그 확인. fetch join/entity graph, count/thumbnail projection, batch query로 묶기.

9. [med] 댓글 하위 리소스의 `postId`를 무시함  
근거: `CommunityController.java:113-119,131-147`, `CommunityService.java:156-170,198-211`  
왜 문제: URL은 `/posts/{postId}/comments/{commentId}`인데 서비스는 `commentId`만 검증합니다. 다른 게시글의 댓글도 잘못된 post 경로로 좋아요/수정/삭제 요청이 성립합니다.  
재현/수정: 댓글 B의 id로 `/posts/A/comments/B/likes` 호출. `comment.post.id == postId` 검증 또는 repository 메서드를 `findByIdAndPostId...`로 변경.

10. [med] 핫플 사진 UI/API가 사실상 미구현  
근거: `HotplaceRegisterView.vue:99-109,271-280`, `HotPlaceCreateRequest.java:16`, `HotPlaceService.java:121-130`  
왜 문제: 사진 추가 버튼은 input/handler가 없고 payload는 항상 `imageUrls: []`입니다. BE는 사진 저장 로직을 갖고 있지만 FE에서 도달할 수 없습니다.  
재현/수정: 핫플 등록 화면에서 사진 추가 클릭. 파일 input, `/community/images` 업로드, 반환 URL을 `imageUrls`에 넣는 플로우 추가.

11. [med] docker compose 배포에서 업로드 파일이 영속화되지 않음  
근거: `application.yaml:104-107`, `BE/Dockerfile:21-30`, `docker-compose.yml:64-108,121-125`  
왜 문제: 업로드 기본 경로는 컨테이너 `/app/uploads/community`인데 backend 서비스에 업로드 volume이 없습니다. 컨테이너 재생성 시 `/uploads/community/...` DB URL은 남고 실제 파일은 사라집니다.  
재현/수정: 이미지 업로드 후 `docker compose up -d --build --force-recreate backend`. backend에 `/app/uploads` 볼륨 마운트 추가.

12. [low] 생성 HTTP 상태코드가 일관되지 않음  
근거: `CommunityController.java:68-69,122-128`, `HotPlaceController.java:47-48`  
왜 문제: 게시글/핫플 생성은 201 + Location인데 댓글 생성은 200입니다. 클라이언트/테스트 계약이 흔들립니다.  
재현/수정: `POST /community/posts/{id}/comments` 응답 확인. 댓글도 201 Created 또는 명시적 API 계약으로 통일.