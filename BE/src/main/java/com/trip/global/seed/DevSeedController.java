package com.trip.global.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 개발용 시드 엔드포인트 — scripts/seed.sh 가 호출한다.
 *
 * <p>운영 안전 게이트(컨트롤러 자체 게이팅 + SecurityConfig permitAll):
 * <ul>
 *   <li>{@code app.seed.api.enabled=true}(env {@code SEED_API_ENABLED}, 기본 false) 가 아니면 404</li>
 *   <li>헤더 {@code X-Seed-Secret} 가 {@code app.seed.api.secret}(env {@code SEED_API_SECRET}) 와 불일치하면 403</li>
 * </ul>
 * 운영에서는 SEED_API_ENABLED 를 설정하지 않으면(기본 false) 항상 404 가 된다.
 */
@Slf4j
@RestController
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevSeedController {

    private final SeedService seedService;

    @Value("${app.seed.api.enabled:false}")
    private boolean enabled;

    @Value("${app.seed.api.secret:}")
    private String secret;

    @PostMapping("/seed")
    public ResponseEntity<?> seed(
            @RequestParam(defaultValue = "demo") String profile,
            @RequestParam(defaultValue = "false") boolean reset,
            @RequestHeader(name = "X-Seed-Secret", required = false) String providedSecret) {

        // 1) 비활성 → 존재하지 않는 것처럼 404 (운영 기본값)
        if (!enabled) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // 2) 시크릿 미설정/불일치 → 403
        if (secret == null || secret.isBlank() || !secret.equals(providedSecret)) {
            log.warn("[DevSeedController] 시드 요청 거부 — X-Seed-Secret 불일치");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "forbidden", "message", "invalid or missing X-Seed-Secret"));
        }

        // 3) 시드 실행
        try {
            Map<String, Integer> counts = seedService.seed(profile, reset);
            log.info("[DevSeedController] 시드 완료 profile={} reset={} counts={}", profile, reset, counts);
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "profile", profile,
                    "reset", reset,
                    "counts", counts));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "error", "bad_request", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("[DevSeedController] 시드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ok", false, "error", "seed_failed", "message", String.valueOf(e.getMessage())));
        }
    }
}
