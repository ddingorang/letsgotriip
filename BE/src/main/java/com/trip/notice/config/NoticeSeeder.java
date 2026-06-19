package com.trip.notice.config;

import com.trip.notice.entity.Notice;
import com.trip.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 공지 시드 — 테이블이 비어 있을 때만 기본 공지 3건 삽입.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class NoticeSeeder implements ApplicationRunner {

    private final NoticeRepository noticeRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (noticeRepository.count() > 0) return;

        noticeRepository.save(Notice.builder()
                .category("필독")
                .title("서비스 베타 오픈 안내")
                .content("관통 여행 서비스가 베타로 오픈했습니다. 이용 중 불편한 점은 언제든 알려 주세요. 감사합니다.")
                .pinned(true)
                .build());

        noticeRepository.save(Notice.builder()
                .category("안내")
                .title("카카오 지도 연동 안내")
                .content("탐색 화면의 지도가 카카오 지도로 전환되었습니다. 현재 위치 기반 주변 관광지 정렬을 지원해요.")
                .pinned(false)
                .build());

        noticeRepository.save(Notice.builder()
                .category("업데이트")
                .title("AI 일정 생성 기능이 개선되었어요")
                .content("이제 동행인·예산·테마를 입력하면 더 정교한 일정을 추천해 드려요. 마이페이지 > AI 여행에서 사용해 보세요.")
                .pinned(false)
                .build());

        log.info("[NoticeSeeder] 공지 시드 3건 삽입 완료");
    }
}
