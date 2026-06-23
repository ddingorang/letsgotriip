package com.trip.attraction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

/** detailImage2 응답 래퍼 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AttractionImageResponse(
        @JsonProperty("response") Response response
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(
            @JsonProperty("body") Body body
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            @JsonDeserialize(using = AttractionTourApiResponse.ImageItemsDeserializer.class)
            @JsonProperty("items")
            List<AttractionTourApiResponse.ImageItem> items
    ) {}
}
