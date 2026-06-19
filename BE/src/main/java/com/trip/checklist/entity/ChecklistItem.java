package com.trip.checklist.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 여행 준비 체크리스트 항목.
 * 사용자별로 관리되며, 특정 여행 계획(planId)·일자(dayNo)에 선택적으로 연결될 수 있다.
 */
@Entity
@Table(name = "checklist_items")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소유자 */
    @Column(nullable = false)
    private Long userId;

    /** 연결된 여행 계획 (선택) */
    @Column
    private Long planId;

    /** 연결된 여행 일차 (선택) */
    @Column
    private Integer dayNo;

    @Column(nullable = false, length = 200)
    private String title;

    /** 분류 라벨 — 준비물 / 서류 / 예약 / 기타 */
    @Column(length = 20)
    private String category;

    /** 완료 여부 */
    @Column(nullable = false)
    private boolean checked;

    /** 정렬 순서 */
    @Column(nullable = false)
    private int sortOrder;

    @Column
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    /** 완료/미완료 토글 */
    public void toggle() {
        this.checked = !this.checked;
    }

    /** 부분 수정 — null은 기존 값 유지 */
    public void update(String title, String category, Boolean checked, Integer sortOrder) {
        if (title != null) {
            this.title = title;
        }
        if (category != null) {
            this.category = category;
        }
        if (checked != null) {
            this.checked = checked;
        }
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
    }
}
