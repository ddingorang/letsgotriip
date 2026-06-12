package com.trip.preprocessing.entity;

import com.trip.preprocessing.entity.enums.AnalysisDataType;
import com.trip.user.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_analysis_data")
public class UserAnalysisData extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisDataType dataType;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String storagePath;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String rawText; // STT 변환 결과 또는 카카오톡 텍스트 원본

    public void updateRawText(String rawText) {
        this.rawText = rawText;
    }
}
