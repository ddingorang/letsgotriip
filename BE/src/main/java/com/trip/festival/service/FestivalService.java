// Created: 2026-06-08 15:34:54
package com.trip.festival.service;

import com.trip.attraction.dto.AttractionTourApiResponse.AttractionItem;
import com.trip.festival.dto.FestivalResponse;
import com.trip.festival.entity.Festival;
import com.trip.festival.entity.FestivalStatus;
import com.trip.festival.repository.FestivalRepository;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalService {

    private final FestivalRepository festivalRepository;

    public List<FestivalResponse> getFestivals(String areaCode, String status) {
        boolean hasArea   = areaCode != null && !areaCode.isBlank();
        boolean hasStatus = status   != null && !status.isBlank();

        LocalDate today = LocalDate.now();
        // 앞뒤 1개월 이내에 시작했거나 진행 예정인 행사만 노출
        LocalDate from = today.minusMonths(1);
        LocalDate to   = today.plusMonths(1);

        List<Festival> festivals;

        if (hasArea && hasStatus) {
            festivals = festivalRepository.findByAreaCodeAndStatusAndStartDateBetweenAndEndDateGreaterThanEqual(
                    areaCode, parseStatus(status), from, to, today);
        } else if (hasArea) {
            festivals = festivalRepository.findByAreaCodeAndStartDateBetweenAndEndDateGreaterThanEqual(
                    areaCode, from, to, today);
        } else if (hasStatus) {
            festivals = festivalRepository.findByStatusAndStartDateBetweenAndEndDateGreaterThanEqual(
                    parseStatus(status), from, to, today);
        } else {
            festivals = festivalRepository.findByStartDateBetweenAndEndDateGreaterThanEqual(
                    from, to, today);
        }

        return festivals.stream()
                .map(FestivalResponse::from)
                .toList();
    }

    /**
     * status 쿼리 파라미터를 FestivalStatus 로 변환.
     * 정의되지 않은 값(예: ?status=foo)이면 valueOf 가 IllegalArgumentException 을 던져
     * 500 으로 새어나가므로, GeneralException(_BAD_REQUEST) 으로 감싸 400 으로 처리한다.
     * (호출부에서 이미 null/blank 는 전체 조회로 분기하므로 여기서는 값이 있다고 가정한다)
     */
    /**
     * 축제 통합 검색 — AttractionItem 형태로 반환해 attraction 검색과 동일한 계약을 유지한다.
     * keyword / areaCode / 위치(mapX·mapY·radius) 중 하나를 기준으로 날짜 윈도우(±1개월) 적용.
     */
    public List<AttractionItem> searchAsItems(String keyword, String areaCode,
                                               String mapX, String mapY, Integer radius) {
        LocalDate today = LocalDate.now();
        LocalDate from  = today.minusMonths(1);
        LocalDate to    = today.plusMonths(1);

        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasCoords  = mapX != null && !mapX.isBlank() && mapY != null && !mapY.isBlank();
        boolean hasArea    = areaCode != null && !areaCode.isBlank();

        List<Festival> festivals;

        if (hasKeyword) {
            festivals = festivalRepository
                    .findByTitleContainingIgnoreCaseAndStartDateBetweenAndEndDateGreaterThanEqualOrderByStartDateAsc(
                            keyword, from, to, today, PageRequest.of(0, 30));
        } else if (hasCoords) {
            double lat     = Double.parseDouble(mapY);
            double lng     = Double.parseDouble(mapX);
            int    radiusM = radius != null ? radius : 20000;
            festivals = festivalRepository.findByLocationAndDateWindow(lat, lng, radiusM, from, to, today);
        } else if (hasArea) {
            festivals = festivalRepository.findByAreaCodeAndStartDateBetweenAndEndDateGreaterThanEqual(
                    areaCode, from, to, today);
        } else {
            festivals = festivalRepository.findByStartDateBetweenAndEndDateGreaterThanEqual(from, to, today);
        }

        return festivals.stream().map(this::toAttractionItem).toList();
    }

    private AttractionItem toAttractionItem(Festival f) {
        return new AttractionItem(
                f.getContentId(),
                "15",
                f.getTitle(),
                f.getAddress(),
                f.getAreaCode(),
                f.getSigunguCode(),
                f.getLongitude() != null ? String.valueOf(f.getLongitude()) : null,  // mapx = 경도
                f.getLatitude()  != null ? String.valueOf(f.getLatitude())  : null,  // mapy = 위도
                f.getImageUrl(),
                f.getTel(),
                null,   // homepage
                null    // overview
        );
    }

    private FestivalStatus parseStatus(String status) {
        try {
            return FestivalStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new GeneralException(ResponseCode._BAD_REQUEST,
                    "유효하지 않은 행사 상태입니다: " + status);
        }
    }
}
