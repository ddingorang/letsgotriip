package com.trip.global.interceptor;

import com.trip.chat.entity.ChatRoomMembership;
import com.trip.chat.repository.ChatRoomMembershipRepository;
import com.trip.global.error.ResponseCode;
import com.trip.global.error.exception.handler.JwtHandler;
import com.trip.global.security.UserPrincipal;
import com.trip.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompSessionInterceptor implements ChannelInterceptor {

    /** 채팅방 실시간 토픽 목적지 접두사 — destination이 이 패턴이면 SUBSCRIBE 인가를 적용한다. */
    private static final String CHAT_TOPIC_PREFIX = "/topic/chat.room.";

    private final JwtUtil jwtUtil;
    private final ChatRoomMembershipRepository chatRoomMembershipRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // CONNECT 프레임일 때만 JWT 검증 (연결당 1번만 파싱)
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String bearerToken = accessor.getFirstNativeHeader("Authorization");
            if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
                throw new JwtHandler(ResponseCode.JWT_MALFORMED_TOKEN);
            }

            String token = bearerToken.substring(7); // "Bearer " 제거

            // REST(JwtAuthenticationFilter)와 동일한 검증 — 서명/만료 + 세션(familyId) 생존 대조.
            // 로그아웃/재사용 탐지로 패밀리가 폐기되면 Redis 세션이 삭제되므로, 만료 전 토큰이라도
            // 여기서 걸러내 CONNECT를 거부한다(폐기된 토큰으로의 WebSocket 연결 차단).
            if (!jwtUtil.validateToken(token) || !jwtUtil.isSessionAlive(token)) {
                log.warn("WebSocket CONNECT 인증 거부 — 유효하지 않거나 폐기된 세션 토큰.");
                throw new JwtHandler(ResponseCode.JWT_INVALID_TOKEN);
            }

            Long userId = jwtUtil.getUserId(token); // JWT 파싱 + 검증 → userId 추출
            accessor.setUser(new UserPrincipal(userId)); // WebSocket 세션에 저장

            log.info("WebSocket 연결 인증 완료. userId: {}", userId);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

            // SUBSCRIBE 인가 — 채팅방 토픽 구독 시 read-side IDOR 차단.
            // /topic/chat.room.{roomId} 패턴만 검증하고, 그 외 topic 구독은 통과시킨다.
            authorizeChatRoomSubscription(accessor);

        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {


        }

        return message;
    }

    /**
     * 채팅방 토픽({@code /topic/chat.room.{roomId}}) SUBSCRIBE 인가.
     * - 채팅 토픽이 아니면(다른 /topic 등) 검증 없이 통과.
     * - roomId 파싱 실패(숫자 아님/패턴 불일치)는 비정상 채팅 토픽 구독이므로 거부.
     * - CONNECT에서 세션에 저장한 userId가 해당 방의 활성 멤버가 아니면 거부.
     * 거부는 CONNECT 인증 실패와 동일하게 예외를 던져 프레임을 차단한다(STOMP ERROR 프레임으로 전달).
     */
    private void authorizeChatRoomSubscription(StompHeaderAccessor accessor) {

        String destination = accessor.getDestination();

        // 채팅방 토픽이 아니면 인가 대상이 아니므로 통과.
        if (destination == null || !destination.startsWith(CHAT_TOPIC_PREFIX)) {
            return;
        }

        // 세션에 저장된 인증 주체(CONNECT 단계에서 설정). 없으면 인증되지 않은 구독 시도 → 거부.
        Principal user = accessor.getUser();
        if (!(user instanceof UserPrincipal principal)) {
            log.warn("STOMP 구독 인가 거부 — 인증되지 않은 세션. destination: {}", destination);
            throw new MessageDeliveryException("채팅방 구독 권한이 없습니다.");
        }
        Long userId = principal.userId();

        // 접두사 이후 문자열을 roomId로 파싱(예: "/topic/chat.room.42" → "42").
        // 추가 세그먼트(점 포함)나 숫자 아님은 정상 채팅 토픽이 아니므로 거부.
        String roomIdPart = destination.substring(CHAT_TOPIC_PREFIX.length());
        Long roomId;
        try {
            roomId = Long.parseLong(roomIdPart);
        } catch (NumberFormatException e) {
            log.warn("STOMP 구독 인가 거부 — roomId 파싱 실패. destination: {}, userId: {}", destination, userId);
            throw new MessageDeliveryException("채팅방 구독 권한이 없습니다.");
        }

        // 송신/히스토리 조회와 동일한 활성 멤버 검증 규칙(미탈퇴·미강퇴).
        boolean isActiveMember = chatRoomMembershipRepository.findByChatRoomId(roomId).stream()
                .filter(m -> m.getUserId().equals(userId))
                .anyMatch(ChatRoomMembership::isActiveMember);
        if (!isActiveMember) {
            log.warn("STOMP 구독 인가 거부 — 비멤버 구독 시도. userId: {}, roomId: {}", userId, roomId);
            throw new MessageDeliveryException("채팅방 구독 권한이 없습니다.");
        }

        log.info("STOMP 구독 인가 통과. userId: {}, roomId: {}", userId, roomId);
    }
}
