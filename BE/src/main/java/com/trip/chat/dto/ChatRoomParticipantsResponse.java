package com.trip.chat.dto;

import lombok.Builder;

import java.util.List;

/**
 * 채팅방 참여자 정보 응답.
 * 헤더 ⋮ 메뉴의 "참여자" 항목에서 인원수/목록을 표시하기 위한 경량 DTO.
 */
@Builder
public record ChatRoomParticipantsResponse(
        Long chatRoomId,
        int count, // 활성 참여 인원수
        List<Participant> participants
) {
    @Builder
    public record Participant(
            Long userId,
            String nickname,
            boolean isHost
    ) {
    }
}
