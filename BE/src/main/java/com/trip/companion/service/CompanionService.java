// Created: 2026-06-15 23:43:19
package com.trip.companion.service;

import com.trip.chat.entity.ChatRoom;
import com.trip.chat.entity.ChatRoomMembership;
import com.trip.chat.repository.ChatRoomMembershipRepository;
import com.trip.chat.repository.ChatRoomRepository;
import com.trip.companion.dto.*;
import com.trip.companion.entity.CompanionApplication;
import com.trip.companion.entity.CompanionPost;
import com.trip.companion.entity.enums.ApplicationStatus;
import com.trip.companion.entity.enums.CompanionStatus;
import com.trip.companion.repository.CompanionApplicationRepository;
import com.trip.companion.repository.CompanionPostRepository;
import com.trip.community.dto.CursorPageResponse;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.user.entity.User;
import com.trip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanionService {

    private final CompanionPostRepository companionPostRepository;
    private final CompanionApplicationRepository companionApplicationRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMembershipRepository chatRoomMembershipRepository;
    private final UserRepository userRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    // ─── 게시글 ───────────────────────────────────────────────

    @Transactional
    public CompanionPostResponse createPost(Long userId, CompanionPostCreateRequest request) {
        User author = findUser(userId);

        // 채팅방 생성 및 모임장 멤버십 등록 (chat_room_id NOT NULL 이므로 post 저장 전에 먼저 생성)
        String chatRoomTitle = request.title().length() > 18
                ? request.title().substring(0, 18)
                : request.title();

        ChatRoom chatRoom = ChatRoom.builder()
                .title(chatRoomTitle)
                .maxParticipants(request.maxMembers())
                .participationCount(1)
                .build();

        chatRoomRepository.save(chatRoom);

        CompanionPost post = CompanionPost.builder()
                .author(author)
                .title(request.title())
                .travelDate(request.travelDate())
                .region(request.region())
                .duration(request.duration())
                .maxMembers(request.maxMembers())
                .estimatedCost(request.estimatedCost())
                .description(request.description())
                .tags(CompanionPost.joinTags(request.tags()))
                .chatRoom(chatRoom)
                .build();

        companionPostRepository.save(post);

        ChatRoomMembership hostMembership = ChatRoomMembership.builder()
                .userId(userId)
                .chatRoom(chatRoom)
                .isHost(true)
                .isBanned(false)
                .build();

        chatRoomMembershipRepository.save(hostMembership);

        return buildResponse(post);
    }

    public CursorPageResponse<CompanionPostSummaryResponse> getPosts(Long cursor, int size) {
        // size=0/음수/과대값 방어 — 컨트롤러 검증을 우회한 호출에도 안전하게 clamp
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(0, safeSize + 1);

        List<CompanionPost> posts = cursor == null
                ? companionPostRepository.findAllByDeletedFalseOrderByIdDesc(pageable)
                : companionPostRepository.findAllByDeletedFalseAndIdLessThanOrderByIdDesc(cursor, pageable);

        boolean hasNext = posts.size() > safeSize;
        List<CompanionPost> content = hasNext ? posts.subList(0, safeSize) : posts;

        List<CompanionPostSummaryResponse> responses = content.stream()
                .map(post -> CompanionPostSummaryResponse.of(post, countCurrentMembers(post)))
                .toList();

        Long nextCursor = (hasNext && !content.isEmpty()) ? content.get(content.size() - 1).getId() : null;
        return new CursorPageResponse<>(responses, nextCursor, hasNext);
    }

    public CompanionPostResponse getPost(Long postId, Long currentUserId) {
        return buildResponse(findPost(postId), currentUserId);
    }

    @Transactional
    public CompanionPostResponse updatePost(Long postId, Long userId, CompanionPostUpdateRequest request) {
        CompanionPost post = findPost(postId);
        verifyAuthor(post, userId);

        post.update(request.title(), request.travelDate(), request.region(),
                request.duration(), request.maxMembers(), request.estimatedCost(), request.description());

        return buildResponse(post);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        CompanionPost post = findPost(postId);
        verifyAuthor(post, userId);
        post.delete();
    }

    @Transactional
    public void closePost(Long postId, Long userId) {
        CompanionPost post = findPost(postId);
        verifyAuthor(post, userId);
        post.close();
    }

    // ─── 동행 신청 ────────────────────────────────────────────

    @Transactional
    public CompanionApplicationResponse apply(Long postId, Long userId, String message) {
        CompanionPost post = findPost(postId);
        User applicant = findUser(userId);

        if (post.getAuthor().getId().equals(userId)) {
            throw new GeneralException(ResponseCode.COMPANION_SELF_APPLY);
        }
        if (post.getStatus() != CompanionStatus.OPEN) {
            throw new GeneralException(ResponseCode.COMPANION_POST_CLOSED);
        }
        // REJECTED 상태는 재신청 허용 — PENDING·APPROVED 상태일 때만 중복으로 간주
        if (companionApplicationRepository.existsByCompanionPostAndApplicantAndStatusNot(
                post, applicant, com.trip.companion.entity.enums.ApplicationStatus.REJECTED)) {
            throw new GeneralException(ResponseCode.COMPANION_ALREADY_APPLIED);
        }

        CompanionApplication application = CompanionApplication.builder()
                .companionPost(post)
                .applicant(applicant)
                .message(message != null && !message.isBlank() ? message.trim() : null)
                .build();

        try {
            // 동시 중복 신청은 DB 제약 충돌로 잡아 409로 변환 (존재 검사를 통과한 레이스 방어)
            companionApplicationRepository.saveAndFlush(application);
        } catch (DataIntegrityViolationException e) {
            throw new GeneralException(ResponseCode.COMPANION_DUPLICATE_APPLY, e);
        }

        // 모집글 작성자에게 알림 (커밋 후 적재)
        eventPublisher.publishEvent(new com.trip.notification.event.NotificationEvent(
                post.getAuthor().getId(),
                "companion",
                "동행 신청이 도착했어요",
                "‘" + post.getTitle() + "’에 " + applicant.getNickname() + "님이 신청했어요.",
                "/community?tab=companion"));

        return CompanionApplicationResponse.of(application);
    }

    public List<CompanionApplicationResponse> getApplications(Long postId, Long userId) {
        CompanionPost post = findPost(postId);
        verifyAuthor(post, userId);

        return companionApplicationRepository.findAllByCompanionPost(post).stream()
                .map(CompanionApplicationResponse::of)
                .toList();
    }

    @Transactional
    public CompanionApplicationResponse approveApplication(Long postId, Long applicationId, Long userId) {
        // 동시 승인 직렬화: post 행에 비관적 쓰기 락을 걸어 정원 체크~멤버십 등록을 원자적으로 처리
        CompanionPost post = companionPostRepository.findByIdForUpdate(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new GeneralException(ResponseCode.COMPANION_POST_NOT_FOUND));
        verifyAuthor(post, userId);

        CompanionApplication application = findApplication(applicationId, post);

        // 정원 초과 방지 — 락 보유 상태에서 현재 인원을 카운트하여 직렬화
        if (post.getChatRoom() != null) {
            int currentMembers = chatRoomMembershipRepository
                    .countByChatRoomId(post.getChatRoom().getId());
            if (currentMembers >= post.getMaxMembers()) {
                throw new GeneralException(ResponseCode.COMPANION_FULL);
            }
        }

        application.approve(); // PENDING이 아니면 COMPANION_ALREADY_PROCESSED 예외

        if (post.getChatRoom() != null) {
            Long applicantId = application.getApplicant().getId();
            boolean alreadyMember = chatRoomMembershipRepository
                    .findByUserIdAndChatRoom(applicantId, post.getChatRoom())
                    .isPresent();

            if (!alreadyMember) {
                ChatRoomMembership membership = ChatRoomMembership.builder()
                        .userId(applicantId)
                        .chatRoom(post.getChatRoom())
                        .isHost(false)
                        .isBanned(false)
                        .build();
                try {
                    // (chat_room_id, user_id) unique 충돌은 동시 승인 레이스 → 409
                    chatRoomMembershipRepository.saveAndFlush(membership);
                } catch (DataIntegrityViolationException e) {
                    throw new GeneralException(ResponseCode.COMPANION_FULL, e);
                }
                post.getChatRoom().setParticipationCount(post.getChatRoom().getParticipationCount() + 1);
            }
        }

        // 신청자에게 수락 알림 (커밋 후 적재)
        eventPublisher.publishEvent(new com.trip.notification.event.NotificationEvent(
                application.getApplicant().getId(),
                "companion",
                "동행 신청이 수락되었어요",
                "‘" + post.getTitle() + "’ 동행 신청이 수락되었어요.",
                "/community?tab=companion"));

        return CompanionApplicationResponse.of(application);
    }

    @Transactional
    public CompanionApplicationResponse rejectApplication(Long postId, Long applicationId, Long userId) {
        CompanionPost post = findPost(postId);
        verifyAuthor(post, userId);

        CompanionApplication application = findApplication(applicationId, post);
        application.reject();

        // 신청자에게 반려 알림 (커밋 후 적재)
        eventPublisher.publishEvent(new com.trip.notification.event.NotificationEvent(
                application.getApplicant().getId(),
                "companion",
                "동행 신청 결과 안내",
                "‘" + post.getTitle() + "’ 동행 신청이 반려되었어요.",
                "/community?tab=companion"));

        return CompanionApplicationResponse.of(application);
    }

    @Transactional
    public void cancelApplication(Long postId, Long applicationId, Long userId) {
        CompanionPost post = findPost(postId);
        CompanionApplication application = findApplication(applicationId, post);

        if (!application.getApplicant().getId().equals(userId)) {
            throw new GeneralException(ResponseCode._FORBIDDEN);
        }

        // 승인된 신청은 이미 채팅방 멤버십이 생성되었으므로 단순 삭제 시 멤버십이 잔존한다.
        // → PENDING(또는 REJECTED) 상태만 취소 허용, APPROVED는 거부.
        if (application.getStatus() == ApplicationStatus.APPROVED) {
            throw new GeneralException(ResponseCode.COMPANION_APPROVED_CANCEL);
        }

        companionApplicationRepository.delete(application);
    }

    // ─── 내부 헬퍼 ───────────────────────────────────────────

    private CompanionPost findPost(Long postId) {
        return companionPostRepository.findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new GeneralException(ResponseCode.COMPANION_POST_NOT_FOUND));
    }

    private CompanionApplication findApplication(Long applicationId, CompanionPost post) {
        return companionApplicationRepository.findByIdAndCompanionPost(applicationId, post)
                .orElseThrow(() -> new GeneralException(ResponseCode.COMPANION_APPLICATION_NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ResponseCode.USER_NOT_FOUND));
    }

    private void verifyAuthor(CompanionPost post, Long userId) {
        if (!post.getAuthor().getId().equals(userId)) {
            throw new GeneralException(ResponseCode._FORBIDDEN);
        }
    }

    public List<MyCompanionRoomResponse> getMyRooms(Long userId) {
        List<ChatRoomMembership> memberships = chatRoomMembershipRepository.findByUserId(userId);
        List<Long> chatRoomIds = memberships.stream()
                .map(m -> m.getChatRoom().getId())
                .toList();
        if (chatRoomIds.isEmpty()) return List.of();

        Map<Long, Boolean> hostMap = memberships.stream()
                .collect(Collectors.toMap(m -> m.getChatRoom().getId(), ChatRoomMembership::getIsHost));

        return companionPostRepository.findAllByDeletedFalseAndChatRoomIdIn(chatRoomIds).stream()
                .map(post -> MyCompanionRoomResponse.of(post, Boolean.TRUE.equals(hostMap.get(post.getChatRoom().getId()))))
                .toList();
    }

    private int countCurrentMembers(CompanionPost post) {
        return post.getChatRoom() != null
                ? chatRoomMembershipRepository.countByChatRoomId(post.getChatRoom().getId())
                : 0;
    }

    private CompanionPostResponse buildResponse(CompanionPost post) {
        return buildResponse(post, null);
    }

    private CompanionPostResponse buildResponse(CompanionPost post, Long currentUserId) {
        int currentMembers = countCurrentMembers(post);
        int pendingCount = companionApplicationRepository
                .countByCompanionPostAndStatus(post, ApplicationStatus.PENDING);
        int approvedCount = companionApplicationRepository
                .countByCompanionPostAndStatus(post, ApplicationStatus.APPROVED);

        // 현재 사용자가 이미 신청(PENDING/APPROVED)한 상태인지 — REJECTED는 미신청으로 간주
        boolean isApplied = false;
        Long myApplicationId = null;
        ApplicationStatus myApplicationStatus = null;
        if (currentUserId != null) {
            CompanionApplication active = companionApplicationRepository
                    .findFirstByCompanionPostAndApplicant_IdAndStatusNot(
                            post, currentUserId, ApplicationStatus.REJECTED)
                    .orElse(null);
            if (active != null) {
                isApplied = true;
                myApplicationId = active.getId();
                myApplicationStatus = active.getStatus();
            }
        }

        return CompanionPostResponse.of(post, currentMembers, pendingCount, approvedCount,
                isApplied, myApplicationId, myApplicationStatus);
    }
}
