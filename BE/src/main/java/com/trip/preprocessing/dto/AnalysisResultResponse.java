package com.trip.preprocessing.dto;

import java.util.List;

/**
 * 대화/음성 분석 결과 응답 — 사용자에게 '실제로 분석됐음'을 보여준다.
 *
 * @param dataId            저장된 분석 데이터 id
 * @param transcriptPreview 마스킹된 전사/대화 텍스트 미리보기(앞부분)
 * @param transcriptChars   전체 전사 글자 수
 * @param truncated         미리보기가 잘렸는지
 * @param themeKeys         LLM이 추출한 여행 취향 테마 키(sea/food/history…)
 * @param themeLabels       위 키의 한글 라벨(바다/맛집/역사·문화…)
 */
public record AnalysisResultResponse(
        Long dataId,
        String transcriptPreview,
        int transcriptChars,
        boolean truncated,
        List<String> themeKeys,
        List<String> themeLabels
) {}
