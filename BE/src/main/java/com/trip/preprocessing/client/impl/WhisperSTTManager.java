package com.trip.preprocessing.client.impl;

import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.preprocessing.client.STTManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.time.Duration;
import java.util.Map;

@Slf4j
@Primary
@Component
public class WhisperSTTManager implements STTManager {

    // Whisper 전사는 오디오 길이에 비례해 오래 걸림 — connect 4s / read 120s.
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(4);
    private static final Duration READ_TIMEOUT    = Duration.ofSeconds(120);

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    private RestClient restClient;

    @PostConstruct
    void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

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

            if (response != null && response.get("text") instanceof String result && !result.isBlank()) {
                log.info("Whisper STT conversion successful");
                return result;
            }

            // 응답이 없거나 text가 비어 있으면 빈 전사 → 실패로 처리(가짜 성공 문자열 반환 금지)
            throw new GeneralException(ResponseCode._INTERNAL_SERVER_ERROR, "Whisper API 응답이 비어 있거나 유효하지 않습니다.");

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            // 과거에는 실패 메시지를 정상 전사 결과처럼 반환해 FAILED가 SUCCESS로 저장되는 버그가 있었음.
            // API 호출 실패 시 예외를 던져 상위(PreprocessingService)에서 저장하지 않도록 한다.
            log.error("Error during Whisper STT conversion: {}", e.getMessage());
            throw new GeneralException(ResponseCode._INTERNAL_SERVER_ERROR, "STT 변환에 실패했습니다.", e);
        }
    }
}
