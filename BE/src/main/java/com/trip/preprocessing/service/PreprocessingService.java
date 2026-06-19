package com.trip.preprocessing.service;

import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.preprocessing.client.STTManager;
import com.trip.preprocessing.entity.UserAnalysisData;
import com.trip.preprocessing.entity.enums.AnalysisDataType;
import com.trip.preprocessing.repository.UserAnalysisDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PreprocessingService {

    private final UserAnalysisDataRepository userAnalysisDataRepository;
    private final STTManager sttManager;

    private final String uploadDir = "temp_uploads/";

    // PII 마스킹 정규식: 전화번호 / 주민등록번호 / 이메일
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("01[016789]-?\\d{3,4}-?\\d{4}");
    private static final Pattern RRN_PATTERN =
            Pattern.compile("\\d{6}-?\\d{7}");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    @Transactional
    public Long uploadAndProcess(Long userId, AnalysisDataType dataType, MultipartFile file) throws IOException {
        Path directory = Paths.get(uploadDir);
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = directory.resolve(fileName);
        file.transferTo(filePath.toFile());

        UserAnalysisData analysisData = UserAnalysisData.builder()
                .userId(userId)
                .dataType(dataType)
                .originalFileName(file.getOriginalFilename())
                .storagePath(filePath.toString())
                .build();

        userAnalysisDataRepository.save(analysisData);

        processData(analysisData, filePath.toFile());

        return analysisData.getId();
    }

    private void processData(UserAnalysisData analysisData, File file) throws IOException {
        if (analysisData.getDataType() == AnalysisDataType.KAKAO_TALK) {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            analysisData.updateRawText(maskPii(content));
        } else if (analysisData.getDataType() == AnalysisDataType.VOICE_CALL) {
            String sttResult = sttManager.convertSpeechToText(file);
            // 전사 결과가 null/blank이면 빈 전사 → 성공 저장하지 않고 실패로 처리
            // (convertSpeechToText가 예외 대신 빈 값을 반환하는 구현을 대비한 방어)
            if (sttResult == null || sttResult.isBlank()) {
                log.error("STT 전사 결과가 비어 있습니다 — analysisId={}", analysisData.getId());
                throw new GeneralException(ResponseCode._INTERNAL_SERVER_ERROR, "음성 전사 결과가 비어 있습니다.");
            }
            // PII 마스킹 후 저장
            analysisData.updateRawText(maskPii(sttResult));
        }
    }

    /**
     * 개인정보(PII) 마스킹: 전화번호 / 주민등록번호 / 이메일을 마스킹 토큰으로 치환한다.
     * 주민번호를 전화번호보다 먼저 치환해 패턴 충돌을 피한다.
     */
    String maskPii(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String masked = RRN_PATTERN.matcher(text).replaceAll("[주민번호]");
        masked = PHONE_PATTERN.matcher(masked).replaceAll("[전화번호]");
        masked = EMAIL_PATTERN.matcher(masked).replaceAll("[이메일]");
        return masked;
    }
}
