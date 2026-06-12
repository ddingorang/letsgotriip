// Created: 2026-06-08 15:32:16
package com.trip.festival.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourApiResponse(
        @JsonProperty("response") Response response
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(
            @JsonProperty("body") Body body
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            @JsonDeserialize(using = ItemsDeserializer.class)
            @JsonProperty("items")
            List<FestivalItem> items,

            @JsonProperty("totalCount") int totalCount
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FestivalItem(
            @JsonProperty("contentid")      String contentId,
            @JsonProperty("title")          String title,
            @JsonProperty("addr1")          String addr1,
            @JsonProperty("addr2")          String addr2,
            @JsonProperty("tel")            String tel,
            @JsonProperty("firstimage")     String firstimage,
            @JsonProperty("eventstartdate") String eventStartDate,
            @JsonProperty("eventenddate")   String eventEndDate,
            @JsonProperty("mapx")           String mapx,
            @JsonProperty("mapy")           String mapy,
            @JsonProperty("areacode")       String areaCode,
            @JsonProperty("sigungucode")    String sigunguCode,
            @JsonProperty("lDongRegnCd")    String lDongRegnCd,
            @JsonProperty("lDongSignguCd")  String lDongSignguCd
    ) {}

    /**
     * TourAPI 응답의 items 필드 파싱.
     * 결과 없으면 빈 문자열 "", 1건이면 단일 객체, N건이면 배열로 옴.
     */
    public static class ItemsDeserializer extends StdDeserializer<List<FestivalItem>> {

        public ItemsDeserializer() {
            super(List.class);
        }

        @Override
        public List<FestivalItem> deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            if (p.currentToken() == JsonToken.VALUE_STRING) {
                return List.of();
            }

            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            JsonNode root = mapper.readTree(p);
            JsonNode itemNode = root.get("item");

            if (itemNode == null || itemNode.isNull()) {
                return List.of();
            }

            if (itemNode.isArray()) {
                return mapper.convertValue(
                        itemNode,
                        mapper.getTypeFactory().constructCollectionType(List.class, FestivalItem.class)
                );
            }

            return List.of(mapper.convertValue(itemNode, FestivalItem.class));
        }
    }
}
