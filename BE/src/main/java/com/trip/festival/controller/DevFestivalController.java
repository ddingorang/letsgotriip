// Created: 2026-06-26 03:18:38
package com.trip.festival.controller;

import com.trip.festival.scheduler.FestivalSyncScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 개발·초기 셋업용 축제 동기화 트리거.
 * SEED_API_ENABLED=true + X-Seed-Secret 일치 시에만 동작한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevFestivalController {

    private final FestivalSyncScheduler festivalSyncScheduler;

    @Value("${app.seed.api.enabled:false}")
    private boolean enabled;

    @Value("${app.seed.api.secret:}")
    private String secret;

    @PostMapping("/festivals/sync")
    public ResponseEntity<?> syncFestivals(
            @RequestHeader(name = "X-Seed-Secret", required = false) String providedSecret) {

        if (!enabled) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (secret == null || secret.isBlank() || !secret.equals(providedSecret)) {
            log.warn("[DevFestivalController] 축제 동기화 요청 거부 — X-Seed-Secret 불일치");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "forbidden", "message", "invalid or missing X-Seed-Secret"));
        }

        try {
            log.info("[DevFestivalController] 축제 동기화 배치 수동 실행");
            festivalSyncScheduler.syncNow();
            return ResponseEntity.ok(Map.of("ok", true, "message", "축제 동기화 배치가 시작되었습니다."));
        } catch (Exception e) {
            log.error("[DevFestivalController] 축제 동기화 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ok", false, "error", "sync_failed", "message", String.valueOf(e.getMessage())));
        }
    }
}
