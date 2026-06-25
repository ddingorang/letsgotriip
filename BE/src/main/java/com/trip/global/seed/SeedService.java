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
        //    카테고리별 5건씩 — 후기(REVIEW)·질문(QUESTION)·꿀팁(TIP)·맛집(RESTAURANT) = 총 20건.
        final String imgHeundae = "https://tong.visitkorea.or.kr/cms/resource/41/2655741_image2_1.jpg";
        final String imgGyeongbok = "https://tong.visitkorea.or.kr/cms/resource/23/2678623_image2_1.jpg";
        final String imgHyeopjae = "https://tong.visitkorea.or.kr/cms/resource/53/2869753_image2_1.jpg";
        final String imgSeongsan = "https://tong.visitkorea.or.kr/cms/resource/08/2871008_image2_1.JPG";
        final String imgNseoul = "https://tong.visitkorea.or.kr/cms/resource/96/2785596_image2_1.jpg";
        final String imgNamsan = "https://tong.visitkorea.or.kr/cms/resource/37/3568037_image2_1.jpg";
        final String imgHeunyeoul = "https://tong.visitkorea.or.kr/cms/resource/19/2576419_image2_1.jpg";
        final String imgGamcheon = "https://tong.visitkorea.or.kr/cms/resource/96/2576496_image2_1.jpg";
        final String imgGwangjang = "https://tong.visitkorea.or.kr/cms/resource/56/3467156_image2_1.jpg";
        final String imgJeju = "https://tong.visitkorea.or.kr/cms/resource/34/3567934_image2_1.jpg";

        safe(counts, "communityPosts", () -> {
            // 좋아요 평균 ≈ 60, 댓글 평균 ≈ 3건, 본문은 최소 두 문장 이상.
            // ── 후기(REVIEW) 5건 ──────────────────────────────────────────
            seedPost(jeju, "혼자 제주 2박3일 뚜벅이 후기",
                    "렌터카 없이 버스와 도보로만 2박 3일을 다녀봤어요. 처음엔 불편할까 걱정했는데 동선만 잘 짜면 충분히 다닐 만하더라고요. 제가 실제로 다닌 코스와 버스 노선까지 아래에 정리해뒀으니 참고하세요.",
                    PostCategory.REVIEW, imgHyeopjae, 72, List.of(seoul, busan, foodie),
                    List.of(c(seoul, "뚜벅이 여행 정보 너무 알차네요!"), c(busan, "버스 노선 정리 감사합니다"), c(foodie, "다음 제주여행 때 꼭 참고할게요")));
            seedPost(busan, "부산 1박2일 다녀온 솔직 후기",
                    "해운대에서 시작해 광안리까지 해안을 따라 천천히 걸었어요. 주말이라 사람이 정말 많았지만 그만큼 활기차서 좋았습니다. 야경 포인트마다 사진 찍느라 시간 가는 줄 몰랐네요.",
                    PostCategory.REVIEW, imgHeundae, 54, List.of(jeju, seoul, gangneung),
                    List.of(c(jeju, "광안리 야경 진짜 예쁘죠"), c(gangneung, "다음 주에 가는데 참고할게요!"), c(seoul, "사진 잘 찍으셨네요 👍")));
            seedPost(gangneung, "강릉 바다 보러 다녀왔어요",
                    "안목해변 커피거리에서 파도 소리를 들으며 커피를 마셨는데 그 자체로 힐링이었어요. 바다 바로 앞 카페에 자리 잡고 한참을 멍하니 앉아 있었네요.",
                    PostCategory.REVIEW, imgHyeopjae, 48, List.of(jeju, sokcho, yeosu),
                    List.of(c(sokcho, "안목해변 분위기 너무 좋죠"), c(yeosu, "커피거리 추천 감사해요")));
            seedPost(gyeongju, "경주 역사 여행 2박3일 후기",
                    "불국사와 첨성대를 거쳐 황리단길까지 알차게 둘러봤어요. 낮에는 유적지, 밤에는 동궁과 월지 야경으로 하루가 꽉 찼습니다. 생각보다 야경 명소가 많아서 놀랐네요.",
                    PostCategory.REVIEW, imgGyeongbok, 63, List.of(seoul, busan, foodie),
                    List.of(c(foodie, "황리단길 맛집도 궁금해요"), c(seoul, "경주 야경 의외네요!"), c(busan, "코스 잘 짜셨네요"), c(jeju, "저도 다음에 가봐야겠어요")));
            seedPost(yeosu, "여수 밤바다 보고 온 후기",
                    "케이블카를 타고 내려다본 여수 밤바다는 정말 잊을 수가 없어요. 노래 가사가 괜히 나온 게 아니라는 걸 직접 보고서야 알았네요. 해상 케이블카는 꼭 타보시길 추천해요.",
                    PostCategory.REVIEW, imgGamcheon, 58, List.of(jeju, busan, gangneung),
                    List.of(c(busan, "여수 밤바다 진리죠"), c(gangneung, "케이블카 꼭 타봐야겠어요"), c(jeju, "사진만 봐도 설레네요")));

            // ── 질문(QUESTION) 5건 ────────────────────────────────────────
            seedPost(busan, "부산 야경 명소 어디가 제일 좋나요?",
                    "이번 주말에 부산으로 1박 2일 여행을 가요. 야경을 정말 좋아하는데 어디가 제일 예쁜지 추천 부탁드려요 🙏 도보나 대중교통으로 갈 수 있으면 더 좋아요.",
                    PostCategory.QUESTION, imgGamcheon, 41, List.of(seoul, jeju),
                    List.of(c(seoul, "광안리 더베이101 강추합니다"), c(jeju, "황령산 전망대도 좋아요"), c(gangneung, "송도 케이블카에서 보는 야경도 멋져요"), c(foodie, "흰여울문화마을 노을도 예뻐요")));
            seedPost(seoul, "서울 근교 당일치기 어디가 좋을까요?",
                    "차가 없어서 대중교통으로만 다녀와야 해요. 서울 근교로 당일치기 다녀오기 좋은 곳 추천해주세요. 너무 빡센 코스 말고 여유롭게 쉬다 올 수 있으면 좋겠어요.",
                    PostCategory.QUESTION, imgNseoul, 38, List.of(foodie, gangneung),
                    List.of(c(gangneung, "춘천 가까워서 좋아요"), c(foodie, "강화도도 추천드려요")));
            seedPost(sokcho, "속초 맛집 추천 부탁드려요",
                    "속초로 여행 가는데 닭강정 말고 현지인이 가는 맛집이 궁금해요. 회 종류 말고도 추천해주실 만한 메뉴 있을까요? 아침 식사로 든든하게 먹을 곳도 찾고 있어요.",
                    PostCategory.QUESTION, imgHeundae, 45, List.of(jeju, gangneung),
                    List.of(c(gangneung, "물회랑 도루묵찌개 추천해요"), c(jeju, "중앙시장 꼭 가보세요"), c(yeosu, "아침엔 순두부 백반도 좋아요")));
            seedPost(jeju, "제주 렌터카 vs 뚜벅이 고민돼요",
                    "운전이 서툰 편이라 제주에서 렌터카를 빌려야 할지 고민이에요. 렌터카 없이 대중교통으로만 다니면 많이 불편할까요? 두 방법 다 경험해보신 분들 의견이 궁금합니다.",
                    PostCategory.QUESTION, imgSeongsan, 67, List.of(busan, seoul, foodie),
                    List.of(c(busan, "버스도 잘 돼 있어서 충분히 가능해요"), c(seoul, "동선 짧게 짜면 뚜벅이도 괜찮아요"), c(gangneung, "초행이면 렌터카가 마음 편하긴 해요")));
            seedPost(gangneung, "강릉 숙소 어디가 좋나요?",
                    "바다가 보이는 숙소 위주로 찾고 있어요. 1박 10만원 정도 예산인데 가성비 좋은 곳 추천 부탁드려요. 조용히 쉴 수 있는 분위기면 더 좋겠습니다.",
                    PostCategory.QUESTION, imgHyeopjae, 52, List.of(sokcho, yeosu),
                    List.of(c(sokcho, "강문해변 쪽 추천해요"), c(yeosu, "경포 근처도 뷰 좋아요"), c(jeju, "사천진해변 쪽도 조용해서 좋더라고요")));

            // ── 꿀팁(TIP) 5건 ─────────────────────────────────────────────
            seedPost(seoul, "서울 고궁 야간개장 일정 공유합니다",
                    "경복궁 야간개장은 예매 경쟁이 치열해서 오픈 시간에 바로 들어가야 해요. 예매 꿀팁과 추천 동선을 아래에 정리해뒀습니다. 한복을 입으면 입장료가 무료라는 점도 잊지 마세요.",
                    PostCategory.TIP, imgGyeongbok, 80, List.of(foodie, jeju),
                    List.of(c(jeju, "야간개장 분위기 미쳤어요"), c(foodie, "예매 링크도 공유 가능할까요?"), c(busan, "동선 정리 감사합니다")));
            seedPost(jeju, "제주 항공권 싸게 사는 꿀팁",
                    "특가 알림을 설정해두고 새벽 시간대 항공권을 노리면 반값에도 잡을 수 있어요. 평일 출발·복귀로 잡는 게 가장 저렴한 핵심 포인트입니다. 성수기는 두 달 전부터 미리 예매하는 걸 추천해요.",
                    PostCategory.TIP, imgJeju, 85, List.of(busan, seoul, gangneung),
                    List.of(c(busan, "새벽 특가 진짜 꿀이죠"), c(gangneung, "알림 설정 바로 해야겠네요"), c(foodie, "평일 출발 정보 감사해요"), c(seoul, "두 달 전 예매 팁 좋네요")));
            seedPost(foodie, "여행 짐 싸기 체크리스트 공유",
                    "충전기, 상비약, 보조배터리처럼 빠뜨리기 쉬운 것부터 먼저 챙기세요. 품목별로 파우치를 나눠 담으면 가방 안에서 찾기도 편하고 짐도 줄어들어요.",
                    PostCategory.TIP, imgNamsan, 61, List.of(jeju, seoul, yeosu),
                    List.of(c(yeosu, "파우치 분류 꿀팁이네요"), c(seoul, "상비약 자주 까먹는데 감사해요")));
            seedPost(busan, "부산 지하철로 여행하는 법",
                    "해운대, 광안리, 자갈치시장까지 주요 명소는 대부분 지하철로 닿아요. 하루에 여러 곳을 도는 일정이면 1일권을 끊는 게 훨씬 저렴합니다. 환승 동선까지 아래에 정리해뒀어요.",
                    PostCategory.TIP, imgHeunyeoul, 49, List.of(seoul, foodie),
                    List.of(c(seoul, "1일권 정보 감사해요"), c(foodie, "자갈치까지 지하철로 되는군요"), c(jeju, "환승 동선 정리 최고네요")));
            seedPost(gyeongju, "경주 한복 대여 꿀팁",
                    "황리단길 근처에서 한복을 빌리면 반납도 편하고 사진 찍을 명소도 바로 가까워요. 평일에 가면 한산해서 인생샷 건지기에도 훨씬 좋습니다. 대여점마다 소품 종류가 다르니 미리 비교해보세요.",
                    PostCategory.TIP, imgGyeongbok, 57, List.of(seoul, busan, gangneung),
                    List.of(c(gangneung, "한복 입고 찍으면 예쁘죠"), c(busan, "평일 추천 감사합니다"), c(foodie, "소품 비교 팁 좋네요")));

            // ── 맛집(RESTAURANT) 5건 ──────────────────────────────────────
            seedPost(foodie, "제주 흑돼지 맛집 BEST 5 🐷",
                    "현지인 추천을 받아 다녀온 제주 흑돼지 맛집 다섯 곳을 정리했어요. 연탄에 구운 흑돼지는 겉은 바삭하고 속은 촉촉해서 정말 별미였습니다. 가게별 특징과 추천 메뉴도 함께 적어뒀어요.",
                    PostCategory.RESTAURANT, imgGwangjang, 84, List.of(jeju, busan, seoul),
                    List.of(c(busan, "흑돼지 진짜 최고죠 ㅎㅎ"), c(seoul, "저장해갑니다 👍"), c(jeju, "근처 카페도 궁금해요!"), c(gangneung, "리스트 알차네요")));
            seedPost(busan, "부산 돼지국밥 맛집 정리",
                    "서면과 남포동 일대 돼지국밥집 다섯 곳을 직접 다니며 비교해봤어요. 집집마다 국물 진하기와 고기 양이 달라서 취향대로 고르시면 됩니다. 부추를 듬뿍 넣어 드시는 걸 추천해요.",
                    PostCategory.RESTAURANT, imgGamcheon, 66, List.of(jeju, foodie, gangneung),
                    List.of(c(foodie, "돼지국밥 리스트 최고예요"), c(jeju, "남포동 자주 가는데 참고할게요"), c(seoul, "국물 진한 집 어디인가요?")));
            seedPost(seoul, "광장시장 먹거리 추천",
                    "광장시장은 빈대떡, 마약김밥, 육회까지 줄 서서 먹을 가치가 충분해요. 평일 낮에 가면 주말보다 덜 붐벼서 여유롭게 즐길 수 있습니다. 현금을 챙겨 가면 결제가 더 수월해요.",
                    PostCategory.RESTAURANT, imgGwangjang, 73, List.of(busan, foodie, gyeongju),
                    List.of(c(foodie, "마약김밥 못 참죠"), c(gyeongju, "육회 맛집 어디인가요?"), c(busan, "현금 팁 감사합니다")));
            seedPost(gangneung, "강릉 초당두부 맛집",
                    "갓 만든 순두부에 간장을 살짝 올려 먹으면 고소함이 입안 가득 퍼져요. 인기 있는 집은 아침 일찍 가야 자리가 있으니 서두르는 게 좋습니다.",
                    PostCategory.RESTAURANT, imgHyeopjae, 55, List.of(jeju, sokcho, yeosu),
                    List.of(c(sokcho, "초당두부 아침에 먹으면 최고죠"), c(yeosu, "순두부 생각나네요")));
            seedPost(yeosu, "여수 게장 맛집 추천",
                    "간장게장 백반 한 상이면 밥 두 공기는 기본으로 비우게 돼요. 짭조름한 게장에 돌산갓김치를 곁들이면 그야말로 밥도둑입니다. 예약하고 가면 대기 없이 먹을 수 있어요.",
                    PostCategory.RESTAURANT, imgGamcheon, 62, List.of(busan, foodie, gangneung),
                    List.of(c(foodie, "게장에 밥도둑이죠"), c(busan, "돌산갓김치 조합 인정합니다"), c(jeju, "예약 팁 감사해요")));
            return 20;
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

    /**
     * 게시글 1건 생성 + 좋아요(데모 수)·스크랩·댓글 참여까지 한 번에 처리한다.
     * likeCount 는 데모용으로 직접 설정해 평균 좋아요 수를 시드 유저 수와 무관하게 맞춘다.
     */
    private void seedPost(User author, String title, String body, PostCategory category, String imageUrl,
                          int likeCount, List<User> scrappers, List<SeedComment> comments) {
        var resp = communityService.createPost(author.getId(), new PostCreateRequest(
                title, body, category, imageUrl == null ? List.of() : List.of(imageUrl)));
        postRepository.findById(resp.id()).ifPresent(p -> {
            p.applyDemoLikeCount(likeCount);
            postRepository.save(p);
        });
        // 스크랩(찜) + 댓글만 실제 토글. 좋아요는 위에서 데모 수로 고정.
        for (User u : scrappers) {
            try { favoriteService.toggle(u.getId(), FavoriteTargetType.POST, String.valueOf(resp.id()), title); }
            catch (Exception ignore) { /* best-effort */ }
        }
        for (SeedComment sc : comments) {
            try { communityService.createComment(resp.id(), sc.user().getId(), new CommentCreateRequest(sc.content())); }
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
