리뷰만 수행했고 코드는 수정하지 않았습니다.

[high] STOMP 채팅방 IDOR / 무단 송수신 가능  
`BE/src/main/java/com/trip/global/interceptor/StompSessionInterceptor.java:29-40`은 CONNECT 토큰만 검증하고 SEND/SUBSCRIBE 목적지 권한은 보지 않습니다. `ChatStompController.java:32-39`도 `roomId` 경로변수를 무시하고, `ChatService.java:42-54`는 클라이언트가 보낸 `chatRoomId`로 저장·브로드캐스트합니다.  
재현/수정: 승인되지 않은 사용자가 토큰만 들고 `/pub/chat.message.1` 또는 body `chatRoomId=1`로 송신 가능. SEND/SUBSCRIBE마다 room membership 검증, path `roomId`와 DTO `chatRoomId` 일치 검증 필요.

[high] 승인된 신청을 “취소”하면 신청만 삭제되고 채팅방 멤버십은 남음  
승인 시 멤버십을 추가합니다(`CompanionService.java:206-213`). 하지만 취소는 application만 삭제합니다(`CompanionService.java:230-240`). 상세 응답은 PENDING/APPROVED를 둘 다 `isApplied=true`로만 내려줍니다(`CompanionService.java:298-305`), FE는 상태 구분 없이 취소 버튼을 보여줍니다(`CompanionDetailView.vue:131-135`).  
재현/수정: 승인된 사용자가 신청 취소 → `companion_applications`는 삭제, `chat_room_membership`은 유지 → `getMyRooms`는 멤버십 기준이라 계속 방 노출(`CompanionService.java:266-277`). 승인 취소를 금지하거나 멤버십/participationCount까지 원자적으로 정리하고, 상세 DTO에 application status를 내려야 합니다.

[high] 승인 정원 체크 레이스로 정원 초과 가능  
`approveApplication`은 현재 인원 조회 후(`CompanionService.java:189-193`) 별도 save로 멤버십을 추가합니다(`CompanionService.java:206-213`). `ChatRoomMembership`에는 user-room unique 제약도 없습니다(`ChatRoomMembership.java:24-28`).  
재현/수정: 정원 2명 방에서 신청 2건을 동시에 승인하면 둘 다 current=1을 보고 통과 가능. post/chatRoom pessimistic lock, DB unique/constraint, 원자적 count update가 필요합니다.

[high] `/companion/posts/my`가 공개 GET 매처에 걸려 미인증 500  
Security가 `GET /companion/posts/*`를 permitAll로 열어 둡니다(`SecurityConfig.java:62-63`). 이 패턴은 `/companion/posts/my`도 포함하지만 컨트롤러는 principal null 체크 없이 `principal.userId()`를 호출합니다(`CompanionController.java:25-30`).  
재현/수정: 비로그인 `GET /companion/posts/my` → 401이 아니라 NPE/500. `/my`를 먼저 authenticated로 매칭하거나 공개 상세 조회 패턴을 숫자 id로 제한해야 합니다.

[high] companion 요청 DTO 검증 부재로 400 대신 500/오염 데이터 가능  
컨트롤러는 `@Valid` 없이 body를 받습니다(`CompanionController.java:37,64,93`). 생성 DTO에도 `@NotBlank/@NotNull/@Min/@Size`가 없습니다(`CompanionPostCreateRequest.java:7-16`). 서비스는 곧바로 `request.title().length()`를 호출합니다(`CompanionService.java:48`). 엔티티는 DB nullable/length에만 의존합니다(`CompanionPost.java:31-50`).  
재현/수정: `{ "travelDate": null, "title": null }`, 긴 title/message, 음수 maxMembers/cost 등으로 500 또는 비정상 데이터. DTO Bean Validation과 controller `@Valid`, business range 검증 필요.

[med] 중복 신청 레이스 가능  
중복 방지는 `existsBy...StatusNot(REJECTED)` 후 save입니다(`CompanionService.java:147-159`). `CompanionApplication` 테이블에는 unique 제약이 없습니다(`CompanionApplication.java:13,25-29`).  
재현/수정: 같은 사용자가 동시에 POST 두 번 → PENDING 신청 중복 가능. `(post_id,user_id,active_status)` 제약 또는 락 필요.

[med] 공개 목록 `size` 파라미터 검증 없음 + `size=0` 500 가능  
`size`는 제한 없이 받습니다(`CompanionController.java:44-48`). `getPosts`는 `PageRequest.of(0, size + 1)` 후 `content.get(content.size()-1)`를 호출합니다(`CompanionService.java:87-102`).  
재현/수정: 게시글이 있는 상태에서 `/companion/posts?size=0` → empty content에서 인덱스 오류. `@Min(1) @Max(...)` 또는 clamp 필요.

[med] 목록/상세/신청자 조회 N+1  
목록은 post마다 `countCurrentMembers`가 `findByChatRoomId(...).size()`를 호출합니다(`CompanionService.java:97-99,281-283`). 응답 DTO는 lazy author 접근을 합니다(`CompanionPostSummaryResponse.java:31-32`). 신청자 목록도 `findAllByCompanionPost` 후 applicant lazy 필드를 접근합니다(`CompanionService.java:176-178`, `CompanionApplicationResponse.java:24-31`).  
수정: fetch join/entity graph, batch count query, `countByChatRoomId` 사용.

[med] 배포 compose에 기본 DB/RabbitMQ 자격증명 고정  
`docker-compose.yml:18,48-49,83-90`과 `BE/trip-docker/docker-compose.yml:7,35-36`에 `password`, `guest/guest`가 그대로 있습니다. companion 채팅 연동의 RabbitMQ까지 기본 계정으로 뜹니다.  
수정: compose도 env/secret 주입으로 바꾸고 기본값 제거.

[low] FE 승인/거절 실패를 성공처럼 로컬 상태 변경  
`approveApplicant`/`rejectApplicant` catch에서 실패했는데도 상태를 `approved/rejected`로 바꿉니다(`frontend/src/stores/companion.js:216-219,231-234`).  
재현/수정: 403/409/네트워크 실패 시 UI가 성공처럼 보일 수 있음. 실패 시 로컬 상태 변경 금지, 에러 rethrow.

[low] 깨진 FE 경로와 인증 가드 누락  
상세의 수정 버튼은 `/companion/${id}/edit`로 이동하지만(`CompanionDetailView.vue:15`) 라우터에는 edit route가 없습니다(`router/index.js:31-33`). 신청자 관리 route도 `requiresAuth`가 없습니다(`router/index.js:32`).  
수정: edit route 구현/버튼 제거, applicants route에 `requiresAuth: true` 추가.