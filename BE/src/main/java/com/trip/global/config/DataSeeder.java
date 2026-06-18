// Created: 2026-06-18 12:45:42
package com.trip.global.config;

import com.trip.community.entity.Post;
import com.trip.community.entity.enums.PostCategory;
import com.trip.community.repository.PostRepository;
import com.trip.user.entity.User;
import com.trip.user.entity.enums.UserRole;
import com.trip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    private static final String SEED_MARKER_EMAIL = "kim@seed.local";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(SEED_MARKER_EMAIL)) return;

        User kim  = seedUser("kim@seed.local",  "김관통");
        User lee  = seedUser("lee@seed.local",  "이여행러");
        User park = seedUser("park@seed.local", "박솔로");

        postRepository.save(Post.builder()
                .author(kim)
                .title("부산 2박 3일 커플 여행 완벽 후기 🌊")
                .content("해운대에서 시작해 광안리, 감천문화마을까지 알차게 다녀왔어요. 특히 해운대 야경은 정말 인생 뷰였고, 밀면도 진짜 맛있었습니다!")
                .category(PostCategory.REVIEW)
                .build());

        postRepository.save(Post.builder()
                .author(lee)
                .title("제주 올레길 완주 도전기 — 11코스 후기")
                .content("드디어 11코스를 완주했습니다! 총 18.4km, 5시간 30분 소요. 서귀포 해안을 따라 걷는 코스인데 정말 힐링 그 자체예요.")
                .category(PostCategory.REVIEW)
                .build());

        postRepository.save(Post.builder()
                .author(park)
                .title("경주 혼여 꿀팁 모음 — 이건 꼭 가세요")
                .content("경주 첫 방문이라면 이 루트대로 하세요. 불국사 → 석굴암 → 첨성대 → 동궁과 월지 야경. 하루에 다 볼 수 있어요!")
                .category(PostCategory.TIP)
                .build());

        log.info("[DataSeeder] 커뮤니티 시드 데이터 삽입 완료");
    }

    private User seedUser(String email, String nickname) {
        return userRepository.save(User.builder()
                .email(email)
                .nickname(nickname)
                .password(passwordEncoder.encode("Seed1234!"))
                .userRole(UserRole.USER)
                .profileImageUrl("")
                .status(true)
                .build());
    }
}
