package com.trip.favorite.entity;

/**
 * 즐겨찾기 대상 종류.
 * - ATTRACTION : 관광지(TourAPI contentId, 문자열)
 * - POST       : 커뮤니티 게시글(Long → 문자열 저장)
 * - HOTPLACE   : 핫플(Long → 문자열 저장)
 */
public enum FavoriteTargetType {
    ATTRACTION,
    POST,
    HOTPLACE
}
