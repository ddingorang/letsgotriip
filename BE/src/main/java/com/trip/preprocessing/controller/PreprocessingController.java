package com.trip.preprocessing.controller;

import com.trip.preprocessing.dto.AnalysisResultResponse;
import com.trip.preprocessing.entity.enums.AnalysisDataType;
import com.trip.preprocessing.service.PreprocessingService;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
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
    public ResponseEntity<AnalysisResultResponse> uploadKakaoTalk(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        validateFile(file);
        return ResponseEntity.ok(
                preprocessingService.uploadAndProcess(userPrincipal.userId(), AnalysisDataType.KAKAO_TALK, file));
    }

    @PostMapping("upload/voice")
    public ResponseEntity<AnalysisResultResponse> uploadVoiceCall(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        validateFile(file);
        return ResponseEntity.ok(
                preprocessingService.uploadAndProcess(userPrincipal.userId(), AnalysisDataType.VOICE_CALL, file));
    }

    /** 빈/누락 파일 거부 — 빈 파일에도 200+dataId를 내보내던 false success 방지 */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new GeneralException(ResponseCode._BAD_REQUEST, "업로드할 파일이 비어있습니다.");
        }
    }
}
