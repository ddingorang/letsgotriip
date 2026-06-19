package com.trip.assistant;

import com.trip.assistant.dto.AssistantChatRequest;
import com.trip.assistant.dto.AssistantChatResponse;
import com.trip.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

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

    /**
     * POST /api/assistant/chat/stream — 대화 메시지 전송(SSE 토큰 스트리밍).
     *
     * <p>비스트리밍 {@link #chat}와 동일한 구성을 쓰되 응답 토큰을 SSE로 흘려보낸다.
     * 첫 이벤트는 {@code event: conversationId} 로 대화 식별자를 내려주어 FE가 후속 요청에
     * 같은 conversationId를 재사용할 수 있게 한다. 이후 이벤트는 {@code event: token} 으로
     * 응답 텍스트 조각을 전달하고, 스트림이 끝나면 {@code event: done} 으로 종료를 알린다.</p>
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AssistantChatRequest request) {

        String conversationId = (request.conversationId() == null || request.conversationId().isBlank())
                ? UUID.randomUUID().toString()
                : request.conversationId();

        // 첫 이벤트: conversationId (FE가 후속 요청에 재사용)
        Flux<ServerSentEvent<String>> idEvent = Flux.just(
                ServerSentEvent.<String>builder()
                        .event("conversationId")
                        .data(conversationId)
                        .build());

        // 종료 이벤트: FE가 스트림 완료를 명확히 인지하도록 done 이벤트를 덧붙인다.
        Flux<ServerSentEvent<String>> doneEvent = Flux.just(
                ServerSentEvent.<String>builder()
                        .event("done")
                        .data("")
                        .build());

        Flux<ServerSentEvent<String>> tokenEvents = assistantService
                .chatStream(principal.userId(), conversationId, request.message())
                .map(token -> ServerSentEvent.<String>builder()
                        .event("token")
                        .data(token)
                        .build())
                // 스트림 도중 오류는 token으로 위장하지 않고 event:error로 구분 전송한 뒤 정상 종료한다.
                // (연결을 끊지 않으므로 FE가 오류를 token과 분리해 처리할 수 있고, 뒤따르는 done으로 마무리된다.)
                .onErrorResume(e -> Flux.just(
                        ServerSentEvent.<String>builder()
                                .event("error")
                                .data("응답 생성 중 오류가 발생했어요. 잠시 후 다시 시도해 주세요.")
                                .build()));

        // 성공·오류 모두 tokenEvents가 정상 완료되므로 done은 정확히 한 번 전송된다.
        return Flux.concat(idEvent, tokenEvents, doneEvent);
    }
}
