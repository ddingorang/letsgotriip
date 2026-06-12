package com.trip.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {
        "com.trip.user.repository",          // UserRepository
        "com.trip.chat.repository",          // ChatRoomRepository, ChatRoomMembershipRepository
        "com.trip.festival.repository",      // FestivalRepository
        "com.trip.preprocessing.repository", // UserAnalysisDataRepository
        "com.trip.attraction.repository",    // AttractionRepository
        "com.trip.plan.repository",          // PlanRepository
        "com.trip.recommend.repository"      // RecommendationRepository
})
@EnableMongoRepositories(basePackages = {
        "com.trip.chat.repository.mongo"     // ChatMessageRepository
})
public class RepositoryConfig {
}
