// Created: 2026-06-19
package com.trip.chat.controller;

import com.trip.chat.dto.MessageResponseDto;
import com.trip.chat.repository.ChatRoomMembershipRepository;
import com.trip.chat.service.ChatService;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 채팅 메시지 히스토리 REST 엔드포인트.
 * STOMP 실시간 채널과 별개로, 새로고침/재입장 시 이전 대화를 복원하기 위해 사용한다.
 *
 * 경로 주의: FE는 /api/chat/... 로 호출하며 dev vite 프록시는 /api prefix를 유지한 채
 * BE로 전달한다(community·companion 과 달리 /chat 전용 rewrite 규칙이 없음).
 * 따라서 이 컨트롤러는 /api/chat/rooms 로 매핑한다.
 */
@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "chat.enabled", havingValue = "true", matchIfMissing = true)
public class ChatHistoryController {

    private final ChatService chatService;
    private final ChatRoomMembershipRepository chatRoomMembershipRepository;

    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<List<MessageResponseDto>> getMessages(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        // 방 멤버만 히스토리 조회 가능
        boolean isMember = chatRoomMembershipRepository
                .findByChatRoomId(chatRoomId).stream()
                .anyMatch(m -> m.getUserId().equals(principal.userId()));
        if (!isMember) {
            throw new GeneralException(ResponseCode._FORBIDDEN);
        }

        return ResponseEntity.ok(chatService.getHistory(chatRoomId));
    }
}
