package com.trip.notice.controller;

import com.trip.notice.dto.NoticeCreateRequest;
import com.trip.notice.dto.NoticeResponse;
import com.trip.notice.dto.NoticeUpdateRequest;
import com.trip.notice.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GET    /api/notices       — 공지 목록 (공개)
 * GET    /api/notices/{id}  — 공지 상세 (공개)
 * POST   /api/notices       — 공지 생성 (admin)
 * PATCH  /api/notices/{id}  — 공지 수정 (admin, FE와 통일)
 * DELETE /api/notices/{id}  — 공지 삭제 (admin)
 *
 * 권한 강제(ADMIN)는 SecurityConfig 에서 처리된다.
 */
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public ResponseEntity<List<NoticeResponse>> getNotices() {
        return ResponseEntity.ok(noticeService.getNotices());
    }

    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeResponse> getNotice(@PathVariable Long noticeId) {
        return ResponseEntity.ok(noticeService.getNotice(noticeId));
    }

    /** 공지 생성 (admin) */
    @PostMapping
    public ResponseEntity<NoticeResponse> createNotice(
            @Valid @RequestBody NoticeCreateRequest request
    ) {
        return ResponseEntity.ok(noticeService.create(request));
    }

    /** 공지 수정 (admin) — FE AdminView가 PATCH를 보내므로 PATCH로 통일 */
    @PatchMapping("/{noticeId}")
    public ResponseEntity<NoticeResponse> updateNotice(
            @PathVariable Long noticeId,
            @Valid @RequestBody NoticeUpdateRequest request
    ) {
        return ResponseEntity.ok(noticeService.update(noticeId, request));
    }

    /** 공지 삭제 (admin) */
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long noticeId) {
        noticeService.delete(noticeId);
        return ResponseEntity.ok().build();
    }
}
