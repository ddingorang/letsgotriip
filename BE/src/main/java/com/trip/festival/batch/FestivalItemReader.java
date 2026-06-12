// Created: 2026-06-08 16:00:55
package com.trip.festival.batch;

import com.trip.festival.client.TourApiClient;
import com.trip.festival.dto.TourApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Slf4j
public class FestivalItemReader implements ItemReader<TourApiResponse.FestivalItem> {

    private static final int PAGE_SIZE = 100;
    private static final List<String> AREA_CODES = List.of(
            "11", "26", "27", "28", "29", "30", "31", "36",
            "41", "42", "43", "44", "45", "46", "47", "48", "50"
    );

    private final TourApiClient tourApiClient;
    private final Queue<TourApiResponse.FestivalItem> buffer = new LinkedList<>();
    private int areaCodeIndex = 0;
    private int currentPage = 0;

    public FestivalItemReader(TourApiClient tourApiClient) {
        this.tourApiClient = tourApiClient;
    }

    @Override
    public TourApiResponse.FestivalItem read() {
        if (!buffer.isEmpty()) {
            return buffer.poll();
        }

        // 버퍼가 비면 다음 페이지(또는 다음 지역)를 가져옴
        while (areaCodeIndex < AREA_CODES.size()) {
            String areaCode = AREA_CODES.get(areaCodeIndex);
            currentPage++;

            List<TourApiResponse.FestivalItem> items = tourApiClient.fetchPage(areaCode, currentPage, PAGE_SIZE);
            log.info("lDongRegnCd={} page={} → {}건", areaCode, currentPage, items.size());

            if (items.isEmpty()) {
                // 현재 지역 소진 → 다음 지역으로
                areaCodeIndex++;
                currentPage = 0;
                continue;
            }

            buffer.addAll(items);
            return buffer.poll();
        }

        return null; // 전 지역 완료
    }
}
