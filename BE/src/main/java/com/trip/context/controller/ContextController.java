package com.trip.context.controller;

import com.trip.context.dto.EvStationResponse;
import com.trip.context.dto.NewsItemResponse;
import com.trip.context.dto.WeatherResponse;
import com.trip.context.service.EvStationService;
import com.trip.context.service.NewsService;
import com.trip.context.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 여행 맥락 정보 — 비회원 허용 공개 엔드포인트.
 *
 * GET /api/context/weather?lat&lng — 날씨 + 일출/일몰 (Open-Meteo 실데이터)
 * GET /api/context/ev-stations?lat&lng — 주변 전기차 충전소 (데모)
 * GET /api/context/news — 여행 뉴스 (데모)
 */
@RestController
@RequestMapping("/api/context")
@RequiredArgsConstructor
public class ContextController {

    private final WeatherService weatherService;
    private final EvStationService evStationService;
    private final NewsService newsService;

    /**
     * GET /api/context/weather
     * 위/경도 기준 현재 날씨 + 3일 예보.
     */
    @GetMapping("/weather")
    public ResponseEntity<WeatherResponse> getWeather(
            @RequestParam double lat,
            @RequestParam double lng) {
        return ResponseEntity.ok(weatherService.getWeather(lat, lng));
    }

    /**
     * GET /api/context/ev-stations
     * 좌표 주변 데모 전기차 충전소 목록.
     */
    @GetMapping("/ev-stations")
    public ResponseEntity<List<EvStationResponse>> getEvStations(
            @RequestParam double lat,
            @RequestParam double lng) {
        return ResponseEntity.ok(evStationService.nearby(lat, lng));
    }

    /**
     * GET /api/context/news
     * 데모 여행 뉴스 목록.
     */
    @GetMapping("/news")
    public ResponseEntity<List<NewsItemResponse>> getNews() {
        return ResponseEntity.ok(newsService.travelNews());
    }
}
