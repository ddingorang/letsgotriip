package com.trip.document.entity;

import com.trip.document.entity.enums.DocumentStatus;
import com.trip.document.entity.enums.DocumentType;
import com.trip.user.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trip_documents")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(length = 100)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType type;

    @Column(nullable = false, length = 500)
    private String storedPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private int extractedChars = 0;

    public void markIngested(int extractedChars) {
        this.status = DocumentStatus.INGESTED;
        this.extractedChars = extractedChars;
    }

    public void markFailed() {
        this.status = DocumentStatus.FAILED;
    }
}
