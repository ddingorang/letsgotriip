package com.trip.preprocessing.client.impl;

import com.trip.preprocessing.client.STTManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

@Slf4j
@Component
public class MockSTTManager implements STTManager {

    @Override
    public String convertSpeechToText(File audioFile) {
        log.info("Mocking STT conversion for file: {}", audioFile.getName());
        return "이것은 음성 통화 파일에서 변환된 가상의 텍스트 데이터입니다. 제주도 여행 계획에 대해 이야기하고 있습니다.";
    }
}
