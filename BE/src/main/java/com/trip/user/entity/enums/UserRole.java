package com.trip.user.entity.enums;

public enum UserRole {

    ADMIN("관리자"),
    VIP("후원자"),
    USER("사용자");

    private final String userRole;

    UserRole(String userRole) {
        this.userRole = userRole;
    }
}
