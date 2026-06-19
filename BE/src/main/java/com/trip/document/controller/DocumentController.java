package com.trip.document.controller;

import com.trip.document.dto.DocumentResponse;
import com.trip.document.service.DocumentService;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private static final long MAX_SIZE = 20L * 1024 * 1024; // 20MB

    private final DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart MultipartFile file
    ) {
        validate(file);
        DocumentResponse response = documentService.upload(principal.userId(), file);
        return ResponseEntity.created(URI.create("/api/documents/" + response.id())).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(documentService.list(principal.userId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        documentService.delete(id, principal.userId());
        return ResponseEntity.noContent().build();
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new GeneralException(ResponseCode._BAD_REQUEST, "업로드할 파일이 비어있습니다.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new GeneralException(ResponseCode._BAD_REQUEST, "파일 크기는 20MB를 초과할 수 없습니다.");
        }
    }
}
