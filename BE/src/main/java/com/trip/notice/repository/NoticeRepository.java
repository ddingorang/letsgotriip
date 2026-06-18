package com.trip.notice.repository;

import com.trip.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /** 고정 공지 우선, 그다음 최신순 */
    List<Notice> findAllByOrderByPinnedDescCreatedAtDesc();
}
