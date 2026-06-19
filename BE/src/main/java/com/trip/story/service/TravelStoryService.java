package com.trip.story.service;

import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.story.dto.TravelStoryCreateRequest;
import com.trip.story.dto.TravelStoryResponse;
import com.trip.story.dto.TravelStoryUpdateRequest;
import com.trip.story.entity.TravelStory;
import com.trip.story.repository.TravelStoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TravelStoryService {

    private final TravelStoryRepository travelStoryRepository;

    /** 내 스토리 목록 — 최신순 */
    @Transactional(readOnly = true)
    public List<TravelStoryResponse> listMy(Long userId) {
        return travelStoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(TravelStoryResponse::from)
                .toList();
    }

    /** 스토리 상세 — 소유자만 조회 가능 */
    @Transactional(readOnly = true)
    public TravelStoryResponse get(Long userId, Long id) {
        return TravelStoryResponse.from(findOwned(userId, id));
    }

    /** 스토리 생성 */
    public TravelStoryResponse create(Long userId, TravelStoryCreateRequest request) {
        TravelStory story = TravelStory.builder()
                .userId(userId)
                .planId(request.planId())
                .title(request.title())
                .beforeNote(request.beforeNote())
                .afterNote(request.afterNote())
                .rating(request.rating())
                .coverImageUrl(request.coverImageUrl())
                .build();
        return TravelStoryResponse.from(travelStoryRepository.save(story));
    }

    /** 스토리 부분 수정 — 소유자만 가능 */
    public TravelStoryResponse update(Long userId, Long id, TravelStoryUpdateRequest request) {
        TravelStory story = findOwned(userId, id);
        story.update(
                request.title(),
                request.planId(),
                request.beforeNote(),
                request.afterNote(),
                request.rating(),
                request.coverImageUrl()
        );
        return TravelStoryResponse.from(story);
    }

    /** 스토리 삭제 — 소유자만 가능 */
    public void delete(Long userId, Long id) {
        TravelStory story = findOwned(userId, id);
        travelStoryRepository.delete(story);
    }

    /** 소유 검증 후 엔티티 반환 */
    private TravelStory findOwned(Long userId, Long id) {
        return travelStoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new GeneralException(ResponseCode._FORBIDDEN));
    }
}
