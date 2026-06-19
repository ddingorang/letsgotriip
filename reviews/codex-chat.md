채팅 범위만 읽고 리뷰했습니다. 코드 수정은 하지 않았습니다.

1. [high] STOMP 구독 인가 우회
   근거: `BE/src/main/java/com/trip/global/interceptor/StompSessionInterceptor.java:29`는 `CONNECT`만 JWT 검증하고, `SUBSCRIBE` destination 검사는 없습니다. `frontend/src/stores/chat.js:88`은 `/topic/chat.room.${roomId}`를 그대로 구독합니다.
   왜 문제인지: 인증만 된 사용자는 멤버가 아닌 방 ID로 직접 구독해 실시간 메시지를 받을 수 있습니다.
   재현/수정: 임의 계정으로 CONNECT 후 `/topic/chat.room.다른방ID` SUBSCRIBE. `SUBSCRIBE`에서 roomId 파싱 후 활성 멤버십을 검사하세요.

2. [high] STOMP 메시지 발행 인가 우회 및 roomId 스푸핑
   근거: `ChatStompController.java:34`의 destination `roomId`는 사용되지 않고, `ChatStompController.java:39`는 DTO만 서비스로 넘깁니다. `ChatService.java:53`은 payload의 `chatRoomId`로 브로드캐스트합니다.
   왜 문제인지: `/pub/chat.message.1`로 보내면서 body의 `chatRoomId`를 `2`로 바꾸면 2번 방에 저장/전파됩니다. 멤버십 검사도 없습니다.
   재현/수정: SEND destination roomId와 body roomId 불일치 요청. 둘을 일치 검증하고, 발신자가 해당 방의 활성 멤버인지 확인하세요.

3. [high] 차단/퇴장 멤버도 히스토리 조회 가능
   근거: `ChatRoomMembership.java:37`, `ChatRoomMembership.java:40`에 `isBanned`, `leftAt` 상태가 있지만, `ChatHistoryController.java:44`-`46`은 userId 존재 여부만 봅니다.
   왜 문제인지: 멤버십 row가 남아 있으면 차단·퇴장 상태여도 과거 대화를 조회할 수 있습니다.
   재현/수정: `is_banned=true` 또는 `left_at != null` 멤버십으로 GET `/api/chat/rooms/{id}/messages`. `isBanned=false`, `leftAt is null` 조건을 repository exists 쿼리로 검사하세요.

4. [med] STOMP DTO 검증 어노테이션이 실제로 적용되지 않음
   근거: `MessageSendRequestDto.java:13`-`16`에 `@NotNull`, `@NotBlank`, `@Size`가 있지만, `ChatStompController.java:35` 파라미터에는 `@Valid`/`@Payload` 검증 트리거가 없습니다.
   왜 문제인지: null `chatRoomId`, null `messageType`, 빈/초대형 `content`가 저장·브로드캐스트될 수 있습니다.
   재현/수정: `{ "chatRoomId": null, "content": "" }` SEND. STOMP handler에 검증을 적용하고 messaging validation 예외를 처리하세요.

5. [med] 히스토리 로드와 구독 사이 메시지 유실 레이스
   근거: `ChatRoomView.vue:133`에서 히스토리를 먼저 로드하고, `ChatRoomView.vue:134`에서 그 뒤에 STOMP 연결/구독합니다.
   왜 문제인지: REST 응답 후 SUBSCRIBE 완료 전 도착한 메시지는 화면에 추가되지 않습니다.
   재현/수정: 방 진입 중 다른 사용자가 메시지 발송. 구독 완료 후 cursor 기반 히스토리를 가져오거나, 구독-히스토리 순서와 중복 제거를 설계하세요.

6. [med] Mongo 저장과 RabbitMQ 브로드캐스트가 원자적이지 않음
   근거: `ChatService.java:47`에서 Mongo 저장 후 `ChatService.java:54`에서 브로드캐스트합니다. 실패 보상/재시도/outbox가 없습니다.
   왜 문제인지: 저장 성공 후 릴레이 장애가 나면 히스토리에는 있지만 실시간 수신자는 못 받습니다. FE도 `chat.js:129`에서 즉시 성공 처리합니다.
   재현/수정: RabbitMQ relay 장애 중 SEND. outbox/retry 또는 sender error/receipt 처리를 추가하세요.

7. [med] 히스토리 전체 조회 + Mongo 인덱스 부재
   근거: `ChatMessageRepository.java:11`은 전체 `List`를 반환하고, `ChatService.java:63`이 제한 없이 조회합니다. `ChatMessage.java:18`, `ChatMessage.java:22`에는 room/timestamp 인덱스가 없습니다.
   왜 문제인지: 큰 방은 collection scan/sort와 대량 응답으로 느려지거나 메모리 압박을 유발합니다.
   재현/수정: 한 방에 대량 메시지 적재 후 히스토리 호출. compound index `(chatRoomId, timestamp)`와 cursor/page limit을 넣으세요.

8. [med] 히스토리 닉네임 조회가 JPA fan-out
   근거: `ChatService.java:76`-`77`에서 메시지 sender별로 `userRepository.findById`를 호출합니다.
   왜 문제인지: 메시지 발신자가 많을수록 Mongo 조회 뒤 JPA 쿼리가 sender 수만큼 추가됩니다.
   재현/수정: 여러 사용자가 섞인 대화 히스토리 조회. senderId를 모아 `findAllById`로 일괄 조회하세요.

9. [low] BE 메시지 타입과 FE 분기 불일치/데드코드
   근거: BE enum은 `TEXT, IMAGE, RECEIPT`뿐입니다(`MessageType.java:4`). FE는 `chat.js:37`에서 이 값을 `type`에 넣지만, `ChatRoomView.vue:38`은 `msg.type === 'plan'`만 특수 처리합니다.
   왜 문제인지: plan 분기는 서버 메시지로 도달 불가하고, IMAGE/RECEIPT는 일반 텍스트 버블로 렌더링됩니다.
   재현/수정: `messageType: IMAGE` 수신. 타입 계약을 맞추고 타입별 렌더링을 구현하세요.

10. [low] 날짜 구분선 하드코딩
   근거: `ChatRoomView.vue:25`에 `6월 10일 화요일`이 고정 문자열입니다.
   왜 문제인지: 실제 메시지 날짜와 무관하게 항상 같은 날짜가 표시됩니다.
   재현/수정: 다른 날짜의 히스토리 조회. 메시지 timestamp 기준으로 날짜 separator를 생성하세요.