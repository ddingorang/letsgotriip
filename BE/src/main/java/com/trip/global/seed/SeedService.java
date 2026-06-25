package com.trip.global.seed;

import com.trip.attraction.entity.Attraction;
import com.trip.attraction.repository.AttractionRepository;
import com.trip.chat.entity.ChatRoom;
import com.trip.chat.repository.ChatRoomMembershipRepository;
import com.trip.chat.repository.ChatRoomRepository;
import com.trip.checklist.dto.ChecklistItemCreateRequest;
import com.trip.checklist.repository.ChecklistItemRepository;
import com.trip.checklist.service.ChecklistService;
import com.trip.community.dto.HotPlaceCreateRequest;
import com.trip.community.dto.PostCreateRequest;
import com.trip.community.dto.CommentCreateRequest;
import com.trip.community.entity.HotPlace;
import com.trip.community.entity.Post;
import com.trip.community.entity.enums.HotPlaceCategory;
import com.trip.community.entity.enums.PostCategory;
import com.trip.community.repository.CommentLikeRepository;
import com.trip.community.repository.CommentRepository;
import com.trip.community.repository.HotPlacePhotoRepository;
import com.trip.community.repository.HotPlaceRepository;
import com.trip.community.repository.PostImageRepository;
import com.trip.community.repository.PostLikeRepository;
import com.trip.community.repository.PostRepository;
import com.trip.community.service.CommunityService;
import com.trip.community.service.HotPlaceService;
import com.trip.companion.dto.CompanionPostCreateRequest;
import com.trip.companion.entity.CompanionPost;
import com.trip.companion.repository.CompanionApplicationRepository;
import com.trip.companion.repository.CompanionPostRepository;
import com.trip.companion.service.CompanionService;
import com.trip.favorite.entity.FavoriteTargetType;
import com.trip.favorite.repository.FavoriteRepository;
import com.trip.favorite.service.FavoriteService;
import com.trip.follow.repository.FollowRepository;
import com.trip.follow.service.FollowService;
import com.trip.plan.entity.CompanionsType;
import com.trip.plan.entity.OriginType;
import com.trip.plan.entity.TripDay;
import com.trip.plan.entity.TripPlace;
import com.trip.plan.entity.TripPlan;
import com.trip.plan.repository.PlanRepository;
import com.trip.review.dto.ReviewCreateRequest;
import com.trip.review.repository.AttractionReviewRepository;
import com.trip.review.service.ReviewService;
import com.trip.story.dto.TravelStoryCreateRequest;
import com.trip.story.repository.TravelStoryRepository;
import com.trip.story.service.TravelStoryService;
import com.trip.user.entity.User;
import com.trip.user.entity.enums.UserRole;
import com.trip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 데모/테스트 데이터 시더 — 스크립트가 호출하는 /api/dev/seed 엔드포인트에서 사용.
 *
 * <p>특징
 * <ul>
 *   <li>profile: {@code demo}(스크린샷용 풍부한 데이터) / {@code test}(엣지케이스 포함 결정적 픽스처)</li>
 *   <li>reset=true: 마커 사용자(@seed.triip) 소유 데이터를 먼저 정리 후 재삽입 → 반복 테스트 가능</li>
 *   <li>reset=false: 이미 시드되어 있으면(마커 사용자 존재) skip → 멱등</li>
 *   <li>TourAPI 의존 금지: Attraction 스냅샷과 plan 엔티티 그래프는 리포지토리로 직접 구성</li>
 * </ul>
 *
 * <p>오프라인 안전한 도메인 서비스(찜/리뷰/스토리/팔로우/체크리스트/커뮤니티/동행)는 재사용한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeedService {

    /** 시드 마커 — 이 도메인(@seed.triip)으로 끝나는 계정 소유 데이터만 reset 대상. */
    public static final String SEED_EMAIL_SUFFIX = "@seed.triip";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final AttractionRepository attractionRepository;
    private final PlanRepository planRepository;

    private final FavoriteService favoriteService;
    private final ReviewService reviewService;
    private final TravelStoryService travelStoryService;
    private final FollowService followService;
    private final ChecklistService checklistService;
    private final CommunityService communityService;
    private final HotPlaceService hotPlaceService;
    private final CompanionService companionService;

    // ── reset 정리용 리포지토리 ──────────────────────────────────────────────
    private final FavoriteRepository favoriteRepository;
    private final AttractionReviewRepository reviewRepository;
    private final TravelStoryRepository travelStoryRepository;
    private final FollowRepository followRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final HotPlaceRepository hotPlaceRepository;
    private final HotPlacePhotoRepository hotPlacePhotoRepository;
    private final com.trip.community.repository.HotPlaceLikeRepository hotPlaceLikeRepository;
    private final CompanionPostRepository companionPostRepository;
    private final CompanionApplicationRepository companionApplicationRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMembershipRepository chatRoomMembershipRepository;

    /**
     * 시드 실행 진입점.
     *
     * @param profile demo | test
     * @param reset   true 면 기존 시드 데이터 정리 후 재삽입, false 면 멱등(이미 있으면 skip)
     * @return 엔티티별 삽입 건수 맵
     */
    @Transactional
    public Map<String, Integer> seed(String profile, boolean reset) {
        final String prof = (profile == null || profile.isBlank()) ? "demo" : profile.trim().toLowerCase();
        if (!prof.equals("demo") && !prof.equals("test")) {
            throw new IllegalArgumentException("지원하지 않는 profile: " + profile + " (demo|test)");
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("profile_demo", prof.equals("demo") ? 1 : 0);
        counts.put("profile_test", prof.equals("test") ? 1 : 0);

        if (reset) {
            resetSeedData();
            counts.put("reset", 1);
        } else if (!userRepository.findByEmailEndingWith(SEED_EMAIL_SUFFIX).isEmpty()) {
            // 멱등: 이미 시드되어 있으면 아무것도 하지 않는다.
            log.info("[SeedService] 시드 데이터가 이미 존재 — reset=false 이므로 skip");
            counts.put("skipped_already_seeded", 1);
            return counts;
        }

        if (prof.equals("demo")) {
            seedDemo(counts);
        } else {
            seedTest(counts);
        }
        return counts;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  RESET — @seed.triip 소유 데이터만 FK 의존 순서로 정리
    // ════════════════════════════════════════════════════════════════════════
    private void resetSeedData() {
        List<Long> seedUserIds = userRepository.findByEmailEndingWith(SEED_EMAIL_SUFFIX).stream()
                .map(User::getId)
                .toList();
        if (seedUserIds.isEmpty()) {
            log.info("[SeedService] reset: 마커 사용자 없음 — 정리할 데이터 없음");
            return;
        }
        log.info("[SeedService] reset 시작 — 마커 사용자 {}명 소유 데이터 정리", seedUserIds.size());

        // 1) 단순 user_id 컬럼 보유 엔티티
        favoriteRepository.deleteByUserIdIn(seedUserIds);
        reviewRepository.deleteByUserIdIn(seedUserIds);
        followRepository.deleteByFollowerIdInOrFolloweeIdIn(seedUserIds);
        travelStoryRepository.deleteAll(
                seedUserIds.stream().flatMap(id -> travelStoryRepository.findByUserIdOrderByCreatedAtDesc(id).stream()).toList());
        checklistItemRepository.deleteAll(
                seedUserIds.stream().flatMap(id -> checklistItemRepository.findByUserIdOrderBySortOrderAscIdAsc(id).stream()).toList());

        // 2) 계획 — days/places 는 cascade(ALL)+orphanRemoval 로 함께 삭제
        planRepository.deleteAll(planRepository.findByUserIdIn(seedUserIds));

        // 3) 커뮤니티 게시글 — 자식(이미지/좋아요/댓글/댓글좋아요) 먼저 정리
        List<Post> posts = postRepository.findAllByAuthor_IdIn(seedUserIds);
        if (!posts.isEmpty()) {
            List<Long> postIds = posts.stream().map(Post::getId).toList();
            List<Long> commentIds = commentRepository.findIdsByPostIdIn(postIds);
            if (!commentIds.isEmpty()) commentLikeRepository.deleteByCommentIdIn(commentIds);
            commentRepository.deleteByPostIdIn(postIds);
            postLikeRepository.deleteByPostIdIn(postIds);
            postIds.forEach(postImageRepository::deleteAllByPostId);
            postRepository.deleteAll(posts);
        }

        // 4) 핫플 — 사진 먼저 정리
        List<HotPlace> hotPlaces = hotPlaceRepository.findAllBySubmitter_IdIn(seedUserIds);
        if (!hotPlaces.isEmpty()) {
            hotPlaceLikeRepository.deleteByHotPlaceIdIn(hotPlaces.stream().map(HotPlace::getId).toList());
            hotPlaces.forEach(hp -> hotPlacePhotoRepository.deleteAllByHotPlaceId(hp.getId()));
            hotPlaceRepository.deleteAll(hotPlaces);
        }

        // 5) 동행글 — 신청/채팅방 멤버십/게시글/채팅방 순으로 정리
        List<CompanionPost> companionPosts = companionPostRepository.findAllByAuthor_IdIn(seedUserIds);
        if (!companionPosts.isEmpty()) {
            List<Long> compIds = companionPosts.stream().map(CompanionPost::getId).toList();
            companionApplicationRepository.deleteByCompanionPostIdIn(compIds);
            List<ChatRoom> chatRooms = companionPosts.stream()
                    .map(CompanionPost::getChatRoom)
                    .filter(cr -> cr != null)
                    .toList();
            List<Long> chatRoomIds = chatRooms.stream().map(ChatRoom::getId).toList();
            if (!chatRoomIds.isEmpty()) chatRoomMembershipRepository.deleteByChatRoomIdIn(chatRoomIds);
            companionPostRepository.deleteAll(companionPosts);  // FK chat_room_id 해제
            if (!chatRooms.isEmpty()) chatRoomRepository.deleteAll(chatRooms);
        }

        // 6) 마커 사용자 본인 — 마지막에 삭제
        userRepository.deleteAllById(seedUserIds);

        // 큐잉된 엔티티 삭제(users/posts 등)를 즉시 DB에 반영한다. 같은 트랜잭션에서 곧바로
        // 재삽입(insert)이 일어나는데, Hibernate 기본 액션 순서는 insert→delete 라서 flush 없이는
        // 재삽입이 삭제보다 먼저 실행돼 unique(email) 충돌이 난다(reset 후 재시드 시 500).
        userRepository.flush();
        log.info("[SeedService] reset 완료");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DEMO 프로필 — 스크린샷 친화적 풍부한 데이터
    // ════════════════════════════════════════════════════════════════════════
    private void seedDemo(Map<String, Integer> counts) {
        // ── 사용자 (팔로우/좋아요/저장 등 참여 데이터용 여러 명) ──────────────
        User jeju  = saveUser("제주러버", "jeju");
        User busan = saveUser("부산갈매기", "busan");
        User seoul = saveUser("서울나들이", "seoul");
        User foodie = saveUser("맛집헌터", "foodie");
        User gangneung = saveUser("강릉바다", "gangneung");
        User yeosu = saveUser("여수밤바다", "yeosu");
        User gyeongju = saveUser("경주역사가", "gyeongju");
        User sokcho = saveUser("속초여행러", "sokcho");
        // 핫플 좋아요/저장 참여에 두루 쓸 사용자 풀
        java.util.List<User> everyone = java.util.List.of(jeju, busan, seoul, foodie, gangneung, yeosu, gyeongju, sokcho);
        inc(counts, "users", 8);

        // ── 관광지 스냅샷(직접 insert — TourAPI 미사용) ─────────────────
        Attraction gyeongbok = saveAttraction("126508", 12, "경복궁",
                "서울특별시 종로구 사직로 161", "1", 37.5796, 126.9770,
                "https://tong.visitkorea.or.kr/cms/resource/23/2678623_image2_1.jpg",
                "조선 왕조의 법궁. 광화문과 근정전이 대표적.");
        Attraction nseoul = saveAttraction("126461", 12, "N서울타워",
                "서울특별시 용산구 남산공원길 105", "1", 37.5512, 126.9882,
                "https://tong.visitkorea.or.kr/cms/resource/96/2785596_image2_1.jpg",
                "남산 정상의 전망 명소. 서울 야경 1번지.");
        Attraction seongsan = saveAttraction("126508001", 12, "성산일출봉",
                "제주특별자치도 서귀포시 성산읍 일출로 284-12", "39", 33.4581, 126.9425,
                "https://tong.visitkorea.or.kr/cms/resource/08/2871008_image2_1.JPG",
                "유네스코 세계자연유산. 일출 명소.");
        Attraction hyeopjae = saveAttraction("126508002", 12, "협재해수욕장",
                "제주특별자치도 제주시 한림읍 협재리", "39", 33.3940, 126.2396,
                "https://tong.visitkorea.or.kr/cms/resource/53/2869753_image2_1.jpg",
                "에메랄드빛 바다와 비양도 전망.");
        Attraction haeundae = saveAttraction("126508003", 12, "해운대해수욕장",
                "부산광역시 해운대구 우동", "6", 35.1587, 129.1604,
                "https://tong.visitkorea.or.kr/cms/resource/41/2655741_image2_1.jpg",
                "부산 대표 해변. 백사장과 마린시티 야경.");
        inc(counts, "attractions", 5);

        // ── 여행 계획(엔티티 그래프 직접 구성) ───────────────────────────
        TripPlan seoulPlan = buildPlan(seoul.getId(), "서울 2박 3일 고궁 투어",
                LocalDate.now().plusDays(7), LocalDate.now().plusDays(9),
                CompanionsType.COUPLE, 350000,
                "https://tong.visitkorea.or.kr/cms/resource/23/2678623_image2_1.jpg");
        addDay(seoulPlan, 1, "도심 고궁",
                place(gyeongbok, 1, LocalTime.of(10, 0), "한복 입고 입장"),
                place(nseoul, 2, LocalTime.of(18, 0), "야경 명소"));
        savePlanGraph(seoulPlan);

        TripPlan jejuPlan = buildPlan(jeju.getId(), "제주 힐링 여행",
                LocalDate.now().plusDays(20), LocalDate.now().plusDays(22),
                CompanionsType.FRIENDS, 500000,
                "https://tong.visitkorea.or.kr/cms/resource/08/2871008_image2_1.JPG");
        addDay(jejuPlan, 1, "동쪽 코스",
                place(seongsan, 1, LocalTime.of(7, 0), "일출 보기"));
        addDay(jejuPlan, 2, "서쪽 코스",
                place(hyeopjae, 1, LocalTime.of(11, 0), "물놀이"));
        savePlanGraph(jejuPlan);

        TripPlan busanPlan = buildPlan(busan.getId(), "부산 바다 1박 2일",
                LocalDate.now().plusDays(30), LocalDate.now().plusDays(31),
                CompanionsType.SOLO, 200000, null);
        addDay(busanPlan, 1, "해운대",
                place(haeundae, 1, LocalTime.of(15, 0), "해변 산책"));
        savePlanGraph(busanPlan);
        inc(counts, "plans", 3);
        inc(counts, "tripDays", 4);
        inc(counts, "tripPlaces", 5);

        // ── 찜(즐겨찾기) — toggle 은 오프라인 안전 ───────────────────────
        safe(counts, "favorites", () -> {
            favoriteService.toggle(jeju.getId(), FavoriteTargetType.ATTRACTION, seongsan.getContentId(), seongsan.getTitle());
            favoriteService.toggle(jeju.getId(), FavoriteTargetType.ATTRACTION, hyeopjae.getContentId(), hyeopjae.getTitle());
            favoriteService.toggle(seoul.getId(), FavoriteTargetType.ATTRACTION, gyeongbok.getContentId(), gyeongbok.getTitle());
            favoriteService.toggle(busan.getId(), FavoriteTargetType.ATTRACTION, haeundae.getContentId(), haeundae.getTitle());
            favoriteService.toggle(foodie.getId(), FavoriteTargetType.ATTRACTION, hyeopjae.getContentId(), hyeopjae.getTitle());
            favoriteService.toggle(gangneung.getId(), FavoriteTargetType.ATTRACTION, nseoul.getContentId(), nseoul.getTitle());
            favoriteService.toggle(yeosu.getId(), FavoriteTargetType.ATTRACTION, haeundae.getContentId(), haeundae.getTitle());
            favoriteService.toggle(gyeongju.getId(), FavoriteTargetType.ATTRACTION, gyeongbok.getContentId(), gyeongbok.getTitle());
            favoriteService.toggle(sokcho.getId(), FavoriteTargetType.ATTRACTION, seongsan.getContentId(), seongsan.getTitle());
            return 9;
        });

        // ── 리뷰 ────────────────────────────────────────────────────────
        safe(counts, "reviews", () -> {
            reviewService.create(jeju.getId(), seongsan.getContentId(), new ReviewCreateRequest(5, "일출이 정말 장관이었어요! 새벽에 올라간 보람이 있네요."));
            reviewService.create(seoul.getId(), gyeongbok.getContentId(), new ReviewCreateRequest(4, "한복 입고 입장하면 무료라 더 좋았어요."));
            reviewService.create(busan.getId(), haeundae.getContentId(), new ReviewCreateRequest(5, "백사장이 넓고 야경이 환상적입니다."));
            reviewService.create(foodie.getId(), hyeopjae.getContentId(), new ReviewCreateRequest(4, "물이 맑고 가족 단위로 오기 좋아요."));
            reviewService.create(gangneung.getId(), nseoul.getContentId(), new ReviewCreateRequest(5, "야경 보러 갔는데 케이블카도 재밌어요."));
            reviewService.create(yeosu.getId(), haeundae.getContentId(), new ReviewCreateRequest(4, "여름엔 사람 많지만 그만한 이유가 있네요."));
            reviewService.create(gyeongju.getId(), gyeongbok.getContentId(), new ReviewCreateRequest(5, "고즈넉한 분위기가 정말 좋았습니다."));
            reviewService.create(sokcho.getId(), seongsan.getContentId(), new ReviewCreateRequest(4, "경치가 멋지고 둘레길도 잘 돼 있어요."));
            return 8;
        });

        // ── 여행 스토리 ──────────────────────────────────────────────────
        safe(counts, "stories", () -> {
            travelStoryService.create(jeju.getId(), new TravelStoryCreateRequest(
                    "제주 힐링 다녀온 후기", jejuPlan.getId(),
                    "오랜만의 여행이라 설렘 반 걱정 반이었어요.",
                    "성산일출봉 일출이 최고였고, 협재 바다도 잊지 못할 거예요!", 5,
                    "https://tong.visitkorea.or.kr/cms/resource/08/2871008_image2_1.JPG"));
            travelStoryService.create(seoul.getId(), new TravelStoryCreateRequest(
                    "고궁 데이트 기록", seoulPlan.getId(),
                    "고궁 투어는 처음이라 기대됐어요.",
                    "경복궁 야경과 N서울타워 전망이 완벽했습니다.", 4, null));
            travelStoryService.create(busan.getId(), new TravelStoryCreateRequest(
                    "부산 바다 혼행 기록", busanPlan.getId(),
                    "혼자 훌쩍 떠난 부산이었어요.",
                    "해운대 야경에 힐링하고 왔습니다.", 5,
                    "https://tong.visitkorea.or.kr/cms/resource/41/2655741_image2_1.jpg"));
            travelStoryService.create(foodie.getId(), new TravelStoryCreateRequest(
                    "전국 맛집 탐방기", null,
                    "먹으러 떠난 여행이었죠.",
                    "광장시장 빈대떡이 최고였어요!", 4, null));
            return 4;
        });

        // ── 팔로우 그래프 ─────────────────────────────────────────────────
        safe(counts, "follows", () -> {
            followService.toggle(jeju.getId(), busan.getId());
            followService.toggle(jeju.getId(), seoul.getId());
            followService.toggle(busan.getId(), jeju.getId());
            followService.toggle(seoul.getId(), foodie.getId());
            followService.toggle(foodie.getId(), jeju.getId());
            followService.toggle(gangneung.getId(), jeju.getId());
            followService.toggle(yeosu.getId(), busan.getId());
            followService.toggle(gyeongju.getId(), seoul.getId());
            followService.toggle(sokcho.getId(), foodie.getId());
            followService.toggle(jeju.getId(), gangneung.getId());
            return 10;
        });

        // ── 체크리스트 ────────────────────────────────────────────────────
        safe(counts, "checklists", () -> {
            checklistService.create(jeju.getId(), new ChecklistItemCreateRequest("여권/신분증", "준비물", jejuPlan.getId(), null, 1));
            checklistService.create(jeju.getId(), new ChecklistItemCreateRequest("선크림", "준비물", jejuPlan.getId(), 2, 2));
            checklistService.create(jeju.getId(), new ChecklistItemCreateRequest("렌터카 예약 확인", "예약", jejuPlan.getId(), 1, 3));
            checklistService.create(seoul.getId(), new ChecklistItemCreateRequest("한복 예약", "예약", seoulPlan.getId(), 1, 1));
            return 4;
        });

        // ── 커뮤니티 게시글 + 참여(댓글·좋아요·스크랩)로 실제 데이터처럼 ─────────────
        safe(counts, "communityPosts", () -> {
            var p1 = communityService.createPost(foodie.getId(), new PostCreateRequest(
                    "제주 흑돼지 맛집 BEST 5 🐷",
                    "현지인 추천 받아 다녀온 흑돼지 맛집들 정리해봤어요. 연탄구이는 진리!",
                    PostCategory.RESTAURANT,
                    List.of("https://tong.visitkorea.or.kr/cms/resource/56/3467156_image2_1.jpg")));
            var p2 = communityService.createPost(seoul.getId(), new PostCreateRequest(
                    "서울 고궁 야간개장 일정 공유합니다",
                    "경복궁 야간개장 예매 꿀팁이랑 동선 정리. 미리 예매 필수예요!",
                    PostCategory.TIP,
                    List.of("https://tong.visitkorea.or.kr/cms/resource/23/2678623_image2_1.jpg")));
            var p3 = communityService.createPost(jeju.getId(), new PostCreateRequest(
                    "혼자 제주 2박3일 뚜벅이 후기",
                    "버스랑 도보로만 다녔는데 생각보다 충분했어요. 다닌 코스 공유합니다!",
                    PostCategory.REVIEW,
                    List.of("https://tong.visitkorea.or.kr/cms/resource/53/2869753_image2_1.jpg")));
            var p4 = communityService.createPost(busan.getId(), new PostCreateRequest(
                    "부산 야경 명소 어디가 제일 좋나요?",
                    "이번 주말 부산 가는데 야경 보기 좋은 곳 추천 부탁드려요 🙏",
                    PostCategory.QUESTION,
                    List.of("https://tong.visitkorea.or.kr/cms/resource/96/2576496_image2_1.jpg")));

            // 게시글별 좋아요/스크랩(여러 사용자) + 댓글
            engage(p1.id(), "제주 흑돼지 맛집 BEST 5", List.of(jeju, busan, seoul),
                    List.of(c(busan, "흑돼지 진짜 최고죠 ㅎㅎ"), c(seoul, "저장해갑니다 👍"), c(jeju, "근처 카페도 궁금해요!")));
            engage(p2.id(), "서울 고궁 야간개장", List.of(foodie, jeju),
                    List.of(c(jeju, "야간개장 분위기 미쳤어요"), c(foodie, "예매 링크도 공유 가능할까요?")));
            engage(p3.id(), "혼자 제주 뚜벅이 후기", List.of(seoul, busan, foodie),
                    List.of(c(seoul, "뚜벅이 여행 좋네요!"), c(busan, "코스 더 자세히 알고 싶어요")));
            engage(p4.id(), "부산 야경 명소", List.of(seoul),
                    List.of(c(seoul, "광안리 더베이101 강추합니다")));
            return 4;
        });

        // ── 핫플(자동 APPROVED) + 좋아요 — '지금 뜨는 여행지' 추천 정렬용 ──────────
        safe(counts, "hotPlaces", () -> {
            seedHotPlace(busan, "흰여울문화마을", "부산광역시 영도구 흰여울길", 35.0789, 129.0468,
                    HotPlaceCategory.ATTRACTION,
                    "영도 절벽 위에 옹기종기 들어선 달동네가 시간을 거슬러 올라간 느낌을 줘요. 좁은 골목마다 바다 뷰가 숨어 있고, 영화 촬영지로 알려지면서 부산 힙스터들의 단골 나들이 코스가 됐어요.",
                    "https://tong.visitkorea.or.kr/cms/resource/19/2576419_image2_1.jpg", 1240,
                    everyone, everyone.subList(0, 5));
            seedHotPlace(jeju, "성산일출봉", "제주특별자치도 서귀포시 성산읍 일출로", 33.4581, 126.9425,
                    HotPlaceCategory.NATURE,
                    "분화구를 향해 오르는 계단길에서 바라보는 제주 동쪽 바다가 시원하게 펼쳐져요. 유네스코 세계자연유산으로 지정된 곳으로, 이른 새벽 일출 시간에 맞춰 오르는 게 진짜 묘미예요.",
                    "https://tong.visitkorea.or.kr/cms/resource/08/2871008_image2_1.JPG", 1080,
                    everyone.subList(0, 7), everyone.subList(0, 4));
            seedHotPlace(seoul, "북촌한옥마을", "서울특별시 종로구 계동길", 37.5826, 126.9831,
                    HotPlaceCategory.ATTRACTION,
                    "600년 넘은 한옥들이 좁다란 골목을 따라 이어져 조선 시대 속으로 걸어 들어가는 기분이 들어요. 이른 아침에 오면 인파 없이 고즈넉한 분위기를 온전히 즐길 수 있어요.",
                    "https://tong.visitkorea.or.kr/cms/resource/23/2678623_image2_1.jpg", 870,
                    everyone.subList(1, 7), everyone.subList(2, 6));
            seedHotPlace(jeju, "협재해수욕장", "제주특별자치도 제주시 한림읍 협재리", 33.3940, 126.2396,
                    HotPlaceCategory.NATURE,
                    "수심이 얕아 걷다 보면 발밑으로 에메랄드빛 바닥이 훤히 들여다보여요. 물빛이 워낙 맑아서 날씨가 흐려도 빛깔이 살아 있고, 멀리 비양도가 수평선에 떠 있어 사진 찍기에도 좋아요.",
                    "https://tong.visitkorea.or.kr/cms/resource/53/2869753_image2_1.jpg", 760,
                    everyone.subList(0, 5), everyone.subList(0, 3));
            seedHotPlace(busan, "감천문화마을", "부산광역시 사하구 감내2로", 35.0975, 129.0107,
                    HotPlaceCategory.ATTRACTION,
                    "가파른 산비탈을 따라 형형색색의 집들이 계단처럼 쌓여 있는 독특한 경관이에요. 골목 곳곳에 숨어 있는 벽화와 조형물을 찾아다니는 재미가 쏠쏠한 부산의 예술 마을이에요.",
                    "https://tong.visitkorea.or.kr/cms/resource/96/2576496_image2_1.jpg", 640,
                    everyone.subList(2, 8), everyone.subList(3, 6));
            seedHotPlace(foodie, "광장시장 먹자골목", "서울특별시 종로구 창경궁로", 37.5701, 126.9999,
                    HotPlaceCategory.RESTAURANT,
                    "100년 넘은 재래시장 안에 이름난 먹거리 노점들이 가득 들어차 있어요. 바삭한 빈대떡과 꼬들꼬들한 마약김밥은 한 번 먹으면 두고두고 생각나는 맛이에요.",
                    "https://tong.visitkorea.or.kr/cms/resource/56/3467156_image2_1.jpg", 520,
                    everyone.subList(0, 6), everyone.subList(1, 4));
            seedHotPlace(seoul, "남산서울타워", "서울특별시 용산구 남산공원길", 37.5512, 126.9882,
                    HotPlaceCategory.ATTRACTION,
                    "케이블카를 타고 오르면 서울 도심이 360도 파노라마로 한눈에 들어와요. 해 질 녘부터 야경이 피어오르는 순간이 특히 아름다워서 데이트 코스로도 인기가 높아요.",
                    "https://tong.visitkorea.or.kr/cms/resource/37/3568037_image2_1.jpg", 410,
                    everyone.subList(1, 5), everyone.subList(0, 2));
            seedHotPlace(jeju, "오설록 티 뮤지엄", "제주특별자치도 서귀포시 안덕면 신화역사로", 33.3056, 126.2895,
                    HotPlaceCategory.CAFE,
                    "제주 안덕 들판에 펼쳐진 초록빛 녹차밭이 카페 창문 너머로 이어져요. 녹차 아이스크림과 함께 차 한 잔 마시며 천천히 쉬어 가기 좋은 힐링 공간이에요.",
                    "https://tong.visitkorea.or.kr/cms/resource/37/3568037_image2_1.jpg", 280,
                    everyone.subList(3, 7), everyone.subList(4, 7));
            return 8;
        });

        // ── 동행 모집글(+ 채팅방 자동 생성) ──────────────────────────────
        safe(counts, "companionPosts", () -> {
            companionService.createPost(jeju.getId(), new CompanionPostCreateRequest(
                    "제주 동쪽 해안 드라이브 함께 하실 분 🚗",
                    LocalDate.now().plusDays(21), "제주", "2박 3일", 4, 300000,
                    "성산일출봉, 우도, 섭지코지 도는 일정이에요. 사진 좋아하시는 분 환영!",
                    List.of("드라이브", "사진", "힐링")));
            return 1;
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  TEST 프로필 — 결정적 픽스처 + 엣지케이스
    // ════════════════════════════════════════════════════════════════════════
    private void seedTest(Map<String, Integer> counts) {
        User tester = saveUser("테스트유저", "tester");
        User buddy  = saveUser("테스트버디", "buddy");
        inc(counts, "users", 2);

        Attraction gyeongbok = saveAttraction("T126508", 12, "경복궁(테스트)",
                "서울특별시 종로구 사직로 161", "1", 37.5796, 126.9770, null, "테스트용 경복궁 스냅샷");
        Attraction haeundae = saveAttraction("T126509", 12, "해운대(테스트)",
                "부산광역시 해운대구 우동", "6", 35.1587, 129.1604, null, "테스트용 해운대 스냅샷");
        // 일부러 동선이 잡히지 않는 지점(설악산 대청봉 — 도로 라우팅 불가) → 동선 폴백 경로 검증용
        Attraction daecheong = saveAttraction("T999001", 12, "설악산 대청봉(비라우팅)",
                "강원특별자치도 속초시 설악동", "32", 38.1194, 128.4656, null, "비라우팅 엣지 — 동선 폴백 검증");
        inc(counts, "attractions", 3);

        // 엣지 1: 빈 계획(day/place 없음)
        TripPlan emptyPlan = buildPlan(tester.getId(), "[테스트] 빈 계획",
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(1),
                CompanionsType.SOLO, 0, null);
        savePlanGraph(emptyPlan);

        // 엣지 2: 최대 길이 계획(제목 100자, day 다수, 장소 다수)
        String maxTitle = "[테스트] " + "가".repeat(94);  // 총 100자
        TripPlan maxPlan = buildPlan(tester.getId(), maxTitle,
                LocalDate.now().plusDays(2), LocalDate.now().plusDays(5),
                CompanionsType.FAMILY, 2000000, null);
        addDay(maxPlan, 1, "1일차",
                place(gyeongbok, 1, LocalTime.of(9, 0), "오전"),
                place(haeundae, 2, LocalTime.of(14, 0), "오후"));
        addDay(maxPlan, 2, "2일차",
                place(haeundae, 1, LocalTime.of(10, 0), "재방문"));
        addDay(maxPlan, 3, "3일차", place(gyeongbok, 1, LocalTime.of(11, 0), null));
        addDay(maxPlan, 4, "4일차(메모 최대 길이)" , place(haeundae, 1, LocalTime.of(13, 0), "마"));
        savePlanGraph(maxPlan);

        // 엣지 3: 비라우팅 장소 계획 → 동선 폴백 경로 검증
        TripPlan fallbackPlan = buildPlan(tester.getId(), "[테스트] 비라우팅 동선 폴백",
                LocalDate.now().plusDays(6), LocalDate.now().plusDays(6),
                CompanionsType.SOLO, 100000, null);
        addDay(fallbackPlan, 1, "라우팅 불가 코스",
                place(gyeongbok, 1, LocalTime.of(9, 0), "도심"),
                place(daecheong, 2, LocalTime.of(12, 0), "산정상(라우팅 불가)"));
        savePlanGraph(fallbackPlan);

        inc(counts, "plans", 3);
        inc(counts, "tripDays", 5);
        inc(counts, "tripPlaces", 7);

        // 결정적 부가 데이터
        safe(counts, "favorites", () -> {
            favoriteService.toggle(tester.getId(), FavoriteTargetType.ATTRACTION, gyeongbok.getContentId(), gyeongbok.getTitle());
            return 1;
        });
        safe(counts, "reviews", () -> {
            reviewService.create(tester.getId(), gyeongbok.getContentId(), new ReviewCreateRequest(3, "테스트 리뷰 본문입니다."));
            return 1;
        });
        safe(counts, "stories", () -> {
            travelStoryService.create(tester.getId(), new TravelStoryCreateRequest(
                    "[테스트] 스토리", maxPlan.getId(), "before", "after", 3, null));
            return 1;
        });
        safe(counts, "follows", () -> {
            followService.toggle(tester.getId(), buddy.getId());
            return 1;
        });
        safe(counts, "checklists", () -> {
            checklistService.create(tester.getId(), new ChecklistItemCreateRequest("테스트 체크 항목", "기타", maxPlan.getId(), 1, 1));
            return 1;
        });
        safe(counts, "communityPosts", () -> {
            communityService.createPost(tester.getId(), new PostCreateRequest(
                    "[테스트] 게시글", "테스트 본문입니다.", PostCategory.QUESTION, List.of()));
            return 1;
        });
        safe(counts, "companionPosts", () -> {
            companionService.createPost(tester.getId(), new CompanionPostCreateRequest(
                    "[테스트] 동행 모집", LocalDate.now().plusDays(10), "서울", "1박 2일", 2, 50000,
                    "테스트 동행 설명입니다.", List.of("테스트")));
            return 1;
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  헬퍼
    // ════════════════════════════════════════════════════════════════════════

    /** 게시글 참여 시드용 — 댓글 작성자+내용 묶음. */
    private record SeedComment(User user, String content) {}
    private SeedComment c(User u, String content) { return new SeedComment(u, content); }

    /** 게시글에 좋아요·스크랩(여러 사용자) + 댓글을 부여해 커뮤니티가 실제 데이터처럼 보이게 한다. */
    private void engage(Long postId, String title, List<User> likers, List<SeedComment> comments) {
        for (User u : likers) {
            try { communityService.toggleLike(postId, u.getId()); } catch (Exception ignore) { /* best-effort */ }
            try { favoriteService.toggle(u.getId(), FavoriteTargetType.POST, String.valueOf(postId), title); }
            catch (Exception ignore) { /* best-effort */ }
        }
        for (SeedComment sc : comments) {
            try { communityService.createComment(postId, sc.user().getId(), new CommentCreateRequest(sc.content())); }
            catch (Exception ignore) { /* best-effort */ }
        }
    }

    /**
     * 핫플 등록(자동 APPROVED) + 데모 좋아요 베이스 부여 + 실제 사용자 좋아요(HotPlaceLike)·저장(찜) 부여.
     * likeCount(데모 베이스)에 실제 likers 수만큼 더해져 '지금 뜨는 여행지' 정렬에 반영된다.
     */
    private void seedHotPlace(User submitter, String name, String addr, double lat, double lng,
                             HotPlaceCategory cat, String desc, String imageUrl, int likeCount,
                             List<User> likers, List<User> savers) {
        var resp = hotPlaceService.register(submitter.getId(), new HotPlaceCreateRequest(
                name, addr, lat, lng, cat, desc, List.of(imageUrl)));
        final Long id = resp.id();
        hotPlaceRepository.findById(id).ifPresent(hp -> {
            hp.applyDemoLikeCount(likeCount);
            // 사실적 별점 — 인기(좋아요)에 약하게 비례하되 시연용으로 4.1~4.8 범위에 고정.
            int h = Math.abs(name.hashCode());
            double rating = 3.9 + Math.min(0.9, likeCount / 1400.0) + ((h % 11) - 5) * 0.01;
            rating = Math.round(Math.max(4.1, Math.min(4.8, rating)) * 10) / 10.0;
            // 리뷰수 — 좋아요의 35~64% 수준(데모용 자연스러운 표본).
            int ratingCount = Math.max(12, (int) Math.round(likeCount * (0.35 + (h % 30) / 100.0)));
            hp.applyDemoRating(rating, ratingCount);
            hotPlaceRepository.save(hp); // rating/ratingCount를 즉시 반영
            for (User u : likers) {
                try {
                    hotPlaceLikeRepository.save(com.trip.community.entity.HotPlaceLike.builder()
                            .userId(u.getId()).hotPlaceId(id).createdAt(java.time.LocalDateTime.now()).build());
                    hp.incLike();
                } catch (Exception ignore) { /* 중복 등 무시 */ }
            }
        });
        for (User u : savers) {
            try { favoriteService.toggle(u.getId(), FavoriteTargetType.HOTPLACE, String.valueOf(id), name); }
            catch (Exception ignore) { /* best-effort */ }
        }
    }

    private User saveUser(String nickname, String localPart) {
        return userRepository.save(User.builder()
                .nickname(nickname)
                .email(localPart + SEED_EMAIL_SUFFIX)
                .password(passwordEncoder.encode("seed1234"))
                .userRole(UserRole.USER)
                .profileImageUrl(User.DEFAULT_PROFILE_IMAGE_URL)
                .status(true)
                .build());
    }

    private Attraction saveAttraction(String contentId, int contentType, String title, String addr,
                                      String areaCode, double lat, double lng, String imageUrl, String overview) {
        // attractions 스냅샷은 유저 소유가 아니라 공유 캐시 — 리셋이 지우지 않으므로 이미 존재할 수 있다.
        // 무조건 save 하면 uk_attr_content(contentId+contentType) 유니크 충돌 → upsert 로 재사용한다.
        return attractionRepository.findByContentIdAndContentType(contentId, contentType)
                .orElseGet(() -> attractionRepository.save(Attraction.builder()
                        .contentId(contentId)
                        .contentType(contentType)
                        .title(title)
                        .addr(addr)
                        .areaCode(areaCode)
                        .latitude(lat)
                        .longitude(lng)
                        .imageUrl(imageUrl)
                        .overview(overview)
                        .fetchedAt(LocalDateTime.now())
                        .build()));
    }

    private TripPlan buildPlan(Long userId, String title, LocalDate start, LocalDate end,
                               CompanionsType companions, Integer budget, String imageUrl) {
        return TripPlan.builder()
                .userId(userId)
                .title(title)
                .startDate(start)
                .endDate(end)
                .companions(companions)
                .budget(budget)
                .origin(OriginType.MANUAL)
                .imageUrl(imageUrl)
                .build();
    }

    /** day + place 들을 plan 의 days 컬렉션에 연결(cascade 로 일괄 저장됨). */
    private void addDay(TripPlan plan, int dayNo, String memo, TripPlace... places) {
        TripDay day = TripDay.builder().plan(plan).dayNo(dayNo).memo(memo).build();
        for (TripPlace p : places) {
            // place 의 day 를 방금 만든 day 로 다시 묶는다(빌더에서 임시 null 로 만든 뒤 연결)
            attachPlace(day, p);
        }
        plan.getDays().add(day);
    }

    /** seq/visitTime/memo/attraction 정보를 보존하면서 day 에 연결한 새 TripPlace 를 day.places 에 추가. */
    private void attachPlace(TripDay day, TripPlace template) {
        TripPlace place = TripPlace.builder()
                .day(day)
                .attraction(template.getAttraction())
                .seq(template.getSeq())
                .visitTime(template.getVisitTime())
                .memo(template.getMemo())
                .build();
        day.getPlaces().add(place);
    }

    /** attraction/seq/visitTime/memo 만 담은 임시 TripPlace(아직 day 미연결). addDay 에서 실제 day 로 재구성. */
    private TripPlace place(Attraction attraction, int seq, LocalTime visitTime, String memo) {
        return TripPlace.builder()
                .attraction(attraction)
                .seq(seq)
                .visitTime(visitTime)
                .memo(memo)
                .build();
    }

    private void savePlanGraph(TripPlan plan) {
        planRepository.save(plan);  // cascade=ALL → days/places 함께 INSERT
    }

    private void inc(Map<String, Integer> counts, String key, int n) {
        counts.merge(key, n, Integer::sum);
    }

    /** 하위 시드 1건의 실패가 전체를 중단시키지 않도록 경고만 남기고 계속. */
    private void safe(Map<String, Integer> counts, String key, Supplier<Integer> action) {
        try {
            inc(counts, key, action.get());
        } catch (Exception e) {
            log.warn("[SeedService] '{}' 시드 일부 실패 — 무시하고 계속: {}", key, e.getMessage());
            counts.merge(key + "_failed", 1, Integer::sum);
        }
    }
}
