// Created: 2026-06-08 15:32:32
package com.trip.festival.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "festivals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Festival {

    @Id
    @Column(name = "content_id")
    private String contentId;

    @Column(nullable = false)
    private String title;

    private String address;
    private String tel;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    private Double latitude;
    private Double longitude;

    @Column(name = "area_code")
    private String areaCode;

    @Column(name = "sigungu_code")
    private String sigunguCode;

    @Enumerated(EnumType.STRING)
    private FestivalStatus status;

    @Column(name = "synced_at")
    private LocalDateTime syncedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        this.syncedAt = LocalDateTime.now();
    }

    @Builder
    public Festival(String contentId, String title, String address, String tel,
                    String imageUrl, LocalDate startDate, LocalDate endDate,
                    Double latitude, Double longitude, String areaCode,
                    String sigunguCode, FestivalStatus status, LocalDateTime syncedAt) {
        this.contentId = contentId;
        this.title = title;
        this.address = address;
        this.tel = tel;
        this.imageUrl = imageUrl;
        this.startDate = startDate;
        this.endDate = endDate;
        this.latitude = latitude;
        this.longitude = longitude;
        this.areaCode = areaCode;
        this.sigunguCode = sigunguCode;
        this.status = status;
        this.syncedAt = syncedAt;
    }

    public void update(String title, String address, String tel, String imageUrl,
                       LocalDate startDate, LocalDate endDate,
                       Double latitude, Double longitude,
                       String areaCode, String sigunguCode, FestivalStatus status) {
        this.title = title;
        this.address = address;
        this.tel = tel;
        this.imageUrl = imageUrl;
        this.startDate = startDate;
        this.endDate = endDate;
        this.latitude = latitude;
        this.longitude = longitude;
        this.areaCode = areaCode;
        this.sigunguCode = sigunguCode;
        this.status = status;
        this.syncedAt = LocalDateTime.now();
    }
}
