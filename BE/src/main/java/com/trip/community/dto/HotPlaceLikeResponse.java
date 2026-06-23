package com.trip.community.dto;

/** 핫플 좋아요 상태 응답 — { liked, likeCount }. */
public record HotPlaceLikeResponse(boolean liked, int likeCount) {}
