package com.trip.preprocessing.client.impl;

import com.trip.preprocessing.client.STTManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.util.Map;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class WhisperSTTManager implements STTManager {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    private final RestClient restClient;

    @Override
    public String convertSpeechToText(File audioFile) {
        log.info("Starting Whisper STT conversion for file: {}", audioFile.getName());

        try {
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("file", new FileSystemResource(audioFile));
            bodyBuilder.part("model", "whisper-1");
            bodyBuilder.part("language", "ko"); // 한국어로 명시적 설정

            MultiValueMap<String, HttpEntity<?>> multipartBody = bodyBuilder.build();

            // Whisper API 호출
            Map<String, Object> response = restClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipartBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("text")) {
                String result = (String) response.get("text");
                log.info("Whisper STT conversion successful");
                return result;
            }

            throw new RuntimeException("Whisper API response is empty or invalid");

        } catch (Exception e) {
            log.error("Error during Whisper STT conversion: {}", e.getMessage());
            // 실제 서비스에서는 에러 처리를 더 세밀하게 해야 함
            return "STT 변환 실패: " + e.getMessage();
        }
    }
}
