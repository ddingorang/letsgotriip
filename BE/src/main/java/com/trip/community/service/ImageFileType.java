package com.trip.community.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;

/**
 * 업로드된 이미지의 실제 바이트(매직 시그니처)를 검사해 타입을 판별한다.
 * 클라이언트가 보낸 Content-Type/원본 확장자는 신뢰하지 않는다.
 *
 * 저장 확장자/Content-Type은 여기서 판별된 실제 타입으로 강제한다.
 */
public enum ImageFileType {

    JPEG("jpg"),
    PNG("png"),
    GIF("gif"),
    WEBP("webp");

    private final String extension;

    ImageFileType(String extension) {
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }

    /**
     * 파일의 선두 바이트를 읽어 실제 이미지 타입을 판별한다.
     * 알려진 이미지 시그니처가 아니면 null 반환(거부 대상).
     */
    public static ImageFileType detect(MultipartFile file) {
        byte[] head;
        try {
            head = readHead(file, 12);
        } catch (IOException e) {
            return null;
        }
        if (head.length < 4) {
            return null;
        }

        // JPEG: FF D8 FF
        if (matches(head, 0xFF, 0xD8, 0xFF)) {
            return JPEG;
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (matches(head, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return PNG;
        }
        // GIF: "GIF87a" / "GIF89a"
        if (matches(head, 0x47, 0x49, 0x46, 0x38)
                && (head.length > 4 && (head[4] == 0x37 || head[4] == 0x39))
                && (head.length > 5 && head[5] == 0x61)) {
            return GIF;
        }
        // WEBP: "RIFF"...."WEBP"
        if (head.length >= 12
                && matches(head, 0x52, 0x49, 0x46, 0x46)
                && head[8] == 0x57 && head[9] == 0x45 && head[10] == 0x42 && head[11] == 0x50) {
            return WEBP;
        }
        return null;
    }

    private static byte[] readHead(MultipartFile file, int len) throws IOException {
        try (var in = file.getInputStream()) {
            byte[] buf = new byte[len];
            int read = 0;
            int n;
            while (read < len && (n = in.read(buf, read, len - read)) != -1) {
                read += n;
            }
            return read == len ? buf : Arrays.copyOf(buf, read);
        }
    }

    private static boolean matches(byte[] data, int... signature) {
        if (data.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((data[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
