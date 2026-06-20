package com.trip.attraction.client;

import com.trip.attraction.dto.AttractionTourApiResponse;
import com.trip.attraction.dto.AttractionTourApiResponse.AttractionItem;
import com.trip.attraction.dto.AreaTourApiResponse;
import com.trip.global.config.TourApiProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

/**
 * TourAPI KorService2 호출 클라이언트.
 * 외부 지연 시 스레드 고갈을 막기 위해 connect/read 타임아웃이 설정된
 * 자체 RestClient 인스턴스를 구성한다(공유 RestClient 빈은 타임아웃 미설정).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttractionTourApiClient {

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

    // ──────────────────────────────────────────────
    // areaBasedList2 — 지역 기반 관광정보 목록 조회
    // ──────────────────────────────────────────────
    public List<AttractionItem> fetchAreaBased(String areaCode, String sigunguCode,
                                               String contentTypeId, int pageNo, int numOfRows) {
        return fetchAreaBased(areaCode, sigunguCode, contentTypeId, pageNo, numOfRows, "A");
    }

    // arrange: A=제목순, B=조회순(인기), C=수정일순, Q=랜덤(KorService2)
    public List<AttractionItem> fetchAreaBased(String areaCode, String sigunguCode,
                                               String contentTypeId, int pageNo, int numOfRows,
                                               String arrange) {
        AttractionTourApiResponse resp = restClient.get()
                .uri(uriBuilder -> {
                    var b = uriBuilder
                            .path("/areaBasedList2")
                            .queryParam("serviceKey",    props.getKey())
                            .queryParam("numOfRows",     numOfRows)
                            .queryParam("pageNo",        pageNo)
                            .queryParam("MobileOS",      "ETC")
                            .queryParam("MobileApp",     "TripApp")
                            .queryParam("_type",         "json")
                            .queryParam("arrange",       arrange);
                    if (areaCode    != null && !areaCode.isBlank())    b = b.queryParam("areaCode",    areaCode);
                    if (sigunguCode != null && !sigunguCode.isBlank())  b = b.queryParam("sigunguCode",  sigunguCode);
                    if (contentTypeId != null && !contentTypeId.isBlank()) b = b.queryParam("contentTypeId", contentTypeId);
                    return b.build();
                })
                .retrieve()
                .body(AttractionTourApiResponse.class);

        return extractItems(resp, "areaBasedList2");
    }

    // ──────────────────────────────────────────────
    // searchKeyword2 — 키워드 검색
    // ──────────────────────────────────────────────
    public List<AttractionItem> fetchByKeyword(String keyword, String areaCode, String sigunguCode,
                                               String contentTypeId, int pageNo, int numOfRows) {
        AttractionTourApiResponse resp = restClient.get()
                .uri(uriBuilder -> {
                    var b = uriBuilder
                            .path("/searchKeyword2")
                            .queryParam("serviceKey",    props.getKey())
                            .queryParam("numOfRows",     numOfRows)
                            .queryParam("pageNo",        pageNo)
                            .queryParam("MobileOS",      "ETC")
                            .queryParam("MobileApp",     "TripApp")
                            .queryParam("_type",         "json")
                            .queryParam("keyword",       keyword);
                    if (areaCode    != null && !areaCode.isBlank())    b = b.queryParam("areaCode",    areaCode);
                    if (sigunguCode != null && !sigunguCode.isBlank())  b = b.queryParam("sigunguCode",  sigunguCode);
                    if (contentTypeId != null && !contentTypeId.isBlank()) b = b.queryParam("contentTypeId", contentTypeId);
                    return b.build();
                })
                .retrieve()
                .body(AttractionTourApiResponse.class);

        return extractItems(resp, "searchKeyword2");
    }

    // ──────────────────────────────────────────────
    // locationBasedList2 — 좌표(내 위치) 기반 목록, 거리순
    // ──────────────────────────────────────────────
    public List<AttractionItem> fetchLocationBased(String mapX, String mapY, int radius,
                                                   String contentTypeId, int pageNo, int numOfRows) {
        AttractionTourApiResponse resp = restClient.get()
                .uri(uriBuilder -> {
                    var b = uriBuilder
                            .path("/locationBasedList2")
                            .queryParam("serviceKey",    props.getKey())
                            .queryParam("numOfRows",     numOfRows)
                            .queryParam("pageNo",        pageNo)
                            .queryParam("MobileOS",      "ETC")
                            .queryParam("MobileApp",     "TripApp")
                            .queryParam("_type",         "json")
                            .queryParam("mapX",          mapX)
                            .queryParam("mapY",          mapY)
                            .queryParam("radius",        radius)
                            .queryParam("arrange",       "E");  // E = 거리순(이미지 포함)
                    if (contentTypeId != null && !contentTypeId.isBlank()) b = b.queryParam("contentTypeId", contentTypeId);
                    return b.build();
                })
                .retrieve()
                .body(AttractionTourApiResponse.class);

        return extractItems(resp, "locationBasedList2");
    }

    // ──────────────────────────────────────────────
    // detailCommon2 — 공통 정보 조회 (overview 포함)
    // ──────────────────────────────────────────────
    public AttractionItem fetchDetail(String contentId) {
        AttractionTourApiResponse resp = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/detailCommon2")
                        .queryParam("serviceKey",        props.getKey())
                        .queryParam("MobileOS",          "ETC")
                        .queryParam("MobileApp",         "TripApp")
                        .queryParam("_type",             "json")
                        // KorService2의 detailCommon2는 *YN 플래그(KorService1 잔재)를 받으면 빈 응답을 반환한다
                        .queryParam("contentId",         contentId)
                        .build())
                .retrieve()
                .body(AttractionTourApiResponse.class);

        List<AttractionItem> items = extractItems(resp, "detailCommon2");
        return items.isEmpty() ? null : items.get(0);
    }

    // ──────────────────────────────────────────────
    // areaCode2 — 지역코드 목록 조회
    // ──────────────────────────────────────────────
    public List<AttractionTourApiResponse.AreaItem> fetchAreaCodes(String areaCode) {
        AreaTourApiResponse resp = restClient.get()
                .uri(uriBuilder -> {
                    var b = uriBuilder
                            .path("/areaCode2")
                            .queryParam("serviceKey",    props.getKey())
                            .queryParam("numOfRows",     100)
                            .queryParam("pageNo",        1)
                            .queryParam("MobileOS",      "ETC")
                            .queryParam("MobileApp",     "TripApp")
                            .queryParam("_type",         "json");
                    if (areaCode != null && !areaCode.isBlank()) b = b.queryParam("areaCode", areaCode);
                    return b.build();
                })
                .retrieve()
                .body(AreaTourApiResponse.class);

        if (resp == null || resp.response() == null || resp.response().body() == null) {
            log.warn("TourAPI areaCode2 빈 응답");
            return List.of();
        }
        List<AttractionTourApiResponse.AreaItem> items = resp.response().body().items();
        return items != null ? items : List.of();
    }

    // ──────────────────────────────────────────────
    // 공통 items 추출 헬퍼
    // ──────────────────────────────────────────────
    private List<AttractionItem> extractItems(AttractionTourApiResponse resp, String endpoint) {
        if (resp == null || resp.response() == null || resp.response().body() == null) {
            log.warn("TourAPI {} 빈 응답", endpoint);
            return List.of();
        }
        List<AttractionItem> items = resp.response().body().items();
        return items != null ? items : List.of();
    }
}
