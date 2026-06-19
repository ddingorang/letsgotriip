package com.trip.group.controller;

import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.global.security.UserPrincipal;
import com.trip.group.dto.GroupCreateRequest;
import com.trip.group.dto.GroupDiscountResponse;
import com.trip.group.dto.GroupMemberResponse;
import com.trip.group.dto.GroupResponse;
import com.trip.group.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    // ─── 단체할인 (데모, 공개) ─────────────────────────────────

    @GetMapping("/discounts")
    public ResponseEntity<List<GroupDiscountResponse>> discounts() {
        return ResponseEntity.ok(groupService.discounts());
    }

    // ─── 그룹 ──────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<GroupResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GroupCreateRequest request
    ) {
        requireAuth(principal);
        GroupResponse response = groupService.create(principal.userId(), request);
        return ResponseEntity.created(URI.create("/api/groups/" + response.id())).body(response);
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        requireAuth(principal);
        return ResponseEntity.ok(groupService.list(principal.userId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.get(id));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<GroupMemberResponse>> members(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.members(id));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<Void> join(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        requireAuth(principal);
        groupService.join(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/leave")
    public ResponseEntity<Void> leave(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        requireAuth(principal);
        groupService.leave(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    private void requireAuth(UserPrincipal principal) {
        if (principal == null) {
            throw new GeneralException(ResponseCode._UNAUTHORIZED);
        }
    }
}
