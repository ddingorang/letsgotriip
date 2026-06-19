// Created: 2026-06-19
package com.trip.chat.controller;

import com.trip.chat.dto.ChatRoomHostTransferRequest;
import com.trip.chat.dto.ChatRoomInviteRequest;
import com.trip.chat.dto.ChatRoomMuteRequest;
import com.trip.chat.dto.ChatRoomUpdateRequest;
import com.trip.chat.service.ChatService;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 채팅방 설정 + 참여자 관리 REST 엔드포인트.
 * ChatHistoryController(/api/chat/rooms)와 동일 base path 를 공유하되,
 * 방 정보 수정·음소거·강퇴·초대·방장 위임 등 "관리" 동작을 담당한다.
 *
 * 모든 엔드포인트는 인증 필요(/api/chat/** 매처). 권한(방장 등)은 서비스 계층에서 검증한다.
 * 실시간 반영이 필요한 변경은 서비스가 /topic/chat.room.{roomId} 로 비영속 SYSTEM 이벤트를 발행한다.
 */
@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "chat.enabled", havingValue = "true", matchIfMissing = true)
public class ChatRoomSettingsController {

    private final ChatService chatService;

    /** 방 정보(제목/소개) 수정 — 방장만. */
    @PatchMapping("/{chatRoomId}")
    public ResponseEntity<Void> updateRoom(
            @PathVariable Long chatRoomId,
            @Valid @RequestBody ChatRoomUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        chatService.updateRoom(chatRoomId, principal.userId(), request.title(), request.description());
        return ResponseEntity.noContent().build();
    }

    /** 본인 멤버십 음소거 토글. */
    @PatchMapping("/{chatRoomId}/membership/mute")
    public ResponseEntity<Void> updateMute(
            @PathVariable Long chatRoomId,
            @Valid @RequestBody ChatRoomMuteRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        chatService.updateMute(chatRoomId, principal.userId(), request.muted());
        return ResponseEntity.noContent().build();
    }

    /** 참여자 강퇴 — 방장만. */
    @DeleteMapping("/{chatRoomId}/participants/{userId}")
    public ResponseEntity<Void> kickParticipant(
            @PathVariable Long chatRoomId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        chatService.kickParticipant(chatRoomId, principal.userId(), userId);
        return ResponseEntity.noContent().build();
    }

    /** 참여자 초대/추가 — 방장만. nickname 또는 email 로 식별. */
    @PostMapping("/{chatRoomId}/participants")
    public ResponseEntity<Void> inviteParticipant(
            @PathVariable Long chatRoomId,
            @RequestBody ChatRoomInviteRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (request == null || request.isEmpty()) {
            throw new GeneralException(ResponseCode._BAD_REQUEST, "닉네임 또는 이메일이 필요합니다.");
        }
        chatService.inviteParticipant(chatRoomId, principal.userId(), request.nickname(), request.email());
        return ResponseEntity.noContent().build();
    }

    /** 방장 위임 — 현 방장만. */
    @PatchMapping("/{chatRoomId}/host")
    public ResponseEntity<Void> transferHost(
            @PathVariable Long chatRoomId,
            @Valid @RequestBody ChatRoomHostTransferRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        chatService.transferHost(chatRoomId, principal.userId(), request.newHostUserId());
        return ResponseEntity.noContent().build();
    }
}
