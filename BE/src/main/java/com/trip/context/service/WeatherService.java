package com.trip.context.service;

import com.trip.context.client.OpenMeteoClient;
import com.trip.context.dto.WeatherResponse;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 날씨 + 일출/일몰 서비스.
 * 좌표를 검증한 뒤 Open-Meteo 클라이언트에 위임한다. (무상태)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final OpenMeteoClient openMeteoClient;

    /**
     * 위/경도 기준 현재 날씨 및 3일 예보를 조회한다.
     *
     * @throws GeneralException 좌표 범위가 유효하지 않으면 _BAD_REQUEST
     */
    public WeatherResponse getWeather(double lat, double lng) {
        if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new GeneralException(ResponseCode._BAD_REQUEST);
        }
        return openMeteoClient.fetch(lat, lng);
    }
}
