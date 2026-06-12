package com.ssafy.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class WeatherService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${api.openweather.key}")
    private String apiKey;

    public String getWeatherByLocation(String location) {
        String city = normalizeCity(location);

        // 1) 1차 시도
        String r1 = callOpenWeather(city);
        if (!looksLikeTransientError(r1))
            return r1;

        // 2) transient(429/5xx/timeout)일 때만 1회 재시도
        String r2 = callOpenWeather(city);
        return r2;
    }

    private String callOpenWeather(String city) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString("https://api.openweathermap.org/data/2.5/weather")
                    .queryParam("q", city)
                    .queryParam("appid", apiKey)
                    .queryParam("units", "metric")
                    .queryParam("lang", "kr")
                    .build(true)
                    .toUriString();

            String body = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            if (body == null || body.isBlank())
                return "날씨 조회 실패: 빈 응답";

            JsonNode root = objectMapper.readTree(body);

            // OpenWeather 에러 응답 케이스도 JSON으로 옴 (cod/message)
            String cod = root.path("cod").asText("");
            if (!cod.isBlank() && !"200".equals(cod)) {
                String msg = root.path("message").asText("unknown");
                return "날씨 조회 실패: cod=" + cod + ", message=" + msg + " (city=" + city + ")";
            }

            String weather = root.path("weather").get(0).path("description").asText("-");
            double temp = root.path("main").path("temp").asDouble();
            double feels = root.path("main").path("feels_like").asDouble();

            return String.format("%s 날씨: %s, 기온 %.1f℃, 체감 %.1f℃", city, weather, temp, feels);

        } catch (Exception e) {
            return "날씨 조회 실패: " + e.getClass().getSimpleName() + " - " + e.getMessage() + " (city=" + city + ")";
        }
    }

    private boolean looksLikeTransientError(String msg) {
        if (msg == null)
            return false;
        // 아주 러프하지만 효과 좋음: 429/5xx/timeout류만 재시도
        String m = msg.toLowerCase();
        return m.contains("429") || m.contains("5") && m.contains("cod=") || m.contains("timeout") || m.contains("connection");
    }

    private String normalizeCity(String location) {
        if (location == null)
            return "Seoul";
        String t = location.trim();

        // 흔한 한국어/형태 정리
        t = t.replace("날씨", "").replace("기온", "").replace("어때", "").trim();
        String[] parts = t.split("\\s+");
        if (parts.length >= 2) {
            // "서울 종로구" -> "서울"
            t = parts[0];
        }

        // OpenWeather는 한국 도시를 영문으로 요구하는 경우가 있어 안전 매핑 추가
        return switch (t) {
        case "서울", "서울시", "서울특별시" -> "Seoul";
        case "부산", "부산시", "부산광역시" -> "Busan";
        case "대구" -> "Daegu";
        case "인천" -> "Incheon";
        case "광주" -> "Gwangju";
        case "대전" -> "Daejeon";
        case "울산" -> "Ulsan";
        case "세종" -> "Sejong";
        case "제주", "제주도" -> "Jeju City";
        default -> t; // 나머지는 일단 그대로 시도
        };
    }
}
