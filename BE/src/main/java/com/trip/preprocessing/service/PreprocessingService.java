package com.trip.preprocessing.service;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class PreprocessingService {

    private final UserAnalysisDataRepository userAnalysisDataRepository;
    private final STTManager sttManager;

    private final String uploadDir = "temp_uploads/";

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
            analysisData.updateRawText(content);
        } else if (analysisData.getDataType() == AnalysisDataType.VOICE_CALL) {
            String sttResult = sttManager.convertSpeechToText(file);
            analysisData.updateRawText(sttResult);
        }
    }
}
