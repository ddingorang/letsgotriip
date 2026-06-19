// Created: 2026-06-18 12:45:42
package com.trip.global.config;

import com.trip.community.dto.HotPlaceCreateRequest;
import com.trip.community.dto.PostCreateRequest;
import com.trip.community.entity.enums.HotPlaceCategory;
import com.trip.community.entity.enums.PostCategory;
import com.trip.community.service.CommunityService;
import com.trip.community.service.HotPlaceService;
import com.trip.companion.dto.CompanionPostCreateRequest;
import com.trip.companion.service.CompanionService;
import com.trip.user.entity.User;
import com.trip.user.entity.enums.UserRole;
import com.trip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 데모 데이터 시더 — 비어 있는 DB에 데모 계정/핫플/커뮤니티/동행 데이터를 1회 삽입한다.
 *
 * - app.seed.enabled=true 일 때만 빈으로 등록됨(@ConditionalOnProperty).
 * - idempotent: demo@triip.com 가 이미 있으면 전부 skip.
 * - 시드 실패가 앱 부팅을 막지 않도록 전체를 try/catch 로 감싼다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final HotPlaceService hotPlaceService;
    private final CommunityService communityService;
    private final CompanionService companionService;

    private static final String DEMO_EMAIL  = "demo@triip.com";
    private static final String ADMIN_EMAIL = "admin@triip.com";

    @Override
    public void run(ApplicationArguments args) {
        try {
            // idempotent — 데모 계정이 이미 있으면 시드 전체 skip
            if (userRepository.findByEmail(DEMO_EMAIL).isPresent()) {
                log.info("[DataSeeder] demo 계정이 이미 존재 — 시드 skip");
                return;
            }

            log.info("[DataSeeder] 데모 데이터 시드 시작");

            Long demoUserId = seedUsers();
            seedHotPlaces(demoUserId);
            seedCommunityPosts(demoUserId);
            seedCompanionPost(demoUserId);

            log.info("[DataSeeder] 데모 데이터 시드 완료");
        } catch (Exception e) {
            // 시드 실패는 부팅을 막지 않는다 — 경고만 남기고 계속
            log.warn("[DataSeeder] 데모 데이터 시드 실패 — 무시하고 부팅 계속: {}", e.getMessage(), e);
        }
    }

    /**
     * 데모/관리자 계정 생성 후 데모 user id 반환.
     * IDENTITY 전략이라 save() 시점에 INSERT 가 실행되어 id 가 즉시 채워진다.
     */
    private Long seedUsers() {
        User demo = userRepository.save(User.builder()
                .nickname("데모여행자")
                .email(DEMO_EMAIL)
                .password(passwordEncoder.encode("demo1234"))
                .userRole(UserRole.USER)
                .profileImageUrl(User.DEFAULT_PROFILE_IMAGE_URL)
                .status(true)
                .build());

        userRepository.save(User.builder()
                .nickname("관리자")
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode("admin1234"))
                .userRole(UserRole.ADMIN)
                .profileImageUrl(User.DEFAULT_PROFILE_IMAGE_URL)
                .status(true)
                .build());

        log.info("[DataSeeder] 사용자 시드 완료 (demo id={})", demo.getId());
        return demo.getId();
    }

    /** 제주 핫플 4건 (HotPlaceService.register → 자동 APPROVED). 실 TourAPI 사진 URL 부여. */
    private void seedHotPlaces(Long userId) {
        register(userId, "성산일출봉", "제주특별자치도 서귀포시 성산읍 일출로 284-12",
                33.4581, 126.9425, HotPlaceCategory.NATURE,
                "유네스코 세계자연유산. 일출 명소로 유명한 제주의 대표 관광지.",
                "https://tong.visitkorea.or.kr/cms/resource/08/2871008_image2_1.JPG");

        register(userId, "협재 해수욕장", "제주특별자치도 제주시 한림읍 협재리",
                33.3940, 126.2396, HotPlaceCategory.NATURE,
                "에메랄드빛 바다와 비양도 전망이 일품인 해변.",
                "https://tong.visitkorea.or.kr/cms/resource/53/2869753_image2_1.jpg");

        register(userId, "오설록 티 뮤지엄", "제주특별자치도 서귀포시 안덕면 신화역사로 15",
                33.3056, 126.2895, HotPlaceCategory.CAFE,
                "녹차밭이 펼쳐진 카페 겸 박물관. 녹차 아이스크림이 인기.",
                "https://tong.visitkorea.or.kr/cms/resource/37/3568037_image2_1.jpg");

        register(userId, "흑돼지 거리", "제주특별자치도 제주시 건입동",
                33.5145, 126.5270, HotPlaceCategory.RESTAURANT,
                "제주 흑돼지 맛집이 모여있는 거리. 연탄구이가 별미.",
                "https://tong.visitkorea.or.kr/cms/resource/56/3467156_image2_1.jpg");

        log.info("[DataSeeder] 핫플 시드 완료 (4건)");
    }

    private void register(Long userId, String name, String address,
                          double lat, double lng, HotPlaceCategory category, String desc, String imageUrl) {
        hotPlaceService.register(userId, new HotPlaceCreateRequest(
                name, address, lat, lng, category, desc, List.of(imageUrl)));
    }

    /** 커뮤니티 게시글 2건 (작성자 = demo user) */
    private void seedCommunityPosts(Long userId) {
        communityService.createPost(userId, new PostCreateRequest(
                "제주 2박 3일 뚜벅이 여행 후기 🌊",
                "성산일출봉에서 일출 보고, 협재 해변 걷고, 오설록에서 녹차 아이스크림까지! "
                        + "렌터카 없이 버스로도 충분히 다닐 수 있었어요. 추천 코스 공유합니다.",
                PostCategory.REVIEW,
                List.of("https://tong.visitkorea.or.kr/cms/resource/34/3567934_image2_1.jpg")));

        communityService.createPost(userId, new PostCreateRequest(
                "제주 흑돼지 맛집 추천해주세요!",
                "이번 주말에 제주 가는데 흑돼지 거리 어디가 제일 맛있나요? "
                        + "연탄구이 잘하는 곳 아시는 분 댓글 부탁드려요 🙏",
                PostCategory.QUESTION,
                List.of("https://tong.visitkorea.or.kr/cms/resource/73/3553073_image2_1.png")));

        log.info("[DataSeeder] 커뮤니티 시드 완료 (2건)");
    }

    /** 동행 모집글 1건 (CompanionService.createPost 내부에서 ChatRoom 생성됨) */
    private void seedCompanionPost(Long userId) {
        companionService.createPost(userId, new CompanionPostCreateRequest(
                "제주 동쪽 해안 드라이브 함께 하실 분 🚗",
                LocalDate.now().plusDays(14),
                "제주",
                "2박 3일",
                4,
                300000,
                "성산일출봉, 우도, 섭지코지를 도는 일정이에요. 사진 찍는 거 좋아하시는 분 환영합니다!",
                List.of("드라이브", "사진", "힐링")));

        log.info("[DataSeeder] 동행 시드 완료 (1건)");
    }
}
