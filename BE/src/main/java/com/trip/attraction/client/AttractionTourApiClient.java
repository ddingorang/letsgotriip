package com.trip.attraction.client;

import com.trip.attraction.dto.AttractionTourApiResponse;
import com.trip.attraction.dto.AttractionTourApiResponse.AttractionItem;
import com.trip.attraction.dto.AreaTourApiResponse;
import com.trip.global.config.TourApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * TourAPI KorService2 호출 클라이언트.
 * 기존 TourApiProperties / RestClient 빈을 재사용.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttractionTourApiClient {

    private final TourApiProperties props;
    private final RestClient restClient;

    // ──────────────────────────────────────────────
    // areaBasedList2 — 지역 기반 관광정보 목록 조회
    // ──────────────────────────────────────────────
    public List<AttractionItem> fetchAreaBased(String areaCode, String sigunguCode,
                                               String contentTypeId, int pageNo, int numOfRows) {
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
                            .queryParam("arrange",       "A");  // 제목순
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
