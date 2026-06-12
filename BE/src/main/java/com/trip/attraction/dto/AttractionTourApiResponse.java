package com.trip.attraction.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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

/**
 * TourAPI KorService2 공통 응답 래퍼.
 * items.item 이 배열/단일객체/빈문자열인 세 가지 경우를 모두 처리.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AttractionTourApiResponse(
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
            List<AttractionItem> items,

            @JsonProperty("totalCount") int totalCount
    ) {}

    /**
     * areaBasedList2 / searchKeyword2 공통 아이템 필드.
     * @JsonAlias는 역직렬화(TourAPI 소문자 키)에만 적용 — 직렬화(FE 응답)는 camelCase 컴포넌트명 사용.
     * @JsonProperty를 쓰면 응답 JSON까지 소문자가 되어 FE 계약(contentId)이 깨진다.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AttractionItem(
            @JsonAlias("contentid")     String contentId,
            @JsonAlias("contenttypeid") String contentTypeId,
            String title,
            String addr1,
            @JsonAlias("areacode")      String areaCode,
            @JsonAlias("sigungucode")   String sigunguCode,
            String mapx,   // 경도
            String mapy,   // 위도
            String firstimage,
            String tel,
            String overview
    ) {}

    /** areaCode2 지역코드 아이템 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AreaItem(
            @JsonProperty("code")  String code,
            @JsonProperty("name")  String name,
            @JsonProperty("rnum")  String rnum
    ) {}

    /**
     * TourAPI 응답의 items 필드 파싱.
     * 결과 없으면 빈 문자열 "", 1건이면 단일 객체, N건이면 배열로 옴.
     */
    public static class ItemsDeserializer extends StdDeserializer<List<AttractionItem>> {

        public ItemsDeserializer() {
            super(List.class);
        }

        @Override
        public List<AttractionItem> deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            // 결과 없을 때 빈 문자열로 옴
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
                        mapper.getTypeFactory().constructCollectionType(List.class, AttractionItem.class)
                );
            }

            // 단일 객체인 경우
            return List.of(mapper.convertValue(itemNode, AttractionItem.class));
        }
    }

    /**
     * areaCode2 전용 역직렬화기 — AreaItem 리스트로 파싱
     */
    public static class AreaItemsDeserializer extends StdDeserializer<List<AreaItem>> {

        public AreaItemsDeserializer() {
            super(List.class);
        }

        @Override
        public List<AreaItem> deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
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
                        mapper.getTypeFactory().constructCollectionType(List.class, AreaItem.class)
                );
            }

            return List.of(mapper.convertValue(itemNode, AreaItem.class));
        }
    }
}
