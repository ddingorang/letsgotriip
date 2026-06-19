범위 내 소스 기준 리뷰입니다. 코드 수정은 하지 않았습니다.

1. [high] STOMP 구독 IDOR
   근거: `BE/src/main/java/com/trip/global/interceptor/StompSessionInterceptor.java:29-40`, `BE/src/main/java/com/trip/global/config/WebSocketBrokerConfig.java:82`, `frontend/src/stores/chat.js:88`
   왜 문제인지: CONNECT 토큰만 검증하고 `/topic/chat.room.{id}` SUBSCRIBE 대상 방의 활성 멤버십을 검사하지 않습니다. 토큰만 있으면 다른 방 메시지를 구독할 수 있습니다.
   수정 방향: SUBSCRIBE 프레임에서 destination roomId를 파싱해 `leftAt == null && isBanned == false` 멤버만 허용하세요.

2. [high] STOMP 송신 IDOR + roomId 스푸핑
   근거: `BE/src/main/java/com/trip/chat/controller/ChatStompController.java:34-39`, `BE/src/main/java/com/trip/chat/service/ChatService.java:41-63`, `frontend/src/stores/chat.js:125-130`
   왜 문제인지: destination의 `{roomId}`는 무시되고, payload의 `chatRoomId`로 저장·브로드캐스트합니다. 멤버십 검사도 없어 임의 방에 메시지를 보낼 수 있습니다.
   수정 방향: path roomId와 DTO chatRoomId 일치 검증, 서버 측 active membership 검증, DTO roomId 제거 또는 path 단일화.

3. [high] STOMP 메시지 DTO 검증이 적용되지 않음
   근거: `BE/src/main/java/com/trip/chat/dto/MessageSendRequestDto.java:13-16`, `BE/src/main/java/com/trip/chat/controller/ChatStompController.java:35`, `BE/src/main/java/com/trip/chat/service/ChatService.java:43-55`
   왜 문제인지: DTO에는 `@NotNull`, `@NotBlank`, `@Size`가 있지만 컨트롤러 파라미터에 `@Valid`가 없습니다. 빈 content, null type, 과대 content가 Mongo 저장/브로드캐스트까지 갈 수 있습니다.
   수정 방향: `@Payload @Valid` 적용, 서비스 레벨에서도 type별 content 검증과 실패 ERROR 프레임 처리.

4. [high] 나가기 후에도 내 채팅방 목록에 남음
   근거: `BE/src/main/java/com/trip/chat/service/ChatService.java:112-114`, `BE/src/main/java/com/trip/companion/service/CompanionService.java:288-300`, `frontend/src/stores/companion.js:74-89`, `frontend/src/views/ChatRoomView.vue:242-246`
   왜 문제인지: 나가기는 `leftAt`만 세팅하지만 내 방 목록은 `findByUserId` 전체 멤버십을 그대로 사용합니다. 탈퇴한 방이 다시 노출됩니다.
   수정 방향: 내 방 조회에서 `isActiveMember()`만 포함하고, FE는 leave 성공 시 해당 roomId를 즉시 제거하세요.

5. [med] 나간 멤버도 정원 카운트에 계속 포함됨
   근거: `BE/src/main/java/com/trip/chat/service/ChatService.java:112-114`, `BE/src/main/java/com/trip/companion/service/CompanionService.java:201-203`, `BE/src/main/java/com/trip/chat/repository/ChatRoomMembershipRepository.java:16-17`
   왜 문제인지: 승인 시 `countByChatRoomId`가 left/banned 멤버까지 세므로, 누군가 나가도 방이 계속 full로 계산될 수 있습니다.
   수정 방향: active membership count 쿼리로 바꾸고 필요하면 `participationCount`도 같은 트랜잭션에서 정합화하세요.

6. [med] 히스토리 로드와 STOMP 구독 사이 메시지 유실 레이스
   근거: `frontend/src/views/ChatRoomView.vue:309-310`, `frontend/src/stores/chat.js:88-91`
   왜 문제인지: REST 히스토리를 먼저 받고 그 뒤 구독합니다. 그 사이에 온 메시지는 Mongo 히스토리 응답에도 없고 STOMP 구독도 전이라 화면에서 빠질 수 있습니다.
   수정 방향: 구독 성공 후 cursor/timestamp 기준 히스토리 catch-up을 하거나, 구독 receipt 이후 최신 메시지를 재조회하세요.

7. [med] 히스토리 전체 조회 + 인덱스 없음
   근거: `BE/src/main/java/com/trip/chat/repository/mongo/ChatMessageRepository.java:11`, `BE/src/main/java/com/trip/chat/service/ChatService.java:70-78`, `BE/src/main/java/com/trip/chat/entity/ChatMessage.java:11-22`
   왜 문제인지: 방의 모든 메시지를 한 번에 `List`로 가져오고 메모리 정렬합니다. 메시지가 쌓이면 로딩 지연/메모리 문제가 납니다.
   수정 방향: `chatRoomId,timestamp` 복합 인덱스와 cursor/page size 기반 조회로 바꾸세요.

8. [med] 참여자 조회 N+1
   근거: `BE/src/main/java/com/trip/chat/service/ChatService.java:132-140`
   왜 문제인지: active membership마다 `userRepository.findById`를 호출합니다. 방 인원이 늘수록 쿼리가 선형 증가합니다.
   수정 방향: userId 목록으로 한 번에 조회해 map으로 조인하거나 membership에서 User 연관을 명시적으로 fetch 하세요.

9. [med] 채팅 라우터 가드 누락
   근거: `frontend/src/router/index.js:36-37`, `frontend/src/router/index.js:81-89`, `frontend/src/stores/chat.js:57-63`
   왜 문제인지: `/chat`, `/chat/:id`에 `requiresAuth`가 없습니다. 미인증 사용자가 진입하면 API/STOMP 실패가 조용히 삼켜져 빈 화면/오류 없는 실패가 됩니다.
   수정 방향: 두 라우트에 `requiresAuth: true`를 추가하고 403/401 시 로그인 또는 목록으로 명시 이동하세요.

10. [med] 첨부 업로드 검증이 Content-Type 신뢰에 의존
    근거: `frontend/src/views/ChatRoomView.vue:269-277`, `BE/src/main/java/com/trip/community/service/FileStorageService.java:31-40`, `BE/src/main/java/com/trip/community/service/FileStorageService.java:59-76`
    왜 문제인지: 서버는 클라이언트가 보낸 `Content-Type`만 보고, 저장 확장자는 원본 파일명에서 가져옵니다. 조작된 multipart로 타입/확장자 불일치 파일이 공개 `/uploads/**`에 저장될 수 있습니다.
    수정 방향: magic byte 검사, 허용 타입별 서버 결정 확장자 사용, 이미지 재인코딩/메타 제거를 적용하세요.

11. [low] 채팅 목록의 마지막 메시지/시간/읽음 수가 실제 계약에 없음
    근거: `BE/src/main/java/com/trip/companion/dto/MyCompanionRoomResponse.java:9-15`, `frontend/src/stores/companion.js:83-88`, `frontend/src/views/ChatRoomListView.vue:36-41`
    왜 문제인지: BE DTO에는 last message/unread/time이 없는데 FE는 빈 문자열/0으로 표시합니다. 사용자는 모든 방이 읽음·무내용처럼 보입니다.
    수정 방향: 목록 DTO에 lastMessage, lastMessageAt, unreadCount를 추가하거나 UI에서 해당 필드를 제거하세요.

12. [low] 하드코딩/죽은 UI 분기
    근거: `frontend/src/views/ChatRoomView.vue:40-41`, `BE/src/main/java/com/trip/chat/entity/enums/MessageType.java:3-4`, `frontend/src/views/ChatRoomView.vue:62-70`
    왜 문제인지: 날짜 구분선이 항상 `6월 10일 화요일`이고, FE는 `msg.type === 'plan'`을 처리하지만 BE enum은 `TEXT, IMAGE, RECEIPT`뿐이라 plan 카드 분기는 현재 계약상 도달하지 않습니다.
    수정 방향: 메시지 timestamp로 날짜 구분선을 생성하고, plan/receipt 타입 계약을 BE enum·DTO·FE 렌더링에서 하나로 맞추세요.