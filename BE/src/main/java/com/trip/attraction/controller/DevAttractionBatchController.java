package com.trip.attraction.controller;

import com.trip.attraction.service.AttractionBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * 개발용 큐레이션 배치 엔드포인트 — 시드와 동일한 dev 게이트로 보호.
 *  POST /api/dev/attractions/batch?total=2000
 *
 * 게이트:
 *  - app.seed.api.enabled=false 면 404 (엔드포인트 자체를 숨김)
 *  - X-Seed-Secret 헤더가 app.seed.api.secret 와 불일치면 403
 * SecurityConfig 에서 /api/dev/** 는 permitAll(헤더 시크릿으로 자체 보호).
 */
@Slf4j
@RestController
@RequestMapping("/api/dev/attractions")
@RequiredArgsConstructor
public class DevAttractionBatchController {

    private final AttractionBatchService attractionBatchService;

    @Value("${app.seed.api.enabled:false}")
    private boolean apiEnabled;

    @Value("${app.seed.api.secret:}")
    private String apiSecret;

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Integer>> batch(
            @RequestParam(defaultValue = "2000") int total,
            @RequestHeader(value = "X-Seed-Secret", required = false) String secret) {

        // 1) 비활성화 시 엔드포인트 노출 자체를 숨긴다(404)
        if (!apiEnabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        // 2) 시크릿 불일치 → 403
        if (apiSecret == null || apiSecret.isBlank() || !apiSecret.equals(secret)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid seed secret");
        }

        log.info("[DevAttractionBatch] 배치 시작 total={}", total);
        Map<String, Integer> counts = attractionBatchService.batch(total);
        log.info("[DevAttractionBatch] 배치 종료 — {}", counts);
        return ResponseEntity.ok(counts);
    }

    /**
     * 이미 수집된 관광지의 데모 좋아요 수를 자연스러운 분포로 재부여(덮어쓰기). TourAPI 재호출 없음.
     * POST /api/dev/attractions/relike — 시연 직전 인기수를 자연스럽게 다듬을 때.
     */
    @PostMapping("/relike")
    public ResponseEntity<Map<String, Integer>> relike(
            @RequestHeader(value = "X-Seed-Secret", required = false) String secret) {
        if (!apiEnabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (apiSecret == null || apiSecret.isBlank() || !apiSecret.equals(secret)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid seed secret");
        }
        int n = attractionBatchService.reroll();
        log.info("[DevAttractionBatch] 좋아요 재부여(relike) — {}건", n);
        return ResponseEntity.ok(Map.of("rerolled", n));
    }
}
