package com.trip.global.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI ChatClient 빈 설정.
 * ChatClient.Builder는 spring-ai-starter-model-openai 자동 구성이 제공한다.
 * build()를 통해 싱글턴 ChatClient 빈을 생성한다.
 */
@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
