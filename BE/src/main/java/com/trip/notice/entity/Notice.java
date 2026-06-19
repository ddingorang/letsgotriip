package com.trip.notice.entity;

import com.trip.user.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공지사항 — 운영자가 등록하는 안내/업데이트/필독 공지.
 * 생성/수정 시각은 BaseEntity(createdAt/updatedAt)에서 관리.
 */
@Entity
@Table(name = "notices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 분류 라벨 — 필독 / 업데이트 / 안내 */
    @Column(nullable = false, length = 20)
    private String category;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    /** 상단 고정 여부 */
    @Column(nullable = false)
    private boolean pinned;

    @Builder
    public Notice(String category, String title, String content, boolean pinned) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.pinned = pinned;
    }

    public void update(String category, String title, String content, boolean pinned) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.pinned = pinned;
    }
}
