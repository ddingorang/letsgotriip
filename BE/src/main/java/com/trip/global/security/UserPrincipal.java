package com.trip.global.security;

import java.security.Principal;

public record UserPrincipal(Long userId) implements Principal {

    // 스프링 내부에서 활용하는 문자열 타입의 사용자 식별자
    // 사용자의 이름이 아니다.
    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
