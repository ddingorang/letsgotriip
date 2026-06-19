package com.trip.story.entity;

import com.trip.user.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 여행 전후 스토리 — 사용자가 여행 계획에 대해 "여행 전 기대/준비"와 "여행 후 회고"를 기록.
 * 생성/수정 시각은 BaseEntity(createdAt/updatedAt)에서 관리.
 */
@Entity
@Table(name = "travel_stories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelStory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 작성자 */
    @Column(nullable = false)
    private Long userId;

    /** 연결된 여행 계획 (선택) */
    private Long planId;

    @Column(nullable = false, length = 200)
    private String title;

    /** 여행 전 기대/준비 메모 */
    @Column(columnDefinition = "TEXT")
    private String beforeNote;

    /** 여행 후 회고 메모 */
    @Column(columnDefinition = "TEXT")
    private String afterNote;

    /** 여행 만족도 1~5 */
    private Integer rating;

    /** 대표 이미지 URL */
    private String coverImageUrl;

    @Builder
    public TravelStory(Long userId, Long planId, String title, String beforeNote,
                       String afterNote, Integer rating, String coverImageUrl) {
        this.userId = userId;
        this.planId = planId;
        this.title = title;
        this.beforeNote = beforeNote;
        this.afterNote = afterNote;
        this.rating = rating;
        this.coverImageUrl = coverImageUrl;
    }

    /**
     * 부분 수정 — null이 아닌 필드만 갱신한다.
     */
    public void update(String title, Long planId, String beforeNote,
                       String afterNote, Integer rating, String coverImageUrl) {
        if (title != null) {
            this.title = title;
        }
        if (planId != null) {
            this.planId = planId;
        }
        if (beforeNote != null) {
            this.beforeNote = beforeNote;
        }
        if (afterNote != null) {
            this.afterNote = afterNote;
        }
        if (rating != null) {
            this.rating = rating;
        }
        if (coverImageUrl != null) {
            this.coverImageUrl = coverImageUrl;
        }
    }
}
