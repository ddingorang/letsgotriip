package com.trip.document.service;

import com.trip.document.dto.DocumentResponse;
import com.trip.document.entity.TripDocument;
import com.trip.document.entity.enums.DocumentType;
import com.trip.document.repository.TripDocumentRepository;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.rag.IngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final TripDocumentRepository documentRepository;
    private final IngestionService ingestionService;

    @Value("${app.upload.base-dir:uploads}")
    private String baseDir;

    /**
     * 파일을 로컬에 저장 → TripDocument(PENDING) 저장 → 텍스트 추출 후 RAG 색인.
     * 추출 텍스트가 있으면 IngestionService.ingest 호출 후 INGESTED,
     * 색인 중 예외 발생 시 FAILED로 기록(예외는 삼키되 로그/상태 보존).
     */
    @Transactional
    public DocumentResponse upload(Long userId, MultipartFile file) {
        DocumentType type = resolveType(file);
        String storedPath = store(file);

        TripDocument document = documentRepository.save(TripDocument.builder()
                .userId(userId)
                .filename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .type(type)
                .storedPath(storedPath)
                .build());

        String text = extractText(type, storedPath);

        if (text != null && !text.isBlank()) {
            try {
                ingestionService.ingest(
                        userId,
                        "doc:" + document.getId(),
                        file.getOriginalFilename(),
                        text);
                document.markIngested(text.length());
            } catch (Exception e) {
                log.error("Document ingestion failed. documentId={}, userId={}",
                        document.getId(), userId, e);
                document.markFailed();
            }
        } else {
            // 추출 텍스트 없음(이미지/기타 OCR 미구현) → 색인 없이 INGESTED 처리
            document.markIngested(0);
        }

        return DocumentResponse.from(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(Long userId) {
        return documentRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long documentId, Long userId) {
        TripDocument document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new GeneralException(ResponseCode._FORBIDDEN));

        // RAG 벡터 인덱스에서도 제거 → 삭제 후 챗봇이 더 이상 그 자료를 참고하지 않음
        try {
            ingestionService.deleteByDoc("doc:" + documentId);
        } catch (Exception e) {
            log.warn("Failed to delete vectors for document {}", documentId, e);
        }
        deleteStoredFile(document.getStoredPath());
        documentRepository.delete(document);
    }

    // ─── 내부 구현 ────────────────────────────────────────────

    private DocumentType resolveType(MultipartFile file) {
        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();

        if (isPdf(contentType, filename)) {
            return DocumentType.PDF;
        }
        if (contentType != null && contentType.startsWith("text/")) {
            return DocumentType.TEXT;
        }
        if (contentType != null && contentType.startsWith("image/")) {
            return DocumentType.IMAGE;
        }
        return DocumentType.OTHER;
    }

    private boolean isPdf(String contentType, String filename) {
        if ("application/pdf".equalsIgnoreCase(contentType)) {
            return true;
        }
        return filename != null && filename.toLowerCase().endsWith(".pdf");
    }

    private String store(MultipartFile file) {
        String ext = extractExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        Path targetPath = Paths.get(baseDir, "documents").resolve(filename);

        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to store document file: {}", filename, e);
            throw new GeneralException(ResponseCode.FILE_UPLOAD_FAILED);
        }

        return targetPath.toString();
    }

    private String extractText(DocumentType type, String storedPath) {
        try {
            return switch (type) {
                case PDF -> extractPdfText(storedPath);
                case TEXT -> Files.readString(Paths.get(storedPath), StandardCharsets.UTF_8);
                default -> null; // IMAGE/OTHER: OCR 미구현
            };
        } catch (Exception e) {
            log.error("Failed to extract text from document. path={}", storedPath, e);
            return null;
        }
    }

    private String extractPdfText(String storedPath) {
        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                new FileSystemResource(storedPath));
        List<Document> documents = reader.get();
        StringBuilder sb = new StringBuilder();
        for (Document doc : documents) {
            String text = doc.getText();
            if (text != null) {
                sb.append(text).append("\n");
            }
        }
        return sb.toString();
    }

    private void deleteStoredFile(String storedPath) {
        try {
            Files.deleteIfExists(Paths.get(storedPath).toAbsolutePath());
        } catch (IOException e) {
            log.warn("Failed to delete document file: {}", storedPath, e);
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
    }
}
