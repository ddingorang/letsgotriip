package com.trip.attraction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;

/**
 * attractions 테이블 엔티티 — TourAPI 스냅샷 저장소
 * (contentId, contentType) 복합 유니크 제약
 */
// plan 상세 조회 시 TripPlace → Attraction LAZY 프록시 배치 초기화로 N+1 방지
@BatchSize(size = 50)
@Entity
@Table(
        name = "attractions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_attr_content", columnNames = {"content_id", "content_type"})
        },
        indexes = {
                @Index(name = "idx_attractions_area", columnList = "area_code, content_type")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_id", length = 20, nullable = false)
    private String contentId;

    @Column(name = "content_type", nullable = false)
    private Integer contentType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 300)
    private String addr;

    @Column(name = "area_code", length = 10)
    private String areaCode;

    @Column(name = "sigungu_code", length = 10)
    private String sigunguCode;

    private Double latitude;
    private Double longitude;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(length = 50)
    private String tel;

    @Column(columnDefinition = "TEXT")
    private String overview;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    /** 좋아요(하트) 수 — 정렬용 비정규화 카운터(찜과 별개의 공개 인기 지표) */
    @Column(name = "like_count", nullable = false)
    private int likeCount;

    /**
     * 큐레이션 태그 — 구분자 포함 형태 ",food,night," (앞뒤 콤마)로 저장해
     * LIKE '%,food,%' 가 정확히 매칭되도록 한다. food|culture|activity|night 키 사용.
     */
    @Column(length = 100)
    private String tags;

    @Builder
    public Attraction(String contentId, Integer contentType, String title, String addr,
                      String areaCode, String sigunguCode, Double latitude, Double longitude,
                      String imageUrl, String tel, String overview, LocalDateTime fetchedAt,
                      int likeCount, String tags) {
        this.contentId   = contentId;
        this.contentType = contentType;
        this.title       = title;
        this.addr        = addr;
        this.areaCode    = areaCode;
        this.sigunguCode = sigunguCode;
        this.latitude    = latitude;
        this.longitude   = longitude;
        this.imageUrl    = imageUrl;
        this.tel         = tel;
        this.overview    = overview;
        this.fetchedAt   = fetchedAt;
        this.likeCount   = likeCount;
        this.tags        = tags;
    }

    /** upsert 시 기존 레코드 필드 갱신 */
    public void update(String title, String addr, String areaCode, String sigunguCode,
                       Double latitude, Double longitude, String imageUrl,
                       String tel, String overview) {
        this.title       = title;
        this.addr        = addr;
        this.areaCode    = areaCode;
        this.sigunguCode = sigunguCode;
        this.latitude    = latitude;
        this.longitude   = longitude;
        this.imageUrl    = imageUrl;
        this.tel         = tel;
        this.overview    = overview;
        this.fetchedAt   = LocalDateTime.now();
    }

    // ─────────────────────────────────────────────────────────────
    // 좋아요(하트) / 태그 — 큐레이션·인기 정렬용 헬퍼
    // ─────────────────────────────────────────────────────────────

    /** 좋아요 +1 */
    public void incLike() {
        this.likeCount++;
    }

    /** 좋아요 -1 (0 미만으로 내려가지 않도록 clamp) */
    public void decLike() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    /** 데모용 가짜 좋아요 수 부여 — 기존 값이 0일 때만 적용(실제 좋아요 보존) */
    public void setLikeCountIfZero(int value) {
        if (this.likeCount == 0) {
            this.likeCount = Math.max(0, value);
        }
    }

    /** 데모 좋아요 수 강제 재부여(덮어쓰기) — reroll 전용. */
    public void applyDemoLikeCount(int value) {
        this.likeCount = Math.max(0, value);
    }

    /**
     * 태그를 멱등하게 추가한다. 내부 저장은 ",food,night," 형태(앞뒤·구분 콤마).
     * 이미 포함된 키는 무시한다.
     */
    public void addTag(String key) {
        if (key == null || key.isBlank()) return;
        String k = key.trim();
        if (this.tags == null || this.tags.isBlank()) {
            this.tags = "," + k + ",";
            return;
        }
        if (!this.tags.contains("," + k + ",")) {
            this.tags = this.tags + k + ",";
        }
    }
}
