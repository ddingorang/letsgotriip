package com.trip.assistant.dto;

/**
 * 어시스턴트 대화 응답.
 * conversationId는 후속 요청에서 그대로 다시 보내 대화 맥락을 이어가는 데 사용한다.
 */
public record AssistantChatResponse(
        String conversationId,
        String reply
) {
}
