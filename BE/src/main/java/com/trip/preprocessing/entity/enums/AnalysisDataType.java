package com.trip.preprocessing.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnalysisDataType {
    KAKAO_TALK("카카오톡 대화"),
    VOICE_CALL("음성 통화");

    private final String description;
}
