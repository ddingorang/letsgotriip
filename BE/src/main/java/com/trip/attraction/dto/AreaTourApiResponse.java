package com.trip.attraction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

/**
 * areaCode2 전용 응답 래퍼 — items 내 AreaItem 리스트
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AreaTourApiResponse(
        @JsonProperty("response") Response response
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(
            @JsonProperty("body") Body body
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            @JsonDeserialize(using = AttractionTourApiResponse.AreaItemsDeserializer.class)
            @JsonProperty("items")
            List<AttractionTourApiResponse.AreaItem> items,

            @JsonProperty("totalCount") int totalCount
    ) {}
}
