package com.trip.checklist.controller;

import com.trip.checklist.dto.ChecklistItemCreateRequest;
import com.trip.checklist.dto.ChecklistItemResponse;
import com.trip.checklist.dto.ChecklistItemUpdateRequest;
import com.trip.checklist.dto.ChecklistTemplateResponse;
import com.trip.checklist.service.ChecklistService;
import com.trip.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * 여행 준비 체크리스트 API — 모든 엔드포인트 인증 필요.
 *
 * GET    /api/checklists            — 내 항목 목록 (planId 선택)
 * POST   /api/checklists            — 항목 생성
 * PATCH  /api/checklists/{id}       — 항목 부분 수정
 * PATCH  /api/checklists/{id}/toggle— 완료/미완료 토글
 * DELETE /api/checklists/{id}       — 항목 삭제
 * GET    /api/checklists/templates  — 내장 템플릿 목록
 * POST   /api/checklists/apply      — 템플릿 일괄 적용
 */
@RestController
@RequestMapping("/api/checklists")
@RequiredArgsConstructor
public class ChecklistController {

    private final ChecklistService checklistService;

    @GetMapping
    public ResponseEntity<List<ChecklistItemResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long planId
    ) {
        return ResponseEntity.ok(checklistService.list(principal.userId(), planId));
    }

    @PostMapping
    public ResponseEntity<ChecklistItemResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChecklistItemCreateRequest request
    ) {
        ChecklistItemResponse response = checklistService.create(principal.userId(), request);
        return ResponseEntity.created(URI.create("/api/checklists/" + response.id())).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ChecklistItemResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ChecklistItemUpdateRequest request
    ) {
        return ResponseEntity.ok(checklistService.update(principal.userId(), id, request));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ChecklistItemResponse> toggle(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(checklistService.toggle(principal.userId(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        checklistService.delete(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/templates")
    public ResponseEntity<List<ChecklistTemplateResponse>> templates() {
        return ResponseEntity.ok(checklistService.templates());
    }

    @PostMapping("/apply")
    public ResponseEntity<List<ChecklistItemResponse>> applyTemplate(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String templateKey,
            @RequestParam(required = false) Long planId
    ) {
        List<ChecklistItemResponse> response =
                checklistService.applyTemplate(principal.userId(), templateKey, planId);
        return ResponseEntity.status(201).body(response);
    }
}
