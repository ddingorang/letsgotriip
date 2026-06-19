package com.trip.community.config;

import com.trip.community.entity.HotPlace;
import com.trip.community.entity.enums.HotPlaceCategory;
import com.trip.community.entity.enums.HotPlaceStatus;
import com.trip.community.repository.HotPlaceRepository;
import com.trip.user.entity.User;
import com.trip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 핫플 시드 — 승인(APPROVED)된 핫플이 하나도 없을 때만 제주 핫플 4건을 시드한다.
 * 커뮤니티 핫플 탭이 빈 화면으로 보이지 않도록 함. (DataSeeder의 시드 유저에 귀속)
 */
@Slf4j
@Component
@Order(30)
@RequiredArgsConstructor
public class HotplaceSeeder implements ApplicationRunner {

    private static final String SEED_USER_EMAIL = "kim@seed.local";

    private final HotPlaceRepository hotPlaceRepository;
    private final UserRepository userRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (!hotPlaceRepository.findAllByStatus(HotPlaceStatus.APPROVED).isEmpty()) return;

        User submitter = userRepository.findByEmail(SEED_USER_EMAIL).orElse(null);
        if (submitter == null) {
            log.warn("[HotplaceSeeder] 시드 유저({}) 없음 — 핫플 시드 생략", SEED_USER_EMAIL);
            return;
        }

        List<HotPlace> seeds = List.of(
                hotplace(submitter, "성산일출봉 전망 카페", "제주특별자치도 서귀포시 성산읍 일출로",
                        33.4581, 126.9425, HotPlaceCategory.CAFE, "성산일출봉이 통창으로 보이는 오션뷰 카페예요."),
                hotplace(submitter, "협재 해수욕장", "제주특별자치도 제주시 한림읍 협재리",
                        33.3942, 126.2394, HotPlaceCategory.NATURE, "에메랄드빛 바다와 비양도 뷰가 일품인 해변."),
                hotplace(submitter, "애월 카페거리", "제주특별자치도 제주시 애월읍",
                        33.4607, 126.3227, HotPlaceCategory.CAFE, "노을 맛집 카페가 모여있는 애월 해안도로."),
                hotplace(submitter, "흑돼지 거리 맛집", "제주특별자치도 제주시 건입동",
                        33.5141, 126.5297, HotPlaceCategory.RESTAURANT, "현지인이 추천하는 제주 흑돼지 노포.")
        );
        hotPlaceRepository.saveAll(seeds);
        log.info("[HotplaceSeeder] 승인 핫플 {}건 시드 완료", seeds.size());
    }

    private HotPlace hotplace(User submitter, String name, String address,
                              double lat, double lng, HotPlaceCategory category, String description) {
        return HotPlace.builder()
                .submitter(submitter)
                .name(name)
                .address(address)
                .latitude(lat)
                .longitude(lng)
                .category(category)
                .description(description)
                .status(HotPlaceStatus.APPROVED)
                .build();
    }
}
