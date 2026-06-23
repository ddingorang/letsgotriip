package com.trip.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 핫플 좋아요(하트) — 찜(Favorite)과 별개의 공개 인기 카운터(per-user).
 * 토글 시 이 행을 생성/삭제하고 HotPlace.likeCount 를 증감한다.
 */
@Entity
@Table(name = "hot_place_like",
        uniqueConstraints = @UniqueConstraint(name = "uk_hpl_user_place", columnNames = {"user_id", "hot_place_id"}))
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HotPlaceLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "hot_place_id", nullable = false)
    private Long hotPlaceId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
