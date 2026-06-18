package com.trip.notice.service;

import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.notice.dto.NoticeResponse;
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
}
