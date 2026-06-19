package com.trip.context.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.trip.context.dto.WeatherResponse;
import com.trip.context.dto.WeatherResponse.DailyForecast;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Open-Meteo 무료 날씨 API 호출 클라이언트.
 * - API 키 불필요.
 * - 자체 RestClient 인스턴스를 구성 (connect 4s / read 5s 타임아웃).
 * - 호출/파싱 실패 시 warn 로깅 후 GeneralException(_INTERNAL_SERVER_ERROR) 으로 변환.
 */
@Slf4j
@Component
public class OpenMeteoClient {

    private static final String BASE_URL = "https://api.open-meteo.com";

    private RestClient restClient;

    @PostConstruct
    void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(4));
        factory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(factory)
                .build();
    }

    /**
     * 위/경도 기준 현재 날씨 + 오늘 포함 3일치 예보(일출/일몰 포함)를 조회한다.
     *
     * @throws GeneralException 외부 호출/파싱 실패 시 (_INTERNAL_SERVER_ERROR)
     */
    public WeatherResponse fetch(double lat, double lng) {
        try {
            OpenMeteoRaw raw = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/forecast")
                            .queryParam("latitude", lat)
                            .queryParam("longitude", lng)
                            .queryParam("current", "temperature_2m,weather_code")
                            .queryParam("daily",
                                    "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset")
                            .queryParam("timezone", "Asia/Seoul")
                            .queryParam("forecast_days", 3)
                            .build())
                    .retrieve()
                    .body(OpenMeteoRaw.class);

            if (raw == null) {
                log.warn("Open-Meteo 빈 응답 (lat={}, lng={})", lat, lng);
                throw new GeneralException(ResponseCode._INTERNAL_SERVER_ERROR);
            }

            return toWeatherResponse(lat, lng, raw);

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Open-Meteo 호출 실패 (lat={}, lng={}): {}", lat, lng, e.getMessage());
            throw new GeneralException(ResponseCode._INTERNAL_SERVER_ERROR);
        }
    }

    // ──────────────────────────────────────────────
    // 매핑
    // ──────────────────────────────────────────────
    private WeatherResponse toWeatherResponse(double lat, double lng, OpenMeteoRaw raw) {
        Double currentTemp = null;
        String currentDesc = "정보 없음";
        if (raw.current() != null) {
            currentTemp = raw.current().temperature();
            currentDesc = describe(raw.current().weatherCode());
        }

        List<DailyForecast> daily = new ArrayList<>();
        OpenMeteoRaw.Daily d = raw.daily();
        if (d != null && d.time() != null) {
            int n = d.time().size();
            for (int i = 0; i < n; i++) {
                daily.add(new DailyForecast(
                        d.time().get(i),
                        get(d.sunrise(), i),
                        get(d.sunset(), i),
                        get(d.tempMin(), i),
                        get(d.tempMax(), i),
                        describe(get(d.weatherCode(), i))
                ));
            }
        }

        return new WeatherResponse(lat, lng, currentTemp, currentDesc, daily);
    }

    private static <T> T get(List<T> list, int i) {
        return (list != null && i < list.size()) ? list.get(i) : null;
    }

    /**
     * WMO weather code → 한글 설명.
     * 0 맑음 / 1-3 대체로 맑음~구름 / 45,48 안개 / 51-67 비 /
     * 71-77 눈 / 80-82 소나기 / 95-99 뇌우 / 그 외 "정보 없음".
     */
    private static String describe(Integer code) {
        if (code == null) {
            return "정보 없음";
        }
        return switch (code) {
            case 0 -> "맑음";
            case 1 -> "대체로 맑음";
            case 2 -> "구름 조금";
            case 3 -> "흐림";
            case 45, 48 -> "안개";
            case 51, 53, 55, 56, 57, 61, 63, 65, 66, 67 -> "비";
            case 71, 73, 75, 77 -> "눈";
            case 80, 81, 82 -> "소나기";
            case 95, 96, 99 -> "뇌우";
            default -> "정보 없음";
        };
    }

    // ──────────────────────────────────────────────
    // Open-Meteo 원시 응답 (역직렬화 전용)
    // ──────────────────────────────────────────────
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenMeteoRaw(
            @JsonProperty("current") Current current,
            @JsonProperty("daily") Daily daily
    ) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Current(
                @JsonProperty("temperature_2m") Double temperature,
                @JsonProperty("weather_code") Integer weatherCode
        ) {}

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Daily(
                @JsonProperty("time") List<String> time,
                @JsonProperty("weather_code") List<Integer> weatherCode,
                @JsonProperty("temperature_2m_max") List<Double> tempMax,
                @JsonProperty("temperature_2m_min") List<Double> tempMin,
                @JsonProperty("sunrise") List<String> sunrise,
                @JsonProperty("sunset") List<String> sunset
        ) {}
    }
}
