// Created: 2026-06-08 15:32:34
package com.trip.festival.repository;

import com.trip.festival.entity.Festival;
import com.trip.festival.entity.FestivalStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FestivalRepository extends JpaRepository<Festival, String> {

    // 추천 모듈(RecommendService)에서 사용 — 지역 코드별 전체 조회
    List<Festival> findByAreaCode(String areaCode);

    // ── 통합검색: 제목 부분일치(대소문자 무시) + 미경과(end_date >= today) 행사만, DB 상한 ──
    List<Festival> findByTitleContainingIgnoreCaseAndEndDateGreaterThanEqualOrderByStartDateAsc(
            String title, LocalDate today, Pageable pageable);

    // ── 조회용: end_date 미경과(>= today) 행사만 노출해 만료 행사 잔존을 차단 ──────────
    List<Festival> findByEndDateGreaterThanEqual(LocalDate today);

    List<Festival> findByAreaCodeAndEndDateGreaterThanEqual(String areaCode, LocalDate today);

    List<Festival> findByStatusAndEndDateGreaterThanEqual(FestivalStatus status, LocalDate today);

    List<Festival> findByAreaCodeAndStatusAndEndDateGreaterThanEqual(
            String areaCode, FestivalStatus status, LocalDate today);

    List<Festival> findByStatusNotAndEndDateGreaterThanEqual(FestivalStatus status, LocalDate today);
}
