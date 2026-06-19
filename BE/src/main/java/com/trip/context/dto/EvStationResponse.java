package com.trip.context.dto;

/**
 * 전기차 충전소 응답 (데모 데이터).
 *
 * @param name         충전소 이름
 * @param address      주소
 * @param lat          위도
 * @param lng          경도
 * @param chargerCount 충전기 대수
 * @param type         충전 타입 (예: "급속", "완속")
 * @param demo         데모 데이터 여부 (항상 true)
 */
public record EvStationResponse(
        String name,
        String address,
        double lat,
        double lng,
        int chargerCount,
        String type,
        boolean demo
) {}
