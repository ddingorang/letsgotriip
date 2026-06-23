// Created: 2026-06-15 23:25:31
package com.trip.community.service;

import com.trip.community.dto.*;
import com.trip.community.entity.HotPlace;
import com.trip.community.entity.HotPlacePhoto;
import com.trip.community.entity.enums.HotPlaceStatus;
import com.trip.community.repository.HotPlacePhotoRepository;
import com.trip.community.repository.HotPlaceRepository;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.user.entity.User;
import com.trip.user.entity.enums.UserRole;
import com.trip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotPlaceService {

    private final HotPlaceRepository hotPlaceRepository;
    private final HotPlacePhotoRepository hotPlacePhotoRepository;
    private final UserRepository userRepository;

    // ─── 조회 ─────────────────────────────────────────────────

    public Page<HotPlaceSummaryResponse> getApprovedHotPlaces(Pageable pageable) {
        return hotPlaceRepository.findAllByStatusOrderByCreatedAtDesc(HotPlaceStatus.APPROVED, pageable)
                .map(hp -> HotPlaceSummaryResponse.of(hp, getThumbnail(hp.getId())));
    }

    /** 인기순 핫플('지금 뜨는 여행지' 추천) — 좋아요 높은 순. */
    public Page<HotPlaceSummaryResponse> getPopularHotPlaces(Pageable pageable) {
        return hotPlaceRepository.findAllByStatusOrderByLikeCountDescIdDesc(HotPlaceStatus.APPROVED, pageable)
                .map(hp -> HotPlaceSummaryResponse.of(hp, getThumbnail(hp.getId())));
    }

    public HotPlaceResponse getHotPlace(Long hotPlaceId, Long userId) {
        HotPlace hotPlace = findHotPlace(hotPlaceId);

        // 공개 상세는 APPROVED 만 노출. PENDING/REJECTED 는 작성자 또는 관리자만 조회 가능하며,
        // 그 외(비로그인 포함)에게는 존재 자체를 숨기기 위해 NOT_FOUND 로 응답한다.
        if (hotPlace.getStatus() != HotPlaceStatus.APPROVED && !canViewHidden(hotPlace, userId)) {
            throw new GeneralException(ResponseCode.HOTPLACE_NOT_FOUND);
        }

        return HotPlaceResponse.of(hotPlace, getImageUrls(hotPlaceId));
    }

    private boolean canViewHidden(HotPlace hotPlace, Long userId) {
        if (userId == null) return false;
        if (hotPlace.getSubmitter().getId().equals(userId)) return true;
        return userRepository.findById(userId)
                .map(user -> user.getUserRole() == UserRole.ADMIN)
                .orElse(false);
    }

    // ─── 등록 신청 ────────────────────────────────────────────

    @Transactional
    public HotPlaceResponse register(Long userId, HotPlaceCreateRequest request) {
        User submitter = findUser(userId);

        // 자유 등록 정책: 신규 등록은 관리자 승인 없이 APPROVED 로 저장하여
        // 즉시 공개 목록/지도에 노출된다. (approve/reject/getPending 관리자 플로우는 유지하나 미사용)
        HotPlace hotPlace = hotPlaceRepository.save(HotPlace.builder()
                .submitter(submitter)
                .name(request.name())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .category(request.category())
                .description(request.description())
                .status(HotPlaceStatus.APPROVED)
                .build());

        List<String> imageUrls = savePhotos(hotPlace, request.imageUrls());
        return HotPlaceResponse.of(hotPlace, imageUrls);
    }

    @Transactional
    public HotPlaceResponse update(Long hotPlaceId, Long userId, HotPlaceUpdateRequest request) {
        HotPlace hotPlace = findHotPlace(hotPlaceId);
        verifySubmitter(hotPlace.getSubmitter().getId(), userId);

        hotPlace.update(request.name(), request.address(), request.latitude(),
                request.longitude(), request.category(), request.description());

        List<String> imageUrls;
        if (request.imageUrls() != null) {
            deletePhotos(hotPlaceId);
            imageUrls = savePhotos(hotPlace, request.imageUrls());
        } else {
            imageUrls = getImageUrls(hotPlaceId);
        }

        return HotPlaceResponse.of(hotPlace, imageUrls);
    }

    @Transactional
    public void delete(Long hotPlaceId, Long userId) {
        HotPlace hotPlace = findHotPlace(hotPlaceId);
        verifySubmitter(hotPlace.getSubmitter().getId(), userId);
        deletePhotos(hotPlaceId);
        hotPlaceRepository.delete(hotPlace);
    }

    // ─── 관리자 승인/반려 ─────────────────────────────────────

    @Transactional
    public HotPlaceResponse approve(Long hotPlaceId, Long adminId) {
        verifyAdmin(adminId);
        HotPlace hotPlace = findHotPlace(hotPlaceId);
        hotPlace.approve();
        return HotPlaceResponse.of(hotPlace, getImageUrls(hotPlaceId));
    }

    @Transactional
    public HotPlaceResponse reject(Long hotPlaceId, Long adminId) {
        verifyAdmin(adminId);
        HotPlace hotPlace = findHotPlace(hotPlaceId);
        hotPlace.reject();
        return HotPlaceResponse.of(hotPlace, getImageUrls(hotPlaceId));
    }

    public Page<HotPlaceSummaryResponse> getPendingHotPlaces(Long adminId, Pageable pageable) {
        verifyAdmin(adminId);
        return hotPlaceRepository.findAllByStatusOrderByCreatedAtDesc(HotPlaceStatus.PENDING, pageable)
                .map(hp -> HotPlaceSummaryResponse.of(hp, getThumbnail(hp.getId())));
    }

    // ─── 내부 헬퍼 ───────────────────────────────────────────

    private List<String> savePhotos(HotPlace hotPlace, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return Collections.emptyList();
        for (int i = 0; i < imageUrls.size(); i++) {
            hotPlacePhotoRepository.save(HotPlacePhoto.builder()
                    .hotPlace(hotPlace)
                    .imageUrl(imageUrls.get(i))
                    .displayOrder(i)
                    .build());
        }
        return imageUrls;
    }

    private void deletePhotos(Long hotPlaceId) {
        hotPlacePhotoRepository.deleteAllByHotPlaceId(hotPlaceId);
    }

    private List<String> getImageUrls(Long hotPlaceId) {
        return hotPlacePhotoRepository.findAllByHotPlaceIdOrderByDisplayOrderAsc(hotPlaceId)
                .stream().map(HotPlacePhoto::getImageUrl).toList();
    }

    private String getThumbnail(Long hotPlaceId) {
        return hotPlacePhotoRepository.findAllByHotPlaceIdOrderByDisplayOrderAsc(hotPlaceId)
                .stream().findFirst().map(HotPlacePhoto::getImageUrl).orElse(null);
    }

    private HotPlace findHotPlace(Long id) {
        return hotPlaceRepository.findById(id)
                .orElseThrow(() -> new GeneralException(ResponseCode.HOTPLACE_NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ResponseCode.USER_NOT_FOUND));
    }

    private void verifySubmitter(Long submitterId, Long userId) {
        if (!submitterId.equals(userId)) {
            throw new GeneralException(ResponseCode._FORBIDDEN);
        }
    }

    private void verifyAdmin(Long userId) {
        User user = findUser(userId);
        if (user.getUserRole() != UserRole.ADMIN) {
            throw new GeneralException(ResponseCode._FORBIDDEN);
        }
    }
}
