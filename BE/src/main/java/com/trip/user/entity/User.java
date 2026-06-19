package com.trip.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.trip.user.dto.SignupRequestDto;
import com.trip.user.entity.enums.Gender;
import com.trip.user.entity.enums.UserRole;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User extends BaseEntity {

    /** 비밀번호 기반 최소 사용자 정보 */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "nickname", nullable = false, length = 20)
    private String nickname;

    @Column(name = "email", nullable = false, unique = true, length = 50)
    private String email;

    @Column(name = "password", nullable = false, length = 60)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    private UserRole userRole;

    @Column(name = "profile_image_url", nullable = false)
    private String profileImageUrl;

    /** 프로필 한줄 소개 */
    @Column(name = "bio", length = 100)
    private String bio;

    /** 온보딩 취향설문 — 관심사(복수). 콤마 구분 문자열로 저장 (예: "nature,food,activity") */
    @Column(name = "preferred_interests", length = 255)
    private String preferredInterests;

    /** 온보딩 취향설문 — 동행 유형 (예: "혼자", "연인", "친구", "가족") */
    @Column(name = "preferred_companion", length = 20)
    private String preferredCompanion;

    @Column(name = "status", nullable = false)
    private boolean status; // 사용자 상태 (정상 회원: true, 삭제 요청 회원: false)

    @Column(name = "inactive_date")
    private LocalDateTime inActiveDate; // 비활성화 datetime

    /** 소셜 로그인 정보 */
    @Column(name = "oauth_type", length = 20)
    private String oauthType;

    @Column(name = "oauth_key", length = 255)
    private String oauthKey;

    // 게임포인트
    // 게임 통계

    /** 프로필 이미지를 지정하지 않은 경우 사용할 기본 이미지 URL */
    public static final String DEFAULT_PROFILE_IMAGE_URL = "/images/default-profile.png";

    public static User of(SignupRequestDto dto, String encodedPassword) {

        final String profileImageUrl =
                (dto.profileImageUrl() == null || dto.profileImageUrl().isBlank())
                        ? DEFAULT_PROFILE_IMAGE_URL
                        : dto.profileImageUrl();

        return User.builder()
                .nickname(dto.nickname())
                .email(dto.email())
                .password(encodedPassword)
                .userRole(UserRole.USER)
                .profileImageUrl(profileImageUrl)
                .status(true)
                .inActiveDate(null)
                .build();
    }

    public static User ofOAuth(String email, String nickname, String profileImageUrl, String oauthType, String oauthKey, String encodedPassword) {

        return User.builder()
                .email(email)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .oauthType(oauthType)
                .oauthKey(oauthKey)
                .password(encodedPassword)
                .userRole(UserRole.USER)
                .status(true)
                .build();
    }

    public void updateProfile(String nickname, String profileImageUrl, String bio) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
        if (bio != null) {
            // 빈 문자열이면 소개 제거(null), 아니면 갱신
            this.bio = bio.isBlank() ? null : bio;
        }
    }

    /** 온보딩 취향설문 저장. null 인자는 무시(기존 값 유지). */
    public void updatePreferences(String preferredInterests, String preferredCompanion) {
        if (preferredInterests != null) {
            this.preferredInterests = preferredInterests.isBlank() ? null : preferredInterests;
        }
        if (preferredCompanion != null) {
            this.preferredCompanion = preferredCompanion.isBlank() ? null : preferredCompanion;
        }
    }

    public void withdraw() {
        this.status = false;
        this.inActiveDate = LocalDateTime.now();
    }

    /** 비밀번호 변경. 이미 암호화된 값을 받는다. */
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
