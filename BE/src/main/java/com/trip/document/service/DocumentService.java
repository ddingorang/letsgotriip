package com.trip.document.service;

import com.trip.document.dto.DocumentResponse;
import com.trip.document.entity.TripDocument;
import com.trip.document.entity.enums.DocumentType;
import com.trip.document.repository.TripDocumentRepository;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.preprocessing.client.STTManager;
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
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final TripDocumentRepository documentRepository;
    private final IngestionService ingestionService;
    private final STTManager sttManager;

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
        // RAG 색인 대상은 PDF/텍스트/오디오(STT 전사)뿐이다. 그 외(이미지/스크립트/임의 바이너리)는
        // 공개 정적 루트(/uploads/**)에 저장될 면을 만들지 않도록 업로드 자체를 거부한다.
        if (type != DocumentType.PDF && type != DocumentType.TEXT && type != DocumentType.AUDIO) {
            throw new GeneralException(ResponseCode._BAD_REQUEST,
                    "지원하지 않는 파일 형식입니다. (PDF·텍스트·음성 파일만 업로드할 수 있습니다.)");
        }
        String storedPath = store(file, type);

        TripDocument document = documentRepository.save(TripDocument.builder()
                .userId(userId)
                .filename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .type(type)
                .storedPath(storedPath)
                .build());

        if (type == DocumentType.AUDIO) {
            return ingestAudio(userId, document, storedPath);
        }

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

    /**
     * 음성 파일을 Whisper STT로 전사 → 전사 텍스트를 RAG 색인한다.
     *
     * <p>전사/색인은 외부 API 호출이라 오래 걸리거나 실패할 수 있다. 호출 자체의 예외를
     * 잡아 FAILED로 기록해 요청/트랜잭션을 안전하게 종료한다(문서 레코드는 보존).
     * 전사 결과가 비어 있으면 가짜 성공으로 저장하지 않고 FAILED로 처리한다.</p>
     */
    private DocumentResponse ingestAudio(Long userId, TripDocument document, String storedPath) {
        String transcript;
        try {
            transcript = sttManager.convertSpeechToText(Paths.get(storedPath).toFile());
        } catch (Exception e) {
            log.error("Audio transcription failed. documentId={}, userId={}",
                    document.getId(), userId, e);
            document.markFailed();
            return DocumentResponse.from(document);
        }

        // 빈 전사는 성공으로 저장하지 않는다(색인할 내용이 없음).
        if (transcript == null || transcript.isBlank()) {
            log.warn("Audio transcription empty. documentId={}, userId={}",
                    document.getId(), userId);
            document.markFailed();
            return DocumentResponse.from(document);
        }

        try {
            ingestionService.ingest(
                    userId,
                    "doc:" + document.getId(),
                    document.getFilename(),
                    transcript);
            document.markIngested(transcript.length());
        } catch (Exception e) {
            log.error("Audio ingestion failed. documentId={}, userId={}",
                    document.getId(), userId, e);
            document.markFailed();
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

    private static final java.util.Set<String> AUDIO_EXTENSIONS =
            java.util.Set.of("m4a", "mp3", "wav", "webm", "ogg", "mp4", "mpeg", "mpga", "flac");

    /**
     * 실제 파일 내용(매직바이트/디코딩 가능성)으로 타입을 판별한다.
     * 클라이언트가 보낸 Content-Type/원본 확장자는 텍스트/PDF 판별에는 신뢰하지 않는다.
     * 다만 오디오는 컨테이너 매직바이트가 다양해(m4a/webm/ogg 등) 매직바이트만으로
     * 모두 잡기 어려우므로, 바이너리(PDF/텍스트 아님)이면서 오디오 매직바이트이거나
     * content-type이 audio/* 이거나 알려진 오디오 확장자인 경우 AUDIO로 본다.
     * PDF(%PDF)·UTF-8 텍스트·오디오 외는 OTHER로 보아 업로드 단계에서 거부된다(이미지 포함).
     */
    private DocumentType resolveType(MultipartFile file) {
        byte[] head = readHead(file, 1024);
        if (isPdf(head)) {
            return DocumentType.PDF;
        }
        if (isText(head)) {
            return DocumentType.TEXT;
        }
        // 여기 도달 = 바이너리(NUL/제어문자 포함). 오디오인지 추가 판별.
        if (isAudio(head, file)) {
            return DocumentType.AUDIO;
        }
        return DocumentType.OTHER;
    }

    /**
     * 오디오 판별: 매직바이트(우선) → content-type audio/* → 알려진 오디오 확장자 순.
     * 이미지(PNG/JPEG/GIF 등)는 어느 조건에도 걸리지 않아 OTHER로 남는다.
     */
    private boolean isAudio(byte[] head, MultipartFile file) {
        if (isAudioMagic(head)) {
            return true;
        }
        String contentType = file.getContentType();
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("audio/")) {
            return true;
        }
        String ext = extensionOf(file.getOriginalFilename());
        return ext != null && AUDIO_EXTENSIONS.contains(ext);
    }

    /**
     * 흔한 오디오 컨테이너의 매직바이트.
     * - "ID3" 또는 0xFF 0xFB/0xF3/0xF2 : MP3
     * - "RIFF"...."WAVE" : WAV
     * - "OggS" : Ogg
     * - "fLaC" : FLAC
     * - 0x1A45DFA3 (EBML) : WebM/Matroska(오디오 포함)
     * - offset 4 "ftyp" : ISO-BMFF(m4a/mp4 오디오)
     */
    private boolean isAudioMagic(byte[] h) {
        if (h.length >= 3 && h[0] == 'I' && h[1] == 'D' && h[2] == '3') {
            return true; // MP3 (ID3 tag)
        }
        if (h.length >= 2 && (h[0] & 0xFF) == 0xFF) {
            int b1 = h[1] & 0xFF;
            if (b1 == 0xFB || b1 == 0xF3 || b1 == 0xF2) {
                return true; // MP3 frame sync
            }
        }
        if (h.length >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'A' && h[10] == 'V' && h[11] == 'E') {
            return true; // WAV
        }
        if (h.length >= 4 && h[0] == 'O' && h[1] == 'g' && h[2] == 'g' && h[3] == 'S') {
            return true; // Ogg
        }
        if (h.length >= 4 && h[0] == 'f' && h[1] == 'L' && h[2] == 'a' && h[3] == 'C') {
            return true; // FLAC
        }
        if (h.length >= 4 && (h[0] & 0xFF) == 0x1A && (h[1] & 0xFF) == 0x45
                && (h[2] & 0xFF) == 0xDF && (h[3] & 0xFF) == 0xA3) {
            return true; // EBML → WebM/Matroska
        }
        if (h.length >= 8 && h[4] == 'f' && h[5] == 't' && h[6] == 'y' && h[7] == 'p') {
            return true; // ISO-BMFF (m4a/mp4)
        }
        return false;
    }

    private String extensionOf(String filename) {
        if (filename == null) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
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
        // 오디오는 Whisper가 파일명 확장자로 포맷을 추론하므로, 지원되는 오디오 확장자로 강제한다.
        String ext = switch (type) {
            case PDF -> "pdf";
            case AUDIO -> audioExtension(file);
            default -> "txt";
        };
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

    /**
     * 저장할 오디오 확장자를 결정한다. content-type → 원본 확장자 순으로 알려진 오디오
     * 확장자를 채택하고, 식별 불가 시 Whisper가 받아들이는 기본값(m4a)으로 강제한다.
     * 원본 파일명/타입은 신뢰하지 않으므로 화이트리스트(AUDIO_EXTENSIONS) 내 값만 사용한다.
     */
    private String audioExtension(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            String sub = contentType.toLowerCase(Locale.ROOT);
            int slash = sub.indexOf('/');
            if (slash >= 0) {
                sub = sub.substring(slash + 1);
            }
            int semi = sub.indexOf(';');
            if (semi >= 0) {
                sub = sub.substring(0, semi);
            }
            sub = sub.trim();
            // "x-m4a" → "m4a", "mpeg" 등 정규화
            if (sub.startsWith("x-")) {
                sub = sub.substring(2);
            }
            if (AUDIO_EXTENSIONS.contains(sub)) {
                return sub;
            }
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (ext != null && AUDIO_EXTENSIONS.contains(ext)) {
            return ext;
        }
        return "m4a";
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
