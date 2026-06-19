package com.trip.assistant;

import com.trip.assistant.dto.AssistantChatRequest;
import com.trip.assistant.dto.AssistantChatResponse;
import com.trip.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * /api/assistant — 대화형 여행 어시스턴트.
 * 인증 필수(SecurityConfig 기본 authenticated 매처에 포함).
 */
@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    /** POST /api/assistant/chat — 대화 메시지 전송 */
    @PostMapping("/chat")
    public ResponseEntity<AssistantChatResponse> chat(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AssistantChatRequest request) {

        String conversationId = (request.conversationId() == null || request.conversationId().isBlank())
                ? UUID.randomUUID().toString()
                : request.conversationId();

        String reply = assistantService.chat(principal.userId(), conversationId, request.message());

        return ResponseEntity.ok(new AssistantChatResponse(conversationId, reply));
    }
}
