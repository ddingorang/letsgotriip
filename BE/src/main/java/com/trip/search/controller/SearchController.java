package com.trip.search.controller;

import com.trip.search.dto.SearchResultResponse;
import com.trip.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/search — 통합검색 (공개, 비회원 탐색 허용).
 * q: 검색어, type: all|attraction|post|companion|festival (기본 all).
 * 소스별 상위 N개를 그룹으로 묶어 반환한다. 외부(TourAPI) 실패는 빈 배열로 graceful.
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<SearchResultResponse> search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "type", defaultValue = "all") String type
    ) {
        return ResponseEntity.ok(searchService.search(q, type));
    }
}
