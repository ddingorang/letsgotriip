package com.trip.assistant.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 어시스턴트 대화 요청.
 * conversationId가 null/blank면 서버가 새 대화로 간주하고 UUID를 발급한다.
 */
public record AssistantChatRequest(
        String conversationId,

        @NotBlank
        String message
) {
}
