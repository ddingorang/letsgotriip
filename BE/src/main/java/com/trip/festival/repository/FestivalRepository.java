// Created: 2026-06-08 15:32:34
package com.trip.festival.repository;

import com.trip.festival.entity.Festival;
import com.trip.festival.entity.FestivalStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FestivalRepository extends JpaRepository<Festival, String> {

    // 추천 모듈(RecommendService)에서 사용 — 지역 코드별 전체 조회
    List<Festival> findByAreaCode(String areaCode);

    // ── 통합검색: 제목 부분일치(대소문자 무시) + 미경과(end_date >= today) 행사만, DB 상한 ──
    List<Festival> findByTitleContainingIgnoreCaseAndEndDateGreaterThanEqualOrderByStartDateAsc(
            String title, LocalDate today, Pageable pageable);

    // ── 통합검색 + 날짜 윈도우: 제목 부분일치 + start_date ∈ [from, to] + end_date >= today ──
    List<Festival> findByTitleContainingIgnoreCaseAndStartDateBetweenAndEndDateGreaterThanEqualOrderByStartDateAsc(
            String title, LocalDate from, LocalDate to, LocalDate today, Pageable pageable);

    // ── 조회용: end_date 미경과(>= today) 행사만 노출해 만료 행사 잔존을 차단 ──────────
    List<Festival> findByEndDateGreaterThanEqual(LocalDate today);

    List<Festival> findByAreaCodeAndEndDateGreaterThanEqual(String areaCode, LocalDate today);

    List<Festival> findByStatusAndEndDateGreaterThanEqual(FestivalStatus status, LocalDate today);

    List<Festival> findByAreaCodeAndStatusAndEndDateGreaterThanEqual(
            String areaCode, FestivalStatus status, LocalDate today);

    List<Festival> findByStatusNotAndEndDateGreaterThanEqual(FestivalStatus status, LocalDate today);

    // ── 날짜 윈도우: start_date ∈ [from, to] AND end_date >= today ───────────────
    // 앞뒤 1개월 이내에 시작했거나 시작 예정인 행사만 조회 (기본 탐색용)
    List<Festival> findByStartDateBetweenAndEndDateGreaterThanEqual(
            LocalDate from, LocalDate to, LocalDate today);

    List<Festival> findByAreaCodeAndStartDateBetweenAndEndDateGreaterThanEqual(
            String areaCode, LocalDate from, LocalDate to, LocalDate today);

    List<Festival> findByStatusAndStartDateBetweenAndEndDateGreaterThanEqual(
            FestivalStatus status, LocalDate from, LocalDate to, LocalDate today);

    List<Festival> findByAreaCodeAndStatusAndStartDateBetweenAndEndDateGreaterThanEqual(
            String areaCode, FestivalStatus status, LocalDate from, LocalDate to, LocalDate today);

    // ── 위치 기반(Haversine) + 날짜 윈도우 ──────────────────────────────────────
    @Query(value = """
            SELECT * FROM festivals f
            WHERE f.start_date BETWEEN :from AND :to
              AND f.end_date   >= :today
              AND f.latitude   IS NOT NULL
              AND f.longitude  IS NOT NULL
              AND (6371000 * acos(LEAST(1.0,
                    cos(radians(:lat)) * cos(radians(f.latitude))
                    * cos(radians(f.longitude) - radians(:lng))
                    + sin(radians(:lat)) * sin(radians(f.latitude))
                  ))) <= :radiusM
            ORDER BY f.start_date ASC
            """, nativeQuery = true)
    List<Festival> findByLocationAndDateWindow(
            @Param("lat")     double lat,
            @Param("lng")     double lng,
            @Param("radiusM") int    radiusM,
            @Param("from")    LocalDate from,
            @Param("to")      LocalDate to,
            @Param("today")   LocalDate today);
}
