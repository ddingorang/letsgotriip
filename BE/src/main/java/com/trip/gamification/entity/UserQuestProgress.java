package com.trip.gamification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자별 퀘스트 진행 — (userId, questCode) 유일.
 * 퀘스트 정의 자체는 정적(QuestCatalog)이고, 진행/완료 상태만 여기 영속화한다.
 */
@Entity
@Table(name = "user_quest_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_quest_progress_user_code",
                columnNames = {"userId", "questCode"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserQuestProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 40)
    private String questCode;

    @Column(nullable = false)
    private int progress;

    @Column(nullable = false)
    private boolean completed;

    private LocalDateTime completedAt;

    @Builder
    public UserQuestProgress(Long userId, String questCode) {
        this.userId = userId;
        this.questCode = questCode;
        this.progress = 0;
        this.completed = false;
    }

    /** goal 까지 1 증가시키고, 도달하면 완료 처리. 이미 완료면 변화 없음(멱등). */
    public void advance(int goal) {
        if (completed) {
            return;
        }
        if (progress < goal) {
            progress++;
        }
        if (progress >= goal) {
            completed = true;
            completedAt = LocalDateTime.now();
        }
    }
}
