package com.ssafy.ai.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.ssafy.ai.service.WeatherService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class WeatherTool {

    private final WeatherService weatherService;

    @Tool(description = """
            지역명을 입력받아 현재 날씨를 조회한다.
            - 입력 예: '서울', '서울 날씨', '서울 종로구'
            - 모델은 사용자의 지역 표현을 그대로 location에 넣어 호출한다.
            - 서버가 내부에서 도시명(시/도 단위)을 정규화한다.
            """)
    public String getWeather(String location) {
        return weatherService.getWeatherByLocation(location);
    }
}
