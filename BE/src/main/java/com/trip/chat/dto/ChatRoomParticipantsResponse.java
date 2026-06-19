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
        boolean viewerIsHost, // 요청자가 방장인지(FE 관리 버튼 게이팅용)
        boolean viewerMuted, // 요청자 본인의 음소거 초기 상태(FE 토글 초기값 동기화용)
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
