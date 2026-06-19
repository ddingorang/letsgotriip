package com.trip.document.dto;

import com.trip.document.entity.TripDocument;
import com.trip.document.entity.enums.DocumentStatus;
import com.trip.document.entity.enums.DocumentType;

import java.time.LocalDateTime;

public record DocumentResponse(
        Long id,
        String filename,
        DocumentType type,
        DocumentStatus status,
        int extractedChars,
        LocalDateTime createdAt
) {
    public static DocumentResponse from(TripDocument document) {
        return new DocumentResponse(
                document.getId(),
                document.getFilename(),
                document.getType(),
                document.getStatus(),
                document.getExtractedChars(),
                document.getCreatedAt()
        );
    }
}
