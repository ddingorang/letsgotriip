package com.trip.notice.service;

import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.notice.dto.NoticeCreateRequest;
import com.trip.notice.dto.NoticeResponse;
import com.trip.notice.dto.NoticeUpdateRequest;
import com.trip.notice.entity.Notice;
import com.trip.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;

    /** 공지 목록 — 고정 우선, 최신순 */
    public List<NoticeResponse> getNotices() {
        return noticeRepository.findAllByOrderByPinnedDescCreatedAtDesc()
                .stream()
                .map(NoticeResponse::from)
                .toList();
    }

    /** 공지 상세 */
    public NoticeResponse getNotice(Long id) {
        return noticeRepository.findById(id)
                .map(NoticeResponse::from)
                .orElseThrow(() -> new GeneralException(ResponseCode.NOTICE_NOT_FOUND));
    }

    /** category 미지정 시 사용할 기본 분류 라벨 */
    private static final String DEFAULT_CATEGORY = "안내";

    /** 공지 생성 (admin) */
    @Transactional
    public NoticeResponse create(NoticeCreateRequest request) {
        final Notice notice = Notice.builder()
                .category(resolveCategory(request.category()))
                .title(request.title())
                .content(request.content())
                .pinned(request.pinned())
                .build();
        return NoticeResponse.from(noticeRepository.save(notice));
    }

    /** 공지 수정 (admin) */
    @Transactional
    public NoticeResponse update(Long id, NoticeUpdateRequest request) {
        final Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new GeneralException(ResponseCode.NOTICE_NOT_FOUND));
        notice.update(
                resolveCategory(request.category()),
                request.title(),
                request.content(),
                request.pinned()
        );
        return NoticeResponse.from(notice);
    }

    /** 공지 삭제 (admin) */
    @Transactional
    public void delete(Long id) {
        final Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new GeneralException(ResponseCode.NOTICE_NOT_FOUND));
        noticeRepository.delete(notice);
    }

    private String resolveCategory(String category) {
        return (category == null || category.isBlank()) ? DEFAULT_CATEGORY : category;
    }
}
