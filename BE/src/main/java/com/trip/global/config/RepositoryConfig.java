package com.trip.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {
        "com.trip.user.repository",          // UserRepository, AlbumRepository, AlbumPhotoRepository
        "com.trip.chat.repository",          // ChatRoomRepository, ChatRoomMembershipRepository
        "com.trip.festival.repository",      // FestivalRepository
        "com.trip.preprocessing.repository", // UserAnalysisDataRepository
        "com.trip.attraction.repository",    // AttractionRepository
        "com.trip.plan.repository",          // PlanRepository
        "com.trip.recommend.repository",     // RecommendationRepository
        "com.trip.community.repository",     // PostRepository, CommentRepository, HotPlaceRepository, ...
        "com.trip.companion.repository",     // CompanionPostRepository, CompanionApplicationRepository
        "com.trip.notice.repository",        // NoticeRepository
        "com.trip.notification.repository",  // NotificationRepository
        "com.trip.document.repository",      // TripDocumentRepository (RAG 문서)
        "com.trip.checklist.repository"      // ChecklistItemRepository
})
@EnableMongoRepositories(basePackages = {
        "com.trip.chat.repository.mongo"     // ChatMessageRepository
})
public class RepositoryConfig {
}
