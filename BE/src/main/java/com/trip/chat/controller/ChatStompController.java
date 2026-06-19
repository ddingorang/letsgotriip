package com.trip.chat.controller;

import com.trip.chat.dto.MessageSendRequestDto;
import com.trip.chat.service.ChatService;
import com.trip.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@Slf4j
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "chat.enabled", havingValue = "true", matchIfMissing = true)
public class ChatStompController {

    private final ChatService chatService;

    /**
     * 채팅 메시지를 특정 채팅방(roomId)으로 전송.
     * 클라이언트가 STOMP 프로토콜을 사용해 "chat.message.{roomId}" 경로로 메시지를 전송하면 해당 메서드가 메시지를 처리(서비스 계층으로 위임).
     * 클라이언트가 WebSocket을 보낸 메시지를 처리할 서버 측 핸들러 메서드를 지정
     *
     * @param roomId                채팅방 ID (STOMP 경로 변수)
     * @param messageSendRequestDto 전송된 메시지 데이터
     * @param principal             WebSocket 연결 시 인증된 사용자 정보
     */
    @MessageMapping("chat.message.{roomId}")
    public void publishMessage(
            @DestinationVariable Long roomId,
            MessageSendRequestDto messageSendRequestDto,
            UserPrincipal principal) {

        Long senderId = principal.userId(); // WebSocket 연결 시점에 JWT에서 추출한 사용자 ID
        // STOMP 경로(@DestinationVariable)의 roomId만 권위 있는 목적지로 신뢰한다.
        // 페이로드의 chatRoomId는 스푸핑 가능하므로 서비스 계층에서 무시한다(IDOR 방지).
        chatService.sendMessage(messageSendRequestDto, senderId, roomId);
    }
}
