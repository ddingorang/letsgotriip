package com.trip.context.service;

import com.trip.context.dto.NewsItemResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 여행 뉴스 서비스 (데모 데이터).
 * 고정된 한국어 여행 뉴스 목록을 반환한다. 모두 demo=true.
 */
@Slf4j
@Service
public class NewsService {

    private static final String SOURCE = "여행뉴스";
    private static final String URL = "#";

    /**
     * 데모 한국어 여행 뉴스 목록(약 6건)을 반환한다.
     */
    public List<NewsItemResponse> travelNews() {
        String today = LocalDate.now().toString();
        return List.of(
                new NewsItemResponse(
                        "올여름 국내 인기 여행지 TOP 10 공개",
                        "제주, 강릉, 부산이 상위권을 차지했습니다. 가족 단위 여행객이 늘며 해변 명소가 주목받고 있습니다.",
                        SOURCE, URL, today, true),
                new NewsItemResponse(
                        "KTX 여름 성수기 임시열차 증편 운행",
                        "주요 노선에 임시열차가 추가 편성됩니다. 예매는 출발 한 달 전부터 가능합니다.",
                        SOURCE, URL, today, true),
                new NewsItemResponse(
                        "전기차로 떠나는 친환경 여행 코스 추천",
                        "고속도로 휴게소 급속충전 인프라가 확대되며 전기차 장거리 여행이 한층 편리해졌습니다.",
                        SOURCE, URL, today, true),
                new NewsItemResponse(
                        "지역 축제 캘린더, 6월 전국 축제 한눈에",
                        "이번 달에는 해안 도시를 중심으로 다채로운 축제가 열립니다. 야간 프로그램도 풍성합니다.",
                        SOURCE, URL, today, true),
                new NewsItemResponse(
                        "장마철 여행 꿀팁, 실내 명소 베스트",
                        "비 오는 날에도 즐길 수 있는 박물관과 카페 거리를 소개합니다. 우천 대비 일정 구성이 핵심입니다.",
                        SOURCE, URL, today, true),
                new NewsItemResponse(
                        "혼자 떠나는 여행, 안전 수칙 체크리스트",
                        "1인 여행객을 위한 숙소 선택과 야간 이동 시 주의사항을 정리했습니다.",
                        SOURCE, URL, today, true)
        );
    }
}
