package com.trip.chat.dto;

/**
 * 참여자 초대/추가 요청(방장 전용).
 * nickname 또는 email 중 하나로 사용자를 식별한다(email 우선).
 */
public record ChatRoomInviteRequest(
        String nickname,
        String email
) {
    public boolean isEmpty() {
        return (nickname == null || nickname.isBlank())
                && (email == null || email.isBlank());
    }
}
