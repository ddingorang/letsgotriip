// Created: 2026-06-08 15:33:06
package com.trip.festival.batch;

import com.trip.festival.dto.TourApiResponse;
import com.trip.festival.entity.Festival;
import com.trip.festival.entity.FestivalStatus;
import org.springframework.batch.item.ItemProcessor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FestivalItemProcessor implements ItemProcessor<TourApiResponse.FestivalItem, Festival> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public Festival process(TourApiResponse.FestivalItem item) {
        if (item.contentId() == null || item.contentId().isBlank()) {
            return null; // contentId 없으면 건너뜀
        }

        LocalDate startDate = parseDate(item.eventStartDate());
        LocalDate endDate   = parseDate(item.eventEndDate());
        LocalDate today     = LocalDate.now();

        FestivalStatus status;
        if (endDate != null && endDate.isBefore(today)) {
            status = FestivalStatus.ENDED;
        } else if (startDate != null && startDate.isAfter(today)) {
            status = FestivalStatus.UPCOMING;
        } else {
            status = FestivalStatus.ONGOING;
        }

        String address = Stream.of(item.addr1(), item.addr2())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" "))
                .strip();

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
                .areaCode(item.lDongRegnCd())
                .sigunguCode(item.lDongSignguCd())
                .status(status)
                .build();
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim(), DATE_FMT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private Double parseCoord(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            double v = Double.parseDouble(s.trim());
            return v == 0.0 ? null : v;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
