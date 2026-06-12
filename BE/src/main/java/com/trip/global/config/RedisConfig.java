package com.trip.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.trip.user.dto.RedisSessionDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    private final String REDIS_HOST;
    private final Integer REDIS_PORT;

    public RedisConfig(
            @Value("${spring.data.redis.host}") String REDIS_HOST,
            @Value("${spring.data.redis.port}") int REDIS_PORT)
    {

        this.REDIS_HOST = REDIS_HOST;
        this.REDIS_PORT = REDIS_PORT;
    }

    /**
     * RedisConnectionFactory 빈 정의.
     * Redis 서버와의 물리적 연결을 생성 및 관리합니다.
     * LettuceConnectionFactory를 사용하여 Redis와 연결합니다.
     *
     * @return RedisConnectionFactory - Redis와의 연결 팩토리 객체
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {

        return new LettuceConnectionFactory(REDIS_HOST, REDIS_PORT);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory redisConnectionFactory) {

        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }

    // TODO: 바이브코딩으로 작성한거라 수정 필요
    @Bean
    public RedisTemplate<String, RedisSessionDto> redisSessionTemplate(RedisConnectionFactory redisConnectionFactory) {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Jackson2JsonRedisSerializer<RedisSessionDto> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, RedisSessionDto.class);

        RedisTemplate<String, RedisSessionDto> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        return template;
    }
}
