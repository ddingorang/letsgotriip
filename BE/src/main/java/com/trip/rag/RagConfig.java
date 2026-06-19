package com.trip.rag;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import redis.clients.jedis.JedisPooled;

/**
 * RAG 벡터 저장소 설정.
 *
 * <p>Spring AI의 Redis 벡터스토어 자동구성(RedisVectorStoreAutoConfiguration)은
 * spring-data-redis의 JedisConnectionFactory(기본 localhost:6379)에 묶여 있어
 * 일반 캐시용 Redis와 벡터 전용 Redis(Redis Stack/RediSearch, 기본 :6380)를 분리할 수 없다.
 * 따라서 전용 JedisPooled로 명시적 VectorStore 빈을 정의한다.
 * 명시 빈이 존재하므로 자동구성의 {@code @ConditionalOnMissingBean} vectorStore는 비활성화된다.</p>
 */
@Configuration
public class RagConfig {

    private final String redisHost;
    private final int redisPort;
    private final String indexName;
    private final String prefix;

    public RagConfig(
            @Value("${app.rag.redis.host}") String redisHost,
            @Value("${app.rag.redis.port}") int redisPort,
            @Value("${app.rag.index-name}") String indexName,
            @Value("${app.rag.prefix}") String prefix) {
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.indexName = indexName;
        this.prefix = prefix;
    }

    /** 벡터 전용 Redis(RediSearch) 연결 풀 */
    @Bean
    public JedisPooled ragJedisPooled() {
        return new JedisPooled(redisHost, redisPort);
    }

    /**
     * RedisVectorStore — userId/docId/source를 tag 메타데이터 필드로 인덱싱한다.
     * tag 필드여야 QuestionAnswerAdvisor의 {@code userId == '...'} 필터 표현식이 동작한다.
     */
    @Bean
    public VectorStore vectorStore(JedisPooled ragJedisPooled, EmbeddingModel embeddingModel) {
        return RedisVectorStore.builder(ragJedisPooled, embeddingModel)
                .indexName(indexName)
                .prefix(prefix)
                .initializeSchema(true)
                .metadataFields(
                        RedisVectorStore.MetadataField.tag("userId"),
                        RedisVectorStore.MetadataField.tag("docId"),
                        RedisVectorStore.MetadataField.tag("source"))
                .build();
    }
}
