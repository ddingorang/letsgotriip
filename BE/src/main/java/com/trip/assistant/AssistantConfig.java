package com.trip.assistant;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 어시스턴트 대화기억 설정.
 *
 * <p>conversationId별 최근 메시지 윈도우(20개)를 유지하는 ChatMemory 빈을 명시적으로 정의한다.
 * 기본 저장소는 in-memory(InMemoryChatMemoryRepository)이며, 별도 빈을 지정하지 않으면
 * Spring AI 자동구성이 동일 빈을 {@code @ConditionalOnMissingBean}으로 제공하지만
 * 윈도우 크기를 명시 제어하기 위해 직접 등록한다.</p>
 */
@Configuration
public class AssistantConfig {

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }
}
