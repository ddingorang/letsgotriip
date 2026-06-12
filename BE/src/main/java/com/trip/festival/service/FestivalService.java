// Created: 2026-06-08 15:34:54
package com.trip.festival.service;

import com.trip.festival.dto.FestivalResponse;
import com.trip.festival.entity.Festival;
import com.trip.festival.entity.FestivalStatus;
import com.trip.festival.repository.FestivalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalService {

    private final FestivalRepository festivalRepository;

    public List<FestivalResponse> getFestivals(String areaCode, String status) {
        boolean hasArea   = areaCode != null && !areaCode.isBlank();
        boolean hasStatus = status   != null && !status.isBlank();

        List<Festival> festivals;

        if (hasArea && hasStatus) {
            festivals = festivalRepository.findByAreaCodeAndStatus(
                    areaCode, FestivalStatus.valueOf(status.toUpperCase()));
        } else if (hasArea) {
            festivals = festivalRepository.findByAreaCode(areaCode);
        } else if (hasStatus) {
            festivals = festivalRepository.findByStatus(
                    FestivalStatus.valueOf(status.toUpperCase()));
        } else {
            // 기본: 종료된 행사 제외
            festivals = festivalRepository.findByStatusNot(FestivalStatus.ENDED);
        }

        return festivals.stream()
                .map(FestivalResponse::from)
                .toList();
    }
}
