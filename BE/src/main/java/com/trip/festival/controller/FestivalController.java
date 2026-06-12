// Created: 2026-06-08 15:34:58
package com.trip.festival.controller;

import com.trip.festival.dto.FestivalResponse;
import com.trip.festival.scheduler.FestivalSyncScheduler;
import com.trip.festival.service.FestivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/festivals")
@RequiredArgsConstructor
public class FestivalController {

    private final FestivalService festivalService;
    private final FestivalSyncScheduler festivalSyncScheduler;

    /**
     * GET /api/festivals?areaCode=1&status=ONGOING
     * areaCode, status 모두 선택적. 기본값: ENDED 제외 전체
     */
    @GetMapping
    public ResponseEntity<List<FestivalResponse>> getFestivals(
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(festivalService.getFestivals(areaCode, status));
    }

    /**
     * POST /api/festivals/sync
     * 배치 Job 수동 실행 (개발·테스트용)
     */
    @PostMapping("/sync")
    public ResponseEntity<String> triggerSync() {
        festivalSyncScheduler.syncNow();
        return ResponseEntity.ok("동기화 작업이 시작되었습니다.");
    }
}
