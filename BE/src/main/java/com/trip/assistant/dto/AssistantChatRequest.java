package com.trip.assistant.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 어시스턴트 대화 요청.
 * conversationId가 null/blank면 서버가 새 대화로 간주하고 UUID를 발급한다.
 * memory가 null이면 모든 개인기록·RAG 사용을 허용(하위호환: 기존 동작 유지).
 */
public record AssistantChatRequest(
        String conversationId,

        @NotBlank
        String message,

        MemoryPrefs memory
) {
    /**
     * 챗봇이 사용자의 어떤 기록을 조회/주입할지 제어하는 플래그.
     * 각 값은 null 허용 — null은 "켜짐"으로 해석(미설정 클라이언트 하위호환).
     *
     * @param useRecords 마스터: 내 기록 조회 도구(계획·찜·리뷰·스토리) 전체 on/off
     * @param plans      내 여행 계획 조회 허용
     * @param favorites  내 찜 목록 조회 허용
     * @param reviews    내 작성 리뷰 조회 허용
     * @param stories    내 여행 스토리 조회 허용
     * @param recall     RAG 기억(내 문서/기록 검색 주입) 사용
     * @param planIds    활용할 계획 ID 화이트리스트. null/빈 배열이면 "전체"(plans 켜진 경우 내 모든 계획).
     */
    public record MemoryPrefs(
            Boolean useRecords,
            Boolean plans,
            Boolean favorites,
            Boolean reviews,
            Boolean stories,
            Boolean recall,
            List<Long> planIds
    ) {
        private static boolean on(Boolean b) { return b == null || b; }

        public boolean wantRecords()   { return on(useRecords); }
        public boolean wantPlans()     { return wantRecords() && on(plans); }
        public boolean wantFavorites() { return wantRecords() && on(favorites); }
        public boolean wantReviews()   { return wantRecords() && on(reviews); }
        public boolean wantStories()   { return wantRecords() && on(stories); }
        public boolean wantRecall()    { return on(recall); }

        /** 계획 화이트리스트(비어있으면 전체). RecordTools.getMyTravelPlans 필터에 사용. */
        public List<Long> planIdsOrEmpty() {
            return planIds == null ? List.of() : planIds;
        }

        /** 어떤 기록 도구라도 켜져 있는지(도구 묶음 장착 여부 판단). */
        public boolean anyRecord() {
            return wantPlans() || wantFavorites() || wantReviews() || wantStories();
        }
    }

    /** memory가 null이면 전체 허용 기본값을 돌려준다(하위호환). */
    public MemoryPrefs memoryOrDefault() {
        return memory != null ? memory
                : new MemoryPrefs(true, true, true, true, true, true, null);
    }
}
