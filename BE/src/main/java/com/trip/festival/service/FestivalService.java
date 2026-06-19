// Created: 2026-06-08 15:34:54
package com.trip.festival.service;

import com.trip.festival.dto.FestivalResponse;
import com.trip.festival.entity.Festival;
import com.trip.festival.entity.FestivalStatus;
import com.trip.festival.repository.FestivalRepository;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
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
                    areaCode, parseStatus(status), today);
        } else if (hasArea) {
            festivals = festivalRepository.findByAreaCodeAndEndDateGreaterThanEqual(areaCode, today);
        } else if (hasStatus) {
            festivals = festivalRepository.findByStatusAndEndDateGreaterThanEqual(
                    parseStatus(status), today);
        } else {
            // 기본: 종료된 행사 제외 + 만료 행사 제외
            festivals = festivalRepository.findByStatusNotAndEndDateGreaterThanEqual(
                    FestivalStatus.ENDED, today);
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
    private FestivalStatus parseStatus(String status) {
        try {
            return FestivalStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new GeneralException(ResponseCode._BAD_REQUEST,
                    "유효하지 않은 행사 상태입니다: " + status);
        }
    }
}
