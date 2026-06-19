package com.trip.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 채팅방 정보 수정 요청(방장 전용).
 * title 길이 18 / description 200 제약은 ChatRoom 컬럼 정의와 일치시킨다.
 */
public record ChatRoomUpdateRequest(
        @NotBlank @Size(max = 18) String title,
        @Size(max = 200) String description // 선택값(null 허용)
) {
}
