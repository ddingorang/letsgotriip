// Created: 2026-06-08 15:34:54
package com.trip.festival.service;

import com.trip.festival.dto.FestivalResponse;
import com.trip.festival.entity.Festival;
import com.trip.festival.entity.FestivalStatus;
import com.trip.festival.repository.FestivalRepository;
import lombok.RequiredArgsConstructor;
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

        // 만료(end_date 경과) 행사는 sync 사이에도 노출되지 않도록 항상 end_date >= today 로 거른다.
        LocalDate today = LocalDate.now();

        List<Festival> festivals;

        if (hasArea && hasStatus) {
            festivals = festivalRepository.findByAreaCodeAndStatusAndEndDateGreaterThanEqual(
                    areaCode, FestivalStatus.valueOf(status.toUpperCase()), today);
        } else if (hasArea) {
            festivals = festivalRepository.findByAreaCodeAndEndDateGreaterThanEqual(areaCode, today);
        } else if (hasStatus) {
            festivals = festivalRepository.findByStatusAndEndDateGreaterThanEqual(
                    FestivalStatus.valueOf(status.toUpperCase()), today);
        } else {
            // 기본: 종료된 행사 제외 + 만료 행사 제외
            festivals = festivalRepository.findByStatusNotAndEndDateGreaterThanEqual(
                    FestivalStatus.ENDED, today);
        }

        return festivals.stream()
                .map(FestivalResponse::from)
                .toList();
    }
}
