// Created: 2026-06-15 23:24:48
package com.trip.community.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hot_place_photos")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HotPlacePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hot_place_id", nullable = false)
    private HotPlace hotPlace;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private int displayOrder;
}
