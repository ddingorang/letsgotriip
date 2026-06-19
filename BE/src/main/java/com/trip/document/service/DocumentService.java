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
        // RAG 색인 대상은 PDF/텍스트뿐이다. 그 외(이미지/스크립트/임의 바이너리)는
        // 공개 정적 루트(/uploads/**)에 저장될 면을 만들지 않도록 업로드 자체를 거부한다.
        if (type != DocumentType.PDF && type != DocumentType.TEXT) {
            throw new GeneralException(ResponseCode._BAD_REQUEST,
                    "지원하지 않는 파일 형식입니다. (PDF 또는 텍스트 파일만 업로드할 수 있습니다.)");
        }
        String storedPath = store(file, type);

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
            // 추출 텍스트 없음(빈 PDF/빈 텍스트 등) → 색인 없이 INGESTED 처리
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

    /**
     * 실제 파일 내용(매직바이트/디코딩 가능성)으로 타입을 판별한다.
     * 클라이언트가 보낸 Content-Type/원본 확장자는 신뢰하지 않는다.
     * PDF(%PDF) 또는 UTF-8로 디코딩되는 텍스트만 인정하고, 그 외는 OTHER로 본다.
     */
    private DocumentType resolveType(MultipartFile file) {
        byte[] head = readHead(file, 1024);
        if (isPdf(head)) {
            return DocumentType.PDF;
        }
        if (isText(head)) {
            return DocumentType.TEXT;
        }
        return DocumentType.OTHER;
    }

    private byte[] readHead(MultipartFile file, int len) {
        try (var in = file.getInputStream()) {
            byte[] buf = new byte[len];
            int read = 0;
            int n;
            while (read < len && (n = in.read(buf, read, len - read)) != -1) {
                read += n;
            }
            return read == len ? buf : java.util.Arrays.copyOf(buf, read);
        } catch (IOException e) {
            log.warn("Failed to read document header for type detection", e);
            return new byte[0];
        }
    }

    private boolean isPdf(byte[] head) {
        return head.length >= 5
                && head[0] == 0x25 && head[1] == 0x50 && head[2] == 0x44
                && head[3] == 0x46 && head[4] == 0x2D; // "%PDF-"
    }

    /**
     * 선두 바이트가 NUL/제어문자 없이 UTF-8로 디코딩되면 텍스트로 간주한다.
     * HTML/JS/SVG도 텍스트로 분류되지만, 저장 확장자는 .txt로 강제되고
     * 서빙 시 text/plain으로 내려가므로 스크립트로 실행되지 않는다.
     */
    private boolean isText(byte[] head) {
        if (head.length == 0) {
            return false;
        }
        for (byte b : head) {
            int v = b & 0xFF;
            if (v == 0x00) {
                return false; // NUL → 바이너리
            }
            if (v < 0x09 || (v > 0x0D && v < 0x20)) {
                return false; // 비텍스트 제어문자
            }
        }
        return true;
    }

    private String store(MultipartFile file, DocumentType type) {
        // 저장 확장자는 원본 확장자가 아니라 판별된 실제 타입으로 강제한다.
        String ext = (type == DocumentType.PDF) ? "pdf" : "txt";
        String filename = UUID.randomUUID() + "." + ext;
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
}
