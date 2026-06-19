// Created: 2026-06-08 15:33:06
package com.trip.festival.batch;

import com.trip.festival.dto.TourApiResponse;
import com.trip.festival.entity.Festival;
import com.trip.festival.entity.FestivalStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class FestivalItemProcessor implements ItemProcessor<TourApiResponse.FestivalItem, Festival> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public Festival process(TourApiResponse.FestivalItem item) {
        if (item.contentId() == null || item.contentId().isBlank()) {
            return null; // contentId 없으면 건너뜀
        }

        LocalDate startDate = parseDate(item.contentId(), "eventStartDate", item.eventStartDate());
        LocalDate endDate   = parseDate(item.contentId(), "eventEndDate", item.eventEndDate());
        LocalDate today     = LocalDate.now();

        // endDate 원본이 존재했는데 파싱에 실패한 경우(blank 가 아닌데 endDate == null)를 구분한다.
        // 이 경우 endDate 기반 ENDED/ONGOING 판정이 불가능하므로 ONGOING 으로 위장하지 않는다.
        boolean endDatePresent = item.eventEndDate() != null && !item.eventEndDate().isBlank();
        boolean endDateUnparseable = endDatePresent && endDate == null;

        FestivalStatus status;
        if (endDate != null && endDate.isBefore(today)) {
            status = FestivalStatus.ENDED;
        } else if (startDate != null && startDate.isAfter(today)) {
            // 시작일이 미래면 endDate 파싱 여부와 무관하게 예정(UPCOMING)으로 본다.
            status = FestivalStatus.UPCOMING;
        } else if (endDateUnparseable) {
            // 종료일을 신뢰할 수 없으므로 ONGOING(진행 중) 으로 단정하지 않는다.
            // 시작일 정보로도 분기되지 않은 항목은 보수적으로 ENDED 로 보류하여
            // 만료 필터(end_date >= today)에서 노출되지 않도록 하고, 경고로 명시한다.
            log.warn("[FestivalSync] contentId={} 의 종료일을 파싱할 수 없어 status 판정을 보류(ENDED 처리)합니다. eventEndDate='{}'",
                    item.contentId(), item.eventEndDate());
            status = FestivalStatus.ENDED;
        } else {
            status = FestivalStatus.ONGOING;
        }

        String address = Stream.of(item.addr1(), item.addr2())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" "))
                .strip();

        // 지역 코드 체계 통일: 배치가 TourAPI를 조회할 때 사용하는 키가 lDongRegnCd 이므로
        // 저장도 lDongRegnCd 를 표준으로 한다. 응답에 누락 시 areacode 로 폴백한다.
        // (FestivalService.getFestivals 의 areaCode 필터도 이 컬럼을 조회한다)
        String areaCode    = firstNonBlank(item.lDongRegnCd(), item.areaCode());
        String sigunguCode = firstNonBlank(item.lDongSignguCd(), item.sigunguCode());

        return Festival.builder()
                .contentId(item.contentId())
                .title(item.title())
                .address(address.isEmpty() ? null : address)
                .tel(item.tel())
                .imageUrl(item.firstimage())
                .startDate(startDate)
                .endDate(endDate)
                .latitude(parseCoord(item.mapy()))
                .longitude(parseCoord(item.mapx()))
                .areaCode(areaCode)
                .sigunguCode(sigunguCode)
                .status(status)
                .build();
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) return primary;
        if (fallback != null && !fallback.isBlank()) return fallback;
        return null;
    }

    private LocalDate parseDate(String contentId, String field, String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim(), DATE_FMT);
        } catch (DateTimeParseException e) {
            // 조용히 null 을 반환하면 status 오판으로 이어지므로, 원본 문자열과 함께 경고를 남긴다.
            log.warn("[FestivalSync] contentId={} 의 {} 날짜 파싱 실패 → null 처리. 원본='{}'",
                    contentId, field, s);
            return null;
        }
    }

    private Double parseCoord(String s) {
        if (s == null || s.isBlank()) return null; // 실제 결측은 blank/누락으로만 판정
        try {
            // 0.0 도 유효 좌표(적도/본초자오선)일 수 있으므로 그대로 보존한다.
            // 결측은 위의 blank/누락 분기로만 처리하고, 값이 파싱되면 0.0 이라도 유지한다.
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
