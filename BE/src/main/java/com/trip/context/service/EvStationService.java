package com.trip.context.service;

import com.trip.context.dto.EvStationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 전기차 충전소 서비스 (데모 데이터).
 * 주어진 좌표 주변에 고정 오프셋의 데모 충전소를 deterministic 하게 생성한다.
 */
@Slf4j
@Service
public class EvStationService {

    /** 기준 좌표로부터의 고정 오프셋(위도, 경도) — deterministic 데모 5곳. */
    private static final double[][] OFFSETS = {
            { 0.0040,  0.0035},
            {-0.0055,  0.0060},
            { 0.0072, -0.0048},
            {-0.0030, -0.0070},
            { 0.0015,  0.0090},
    };

    private static final String[] NAMES = {
            "그린충전 스테이션",
            "시청 앞 공영주차장 충전소",
            "휴게소 급속충전소",
            "에코파크 충전허브",
            "중앙로 완속충전소",
    };

    private static final String[] TYPES = {"급속", "완속", "급속", "급속", "완속"};

    private static final int[] CHARGER_COUNTS = {4, 2, 6, 3, 2};

    /**
     * 주어진 좌표 주변의 데모 충전소 목록(약 5곳)을 반환한다. 모두 demo=true.
     */
    public List<EvStationResponse> nearby(double lat, double lng) {
        List<EvStationResponse> result = new java.util.ArrayList<>(OFFSETS.length);
        for (int i = 0; i < OFFSETS.length; i++) {
            double stationLat = lat + OFFSETS[i][0];
            double stationLng = lng + OFFSETS[i][1];
            result.add(new EvStationResponse(
                    NAMES[i],
                    String.format("위도 %.4f, 경도 %.4f 부근", stationLat, stationLng),
                    stationLat,
                    stationLng,
                    CHARGER_COUNTS[i],
                    TYPES[i],
                    true
            ));
        }
        return result;
    }
}
