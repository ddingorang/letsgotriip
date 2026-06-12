package com.trip.user.controller;

import com.trip.global.security.UserPrincipal;
import com.trip.global.util.CookieUtil;
import com.trip.user.dto.UserProfileResponseDto;
import com.trip.user.dto.UserUpdateRequestDto;
import com.trip.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CookieUtil cookieUtil;

    @GetMapping("me")
    public ResponseEntity<UserProfileResponseDto> getProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ResponseEntity.ok(userService.getProfile(userPrincipal.userId()));
    }

    @PatchMapping("me")
    public ResponseEntity<Void> updateProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody UserUpdateRequestDto updateDto
    ) {
        userService.updateProfile(userPrincipal.userId(), updateDto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("me")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @CookieValue(value = "sessionId", required = false) String sessionId,
            HttpServletResponse response
    ) {
        userService.withdraw(userPrincipal.userId(), sessionId);
        cookieUtil.expireAuthCookies(response);
        return ResponseEntity.ok().build();
    }
}
