package com.trip.preprocessing.controller;

import com.trip.preprocessing.entity.enums.AnalysisDataType;
import com.trip.preprocessing.service.PreprocessingService;
import com.trip.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("analysis")
@RequiredArgsConstructor
public class PreprocessingController {

    private final PreprocessingService preprocessingService;

    @PostMapping("upload/kakao")
    public ResponseEntity<Long> uploadKakaoTalk(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        Long dataId = preprocessingService.uploadAndProcess(userPrincipal.userId(), AnalysisDataType.KAKAO_TALK, file);
        return ResponseEntity.ok(dataId);
    }

    @PostMapping("upload/voice")
    public ResponseEntity<Long> uploadVoiceCall(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        Long dataId = preprocessingService.uploadAndProcess(userPrincipal.userId(), AnalysisDataType.VOICE_CALL, file);
        return ResponseEntity.ok(dataId);
    }
}
