// Created: 2026-06-15 23:23:49
package com.trip.community.entity;

import com.trip.community.entity.enums.PostCategory;
import com.trip.user.entity.BaseEntity;
import com.trip.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "posts")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private int viewCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int likeCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostCategory category;

    public void update(String title, String content, PostCategory category) {
        if (title != null && !title.isBlank()) this.title = title;
        if (content != null && !content.isBlank()) this.content = content;
        if (category != null) this.category = category;
    }

    public void delete() {
        this.deleted = true;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    /** 데모/시드용 — 좋아요 수를 임의 값으로 직접 설정한다. */
    public void applyDemoLikeCount(int value) {
        this.likeCount = Math.max(0, value);
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) this.likeCount--;
    }
}
