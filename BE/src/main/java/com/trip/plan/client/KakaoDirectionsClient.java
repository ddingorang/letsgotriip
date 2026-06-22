package com.trip.plan.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 카카오 모빌리티 다중경유지 길찾기 클라이언트.
 * - 자동차 도로 경로(좌표열) + 총거리/소요시간/요금을 조회한다.
 * - REST API 키 필요(브라우저 노출 금지 → 서버에서만 호출).
 * - 키 미설정/호출 실패 시 예외 대신 null 반환 → 호출부가 직선거리(Haversine)로 폴백한다.
 */
@Slf4j
@Component
public class KakaoDirectionsClient {

    /** 다중경유지 길찾기 1회 호출 좌표 상한(출발+도착+경유 = 30). */
    private static final int MAX_POINTS = 30;

    @Value("${kakao.directions.base-url:https://apis-navi.kakaomobility.com}")
    private String baseUrl;

    @Value("${kakao.directions.key:}")
    private String apiKey;

    private RestClient restClient;

    @PostConstruct
    void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(4));
        factory.setReadTimeout(Duration.ofSeconds(8));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** 좌표(위/경도) — 입력 순서대로 출발→경유→도착으로 이어진다. */
    public record Point(double lat, double lng) {}

    /** 길찾기 결과 — path 는 도로를 따라가는 [lat,lng] 좌표열. */
    public record RouteResult(
            int distanceMeters,
            int durationSeconds,
            int taxiFare,
            int tollFare,
            List<double[]> path
    ) {}

    /**
     * 순서대로 정렬된 좌표들의 자동차 경로를 조회한다.
     * @return 성공 시 RouteResult, 좌표 부족·키 미설정·호출 실패 시 null
     */
    public RouteResult route(List<Point> points) {
        if (!isEnabled() || points == null || points.size() < 2) {
            return null;
        }
        // 상한 초과 시 앞에서부터 MAX_POINTS 만큼만 사용(데모 — 하루 일정은 통상 그 이하).
        List<Point> pts = points.size() > MAX_POINTS ? points.subList(0, MAX_POINTS) : points;

        Point origin = pts.get(0);
        Point dest = pts.get(pts.size() - 1);
        List<Map<String, Double>> waypoints = new ArrayList<>();
        for (int i = 1; i < pts.size() - 1; i++) {
            waypoints.add(Map.of("x", pts.get(i).lng(), "y", pts.get(i).lat()));
        }

        Map<String, Object> body = Map.of(
                "origin", Map.of("x", origin.lng(), "y", origin.lat()),
                "destination", Map.of("x", dest.lng(), "y", dest.lat()),
                "waypoints", waypoints,
                "priority", "RECOMMEND"
        );

        try {
            KakaoRoutesResponse resp = restClient.post()
                    .uri("/v1/waypoints/directions")
                    .header("Authorization", "KakaoAK " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(KakaoRoutesResponse.class);

            if (resp == null || resp.routes() == null || resp.routes().isEmpty()) {
                log.warn("카카오 길찾기 빈 응답");
                return null;
            }
            KakaoRoute r = resp.routes().get(0);
            if (r.resultCode() != 0) {
                log.warn("카카오 길찾기 실패: code={} msg={}", r.resultCode(), r.resultMsg());
                return null;
            }
            return toResult(r);
        } catch (Exception e) {
            log.warn("카카오 길찾기 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 구간별 길찾기 — 연속한 두 장소씩 도로경로를 조회해 이어붙인다.
     * 한 지점이 비도로(코드 106/107 등)라 그 구간이 실패하면 그 구간만 직선으로 잇고,
     * 나머지는 실제 도로경로 값을 유지한다("되는 건 실제로"). 좌표 2개 미만/키 미설정이면 null.
     * 거리/시간은 실패 구간만 직선거리(Haversine)·약 40km/h로 근사 합산한다.
     */
    public RouteResult routeStitched(List<Point> points) {
        if (!isEnabled() || points == null || points.size() < 2) {
            return null;
        }
        List<Point> pts = points.size() > MAX_POINTS ? points.subList(0, MAX_POINTS) : points;
        List<double[]> path = new ArrayList<>();
        int dist = 0, dur = 0, taxi = 0, toll = 0;
        for (int i = 0; i + 1 < pts.size(); i++) {
            Point a = pts.get(i), b = pts.get(i + 1);
            RouteResult seg = route(List.of(a, b)); // 단일 구간(출발→도착, 경유 없음)
            if (seg != null && seg.path() != null && seg.path().size() >= 2) {
                appendPath(path, seg.path());
                dist += seg.distanceMeters();
                dur  += seg.durationSeconds();
                taxi += seg.taxiFare();
                toll += seg.tollFare();
            } else {
                // 직선 폴백 — 두 점을 직접 잇고 거리/시간은 근사
                appendPath(path, List.of(new double[]{a.lat(), a.lng()}, new double[]{b.lat(), b.lng()}));
                int d = (int) haversineMeters(a, b);
                dist += d;
                dur  += (int) (d / 11.1); // 약 40km/h
            }
        }
        return path.size() < 2 ? null : new RouteResult(dist, dur, taxi, toll, path);
    }

    /** 경로 좌표열을 이어붙인다. 직전 끝점과 새 구간 첫점이 같으면 중복을 건너뛴다. */
    private void appendPath(List<double[]> acc, List<double[]> seg) {
        int start = 0;
        if (!acc.isEmpty() && !seg.isEmpty()) {
            double[] last = acc.get(acc.size() - 1);
            double[] first = seg.get(0);
            if (Math.abs(last[0] - first[0]) < 1e-7 && Math.abs(last[1] - first[1]) < 1e-7) start = 1;
        }
        for (int i = start; i < seg.size(); i++) acc.add(seg.get(i));
    }

    private double haversineMeters(Point a, Point b) {
        final double R = 6371000;
        double dLat = Math.toRadians(b.lat() - a.lat());
        double dLng = Math.toRadians(b.lng() - a.lng());
        double s = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(a.lat())) * Math.cos(Math.toRadians(b.lat()))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * R * Math.asin(Math.min(1, Math.sqrt(s)));
    }

    private RouteResult toResult(KakaoRoute r) {
        List<double[]> path = new ArrayList<>();
        if (r.sections() != null) {
            for (KakaoSection s : r.sections()) {
                if (s.roads() == null) continue;
                for (KakaoRoad road : s.roads()) {
                    double[] v = road.vertexes();
                    if (v == null) continue;
                    // vertexes 는 [lng,lat,lng,lat,...] 평탄 배열 → [lat,lng] 쌍으로 변환
                    for (int i = 0; i + 1 < v.length; i += 2) {
                        path.add(new double[]{v[i + 1], v[i]});
                    }
                }
            }
        }
        KakaoSummary sum = r.summary();
        int dist = sum != null ? sum.distance() : 0;
        int dur = sum != null ? sum.duration() : 0;
        int taxi = (sum != null && sum.fare() != null) ? sum.fare().taxi() : 0;
        int toll = (sum != null && sum.fare() != null) ? sum.fare().toll() : 0;
        return new RouteResult(dist, dur, taxi, toll, path);
    }

    // ── 카카오 응답(역직렬화 전용) ──────────────────────────────────────────────
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoRoutesResponse(@JsonProperty("routes") List<KakaoRoute> routes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoRoute(
            @JsonProperty("result_code") int resultCode,
            @JsonProperty("result_msg") String resultMsg,
            @JsonProperty("summary") KakaoSummary summary,
            @JsonProperty("sections") List<KakaoSection> sections
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoSummary(
            @JsonProperty("distance") int distance,
            @JsonProperty("duration") int duration,
            @JsonProperty("fare") KakaoFare fare
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoFare(
            @JsonProperty("taxi") int taxi,
            @JsonProperty("toll") int toll
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoSection(@JsonProperty("roads") List<KakaoRoad> roads) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoRoad(@JsonProperty("vertexes") double[] vertexes) {}
}
