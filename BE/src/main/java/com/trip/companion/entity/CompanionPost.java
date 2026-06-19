// Created: 2026-06-15 23:42:02
package com.trip.companion.entity;

import com.trip.chat.entity.ChatRoom;
import com.trip.companion.entity.enums.CompanionStatus;
import com.trip.user.entity.BaseEntity;
import com.trip.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "companion_posts")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanionPost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private LocalDate travelDate;

    @Column(nullable = false, length = 100)
    private String region;

    @Column(nullable = false, length = 50)
    private String duration;

    @Column(nullable = false)
    private int maxMembers;

    @Column(nullable = false)
    private int estimatedCost;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** 태그 — 콤마 구분 문자열로 저장 (예: "#20대,#맛집투어") */
    @Column(length = 255)
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CompanionStatus status = CompanionStatus.OPEN;

    /**
     * 연결된 여행 계획 ID (nullable). 동행 모집을 작성자의 여행 계획·지도와 연동하기 위한 약한 참조.
     * FK 조인 대신 단순 Long 컬럼으로 보유한다(plan 도메인과 느슨한 결합 유지).
     * ddl-auto=update가 컬럼을 추가한다.
     */
    @Column(name = "plan_id")
    private Long planId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false, unique = true)
    private ChatRoom chatRoom;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    public void update(String title, LocalDate travelDate, String region, String duration,
                       Integer maxMembers, Integer estimatedCost, String description) {
        if (title != null && !title.isBlank()) this.title = title;
        if (travelDate != null) this.travelDate = travelDate;
        if (region != null && !region.isBlank()) this.region = region;
        if (duration != null && !duration.isBlank()) this.duration = duration;
        // 정원은 최소 1명 이상만 허용 — 0/음수 같은 비정상 값으로 덮어쓰지 않는다.
        // (현재 참여 인원 대비 축소 차단은 멤버십 카운트를 아는 서비스 계층에서 수행한다.)
        if (maxMembers != null && maxMembers >= 1) this.maxMembers = maxMembers;
        if (estimatedCost != null) this.estimatedCost = estimatedCost;
        if (description != null && !description.isBlank()) this.description = description;
    }

    public void delete() {
        this.deleted = true;
    }

    public void close() {
        this.status = CompanionStatus.CLOSED;
    }

    public void assignChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    /** 연결할 여행 계획 ID 지정. null이면 연결 해제. */
    public void linkPlan(Long planId) {
        this.planId = planId;
    }

    /** 콤마 구분 tags 문자열을 리스트로 반환. 비어 있으면 빈 리스트. */
    public List<String> getTagList() {
        if (tags == null || tags.isBlank()) return List.of();
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /** tags 리스트를 콤마 구분 문자열로 직렬화. null/빈 리스트면 null 저장. */
    public static String joinTags(List<String> tagList) {
        if (tagList == null || tagList.isEmpty()) return null;
        return tagList.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .reduce((a, b) -> a + "," + b)
                .orElse(null);
    }
}
