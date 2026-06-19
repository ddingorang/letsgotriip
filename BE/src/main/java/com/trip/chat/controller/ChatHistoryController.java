// Created: 2026-06-19
package com.trip.chat.controller;

import com.trip.chat.dto.ChatRoomParticipantsResponse;
import com.trip.chat.dto.MessageResponseDto;
import com.trip.chat.entity.ChatRoomMembership;
import com.trip.chat.repository.ChatRoomMembershipRepository;
import com.trip.chat.service.ChatService;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
        // 방의 활성 멤버(나가지 않았고 강퇴되지 않음)만 히스토리 조회 가능
        boolean isActiveMember = chatRoomMembershipRepository
                .findByChatRoomId(chatRoomId).stream()
                .filter(m -> m.getUserId().equals(principal.userId()))
                .anyMatch(ChatRoomMembership::isActiveMember);
        if (!isActiveMember) {
            throw new GeneralException(ResponseCode._FORBIDDEN);
        }

        return ResponseEntity.ok(chatService.getHistory(chatRoomId));
    }

    /**
     * 채팅방 참여자(활성 멤버) 목록/인원수.
     * 헤더 ⋮ 메뉴의 "참여자" 항목에서 사용.
     */
    @GetMapping("/{chatRoomId}/participants")
    public ResponseEntity<ChatRoomParticipantsResponse> getParticipants(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(chatService.getParticipants(chatRoomId, principal.userId()));
    }

    /**
     * 채팅방 나가기 — 본인 멤버십을 소프트 탈퇴(leftAt 설정) 처리.
     * 방장은 나갈 수 없음(400).
     */
    @DeleteMapping("/{chatRoomId}/membership")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable Long chatRoomId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        chatService.leaveRoom(chatRoomId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
