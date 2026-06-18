// Created: 2026-06-15 22:17:49
package com.trip.community.service;

import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB

    @Value("${app.upload.dir:uploads/community}")
    private String uploadDir;

    @Value("${app.upload.url-prefix:/uploads/community}")
    private String urlPrefix;

    public String store(MultipartFile file) {
        validate(file);

        String ext = extractExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + ext;
        Path targetPath = Paths.get(uploadDir).resolve(filename);

        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to store file: {}", filename, e);
            throw new GeneralException(ResponseCode.FILE_UPLOAD_FAILED);
        }

        return urlPrefix + "/" + filename;
    }

    public void delete(String imageUrl) {
        String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        Path filePath = Paths.get(uploadDir).resolve(filename);
        try {
            Files.deleteIfExists(filePath.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", filename, e);
        }
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("파일 크기는 10MB를 초과할 수 없습니다.");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. (jpeg, png, webp, gif만 허용)");
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "jpg";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
    }
}
