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

    @Builder
    public Attraction(String contentId, Integer contentType, String title, String addr,
                      String areaCode, String sigunguCode, Double latitude, Double longitude,
                      String imageUrl, String tel, String overview, LocalDateTime fetchedAt) {
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
}
