package com.trip.global.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 관광지(TourAPI) 응답을 위한 Redis 캐시 설정.
 * 기존 spring.data.redis 연결(RedisConnectionFactory)을 재사용하며,
 * 캐시별 TTL을 지정한다.
 *
 * <ul>
 *     <li>attractions       — 목록/위치기반 검색 결과, 15분</li>
 *     <li>attractionDetail  — 상세(detailCommon2) 결과, 30분</li>
 *     <li>attractionAreas   — 지역코드(areaCode2) 목록, 24시간</li>
 * </ul>
 *
 * 값 직렬화는 GenericJackson2JsonRedisSerializer(JSON)를 사용하고,
 * 키는 SimpleKey(메서드 인자) 기반 문자열로 직렬화한다.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_ATTRACTIONS = "attractions";
    public static final String CACHE_ATTRACTION_DETAIL = "attractionDetail";
    public static final String CACHE_ATTRACTION_AREAS = "attractionAreas";

    @Bean
    public RedisCacheManager attractionCacheManager(RedisConnectionFactory redisConnectionFactory) {

        // 공통 기본 설정 — JSON 값 직렬화 + null 캐싱 비활성화
        RedisCacheConfiguration defaultConfig = baseConfig(Duration.ofMinutes(15));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put(CACHE_ATTRACTIONS,       baseConfig(Duration.ofMinutes(15)));
        perCache.put(CACHE_ATTRACTION_DETAIL, baseConfig(Duration.ofMinutes(30)));
        perCache.put(CACHE_ATTRACTION_AREAS,  baseConfig(Duration.ofHours(24)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCache)
                .build();
    }

    private RedisCacheConfiguration baseConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}
