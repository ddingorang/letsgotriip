// Created: 2026-06-08 16:00:42
package com.trip.festival.client;

import com.trip.festival.dto.TourApiResponse;
import com.trip.global.config.TourApiProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourApiClient {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 외부(TourAPI) 응답 지연 한계 — connect 4s / read 8s.
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(4);
    private static final Duration READ_TIMEOUT    = Duration.ofSeconds(8);

    private final TourApiProperties props;
    private RestClient restClient;

    @PostConstruct
    void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);

        this.restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Accept", "application/json")
                .requestFactory(factory)
                .build();
    }

    public List<TourApiResponse.FestivalItem> fetchPage(String lDongRegnCd, int pageNo, int numOfRows) {
        String today = LocalDate.now().format(DATE_FMT);

        TourApiResponse resp = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/searchFestival2")
                        .queryParam("serviceKey",    props.getKey())
                        .queryParam("numOfRows",     numOfRows)
                        .queryParam("pageNo",        pageNo)
                        .queryParam("MobileOS",      "ETC")
                        .queryParam("MobileApp",     "TripApp")
                        .queryParam("_type",         "json")
                        .queryParam("eventStartDate", today)
                        .queryParam("lDongRegnCd",   lDongRegnCd)
                        .build())
                .retrieve()
                .body(TourApiResponse.class);

        if (resp == null || resp.response() == null || resp.response().body() == null) {
            log.warn("TourAPI 빈 응답 — lDongRegnCd={}, page={}", lDongRegnCd, pageNo);
            return List.of();
        }

        List<TourApiResponse.FestivalItem> items = resp.response().body().items();
        return items != null ? items : List.of();
    }
}
