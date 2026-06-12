package com.trip.global.security.oauth2;

import com.trip.user.entity.User;
import com.trip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuth2Attribute oAuth2Attribute =
                OAuth2Attribute.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        User user = saveOrUpdate(oAuth2Attribute);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getUserRole().name())),
                oAuth2Attribute.convertToMap(),
                "email"
        );
    }

    private User saveOrUpdate(OAuth2Attribute oAuth2Attribute) {
        User user = userRepository.findByEmail(oAuth2Attribute.getEmail())
                .map(entity -> {
                    // 기존 사용자의 경우 정보를 업데이트할 수 있음 (선택 사항)
                    return entity;
                })
                .orElseGet(() -> {
                    // 신규 사용자의 경우 회원가입 처리
                    // 비밀번호는 사용하지 않으므로 임의의 값 설정
                    String dummyPassword = UUID.randomUUID().toString();
                    return User.ofOAuth(
                            oAuth2Attribute.getEmail(),
                            oAuth2Attribute.getName(),
                            oAuth2Attribute.getPicture(),
                            oAuth2Attribute.getOauthType(),
                            oAuth2Attribute.getOauthKey(),
                            passwordEncoder.encode(dummyPassword)
                    );
                });

        return userRepository.save(user);
    }
}
