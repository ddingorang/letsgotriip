package com.trip.global.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 부팅 시 데모 데이터를 자동으로 1회 시드한다 — "서버가 켜지면 데이터가 비어있지 않게".
 *
 * <p>{@code app.seed.boot.enabled=true}(env {@code SEED_BOOT_ENABLED}, 기본 false)일 때만 등록된다.
 * {@link SeedService#seed(String, boolean)} 를 {@code reset=false}(멱등)로 호출하므로,
 * 이미 시드되어 있으면(=@seed.triip 사용자 존재) 아무것도 하지 않고 즉시 skip 한다.
 * → 매 부팅마다 안전하게 호출 가능(첫 부팅만 실제 삽입, 이후엔 no-op).</p>
 *
 * <p>운영에서는 {@code SEED_BOOT_ENABLED} 를 설정하지 않으면(기본 false) 자동 시드가 비활성이다.
 * 시드 실패는 부팅을 막지 않는다(경고만 남기고 계속). 반복 초기화·테스트 픽스처는
 * 여전히 {@code scripts/seed.sh}(reset/test)로 수행한다.</p>
 */
@Slf4j
@Component
@Order(100) // 다른 부팅 시더(DataSeeder 등) 이후에 실행
@ConditionalOnProperty(name = "app.seed.boot.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class SeedBootRunner implements ApplicationRunner {

    private final SeedService seedService;

    /** 부팅 자동 시드 프로파일(demo|test). 기본 demo. */
    @Value("${app.seed.boot.profile:demo}")
    private String profile;

    @Override
    public void run(ApplicationArguments args) {
        try {
            // reset=false → 멱등. 이미 시드되어 있으면 SeedService 내부에서 skip 처리된다.
            Map<String, Integer> counts = seedService.seed(profile, false);
            if (counts.getOrDefault("skipped_already_seeded", 0) == 1) {
                log.info("[SeedBootRunner] 부팅 시드 skip — 이미 데이터 존재(profile={})", profile);
            } else {
                log.info("[SeedBootRunner] 부팅 시드 완료 profile={} counts={}", profile, counts);
            }
        } catch (Exception e) {
            // 시드 실패는 부팅을 막지 않는다.
            log.warn("[SeedBootRunner] 부팅 시드 실패 — 무시하고 계속: {}", e.getMessage(), e);
        }
    }
}
