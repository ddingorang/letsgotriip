package com.trip.favorite.entity;

import com.trip.user.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 즐겨찾기/북마크 — 사용자가 관광지/게시글/핫플을 저장.
 * 생성 시각은 BaseEntity(createdAt)에서 관리.
 * (userId, targetType, targetId) 조합은 유일.
 */
@Entity
@Table(
        name = "favorites",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_favorite_user_target",
                columnNames = {"user_id", "target_type", "target_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 대상 종류 — ATTRACTION / POST / HOTPLACE */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private FavoriteTargetType targetType;

    /** 대상 식별자 — attraction contentId(문자열), post/hotplace Long을 문자열로 저장 */
    @Column(name = "target_id", nullable = false, length = 100)
    private String targetId;

    /** 대상 이름 — 찜 등록 시점의 이름을 저장해 목록에서 바로 표시 */
    @Column(name = "target_name", length = 255)
    private String targetName;

    @Builder
    public Favorite(Long userId, FavoriteTargetType targetType, String targetId, String targetName) {
        this.userId = userId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetName = targetName;
    }
}
