package com.trip.notification.controller;

import com.trip.global.security.UserPrincipal;
import com.trip.notification.dto.NotificationResponse;
import com.trip.notification.service.NotificationService;
import com.trip.notification.sse.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 내 알림 — 인증 필요(본인 알림만 조회/수정).
 * GET   /api/notifications              목록(최신 50)
 * GET   /api/notifications/stream        실시간 알림 SSE 구독
 * GET   /api/notifications/unread-count 안 읽은 수
 * PATCH /api/notifications/read-all     모두 읽음
 * PATCH /api/notifications/{id}/read    단건 읽음
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterRegistry sseEmitterRegistry;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getMyNotifications(principal.userId()));
    }

    /** 실시간 알림 SSE 구독 — 신규 알림 적재 시 즉시 푸시 받는다. */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal UserPrincipal principal) {
        return sseEmitterRegistry.add(principal.userId());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of("count", notificationService.unreadCount(principal.userId())));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllRead(principal.userId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long notificationId) {
        notificationService.markRead(notificationId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
