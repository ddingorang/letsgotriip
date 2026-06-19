package com.trip.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trip.chat.dto.ChatRoomParticipantsResponse;
import com.trip.chat.dto.MessageResponseDto;
import com.trip.chat.dto.MessageSendRequestDto;
import com.trip.chat.dto.converter.MessageDtoConverter;
import com.trip.chat.entity.ChatMessage;
import com.trip.chat.entity.ChatRoom;
import com.trip.chat.entity.ChatRoomMembership;
import com.trip.chat.repository.ChatRoomMembershipRepository;
import com.trip.chat.repository.ChatRoomRepository;
import com.trip.chat.repository.mongo.ChatMessageRepository;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.user.entity.User;
import com.trip.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.trip.chat.dto.converter.MessageDtoIdInjector.withGeneratedMessageId;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "chat.enabled", havingValue = "true", matchIfMissing = true)
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate; // STOMP 브로커(/topic) 목적지로 메시지 브로드캐스트
    private final UserRepository userRepository;
    private final ChatRoomMembershipRepository chatRoomMembershipRepository;
    private final ChatRoomRepository chatRoomRepository;

    public void sendMessage(final MessageSendRequestDto messageSendRequest, final Long senderId, final Long chatRoomId) {

        log.info("[1/3] 메시지 전송 프로세스 시작. senderId: {}, chatRoomId: {}", senderId, chatRoomId);

        // STOMP 송신 인가 — 발신자가 해당 방의 활성 멤버(미탈퇴·미강퇴)인지 검증한다.
        // 페이로드의 chatRoomId는 스푸핑 가능하므로 신뢰하지 않고, STOMP 경로(@DestinationVariable)
        // 에서 전달된 chatRoomId만 권위 있는 값으로 사용한다(영속/브로드캐스트 전에 검증).
        boolean isActiveMember = chatRoomMembershipRepository.findByChatRoomId(chatRoomId).stream()
                .filter(m -> m.getUserId().equals(senderId))
                .anyMatch(ChatRoomMembership::isActiveMember);
        if (!isActiveMember) {
            log.warn("STOMP 송신 인가 거부 — 비멤버 발신 시도. senderId: {}, chatRoomId: {}", senderId, chatRoomId);
            throw new GeneralException(ResponseCode._FORBIDDEN);
        }

        String senderNickname = userRepository.findById(senderId)
                .map(u -> u.getNickname())
                .orElse("알 수 없음");

        // 클라이언트 요청 DTO에 서버 생성 값(TSID, senderId, timestamp 등) 주입.
        // 권위 있는 chatRoomId(경로 변수)를 사용해 페이로드 roomId 스푸핑을 무력화한다.
        final MessageResponseDto messageDtoWithId =
                withGeneratedMessageId(messageSendRequest, senderId, senderNickname, chatRoomId);
        log.info("[2/3] 메시지 ID 생성 완료. messageTSID: {}", messageDtoWithId.messageTSID());

        // MongoDB에 채팅 메시지 영구 저장
        final ChatMessage chatMessage = MessageDtoConverter.toMessage(messageDtoWithId);
        chatMessageRepository.save(chatMessage);
        log.info("[3/3] MongoDB 저장 완료. messageTSID: {}", chatMessage.getMessageTSID());

        // STOMP 브로커(/topic) 목적지로 명시 발행 → 구독 중인 클라이언트에게 브로드캐스트.
        // (이전의 rabbitTemplate.convertAndSend(payload) 단일 인자 호출은 exchange/routing key가
        //  지정되지 않아 실제 라우팅되지 않는 dead code였으므로 제거)
        final String destination = "/topic/chat.room." + chatRoomId;
        messagingTemplate.convertAndSend(destination, messageDtoWithId);
        log.info("메시지 전송 완료. destination: {}, messageTSID: {}", destination, chatMessage.getMessageTSID());
    }

    /**
     * 채팅방 메시지 히스토리 조회 (오래된 → 최신 순).
     * 새로고침/재입장 시 이전 대화를 복원하기 위한 REST 경로.
     */
    public List<MessageResponseDto> getHistory(final Long chatRoomId) {
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdOrderByTimestampDesc(chatRoomId);

        // 발신자 닉네임 일괄 조회(메시지마다 쿼리하지 않도록 캐싱)
        Map<Long, String> nicknameCache = new HashMap<>();

        // timestamp DESC로 조회되므로 화면 표시용으로 ASC(오래된→최신)로 뒤집는다.
        return messages.stream()
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .map(m -> MessageResponseDto.builder()
                        .messageTSID(m.getMessageTSID())
                        .correlationId(null)
                        .chatRoomId(m.getChatRoomId())
                        .senderId(m.getSenderId())
                        .senderNickname(nicknameCache.computeIfAbsent(m.getSenderId(), id ->
                                userRepository.findById(id).map(User::getNickname).orElse("알 수 없음")))
                        .messageType(m.getMessageType())
                        .content(m.getContent())
                        .timestamp(m.getTimestamp())
                        .unreadCount(0)
                        .build())
                .toList();
    }

    /**
     * 채팅방 나가기 — 본인 멤버십의 leftAt 을 현재 시각으로 설정(소프트 탈퇴).
     * - 멤버가 아니면 403
     * - 방장(host)은 나갈 수 없음 → 400(_BAD_REQUEST)
     * - 이미 나간 상태면 멱등 처리(그대로 통과)
     */
    @Transactional
    public void leaveRoom(final Long chatRoomId, final Long userId) {
        ChatRoomMembership membership = chatRoomMembershipRepository.findByChatRoomId(chatRoomId).stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new GeneralException(ResponseCode._FORBIDDEN));

        if (Boolean.TRUE.equals(membership.getIsHost())) {
            // 방장은 채팅방을 나갈 수 없다(동행 게시글 삭제/취소로만 정리).
            throw new GeneralException(ResponseCode._BAD_REQUEST, "방장은 채팅방을 나갈 수 없습니다.");
        }

        if (membership.getLeftAt() == null) {
            membership.leave(LocalDateTime.now());
        }
        log.info("채팅방 나가기 완료. chatRoomId: {}, userId: {}", chatRoomId, userId);
    }

    /**
     * 채팅방 참여자(활성 멤버) 목록/인원수 조회.
     * 본인이 활성 멤버여야 조회 가능.
     */
    @Transactional(readOnly = true)
    public ChatRoomParticipantsResponse getParticipants(final Long chatRoomId, final Long userId) {
        List<ChatRoomMembership> memberships = chatRoomMembershipRepository.findByChatRoomId(chatRoomId);

        boolean isActiveMember = memberships.stream()
                .anyMatch(m -> m.getUserId().equals(userId) && m.isActiveMember());
        if (!isActiveMember) {
            throw new GeneralException(ResponseCode._FORBIDDEN);
        }

        // 요청자가 방장인지(활성 멤버 + isHost) — FE 관리 버튼 게이팅용
        boolean viewerIsHost = memberships.stream()
                .anyMatch(m -> m.getUserId().equals(userId)
                        && m.isActiveMember()
                        && Boolean.TRUE.equals(m.getIsHost()));

        List<ChatRoomParticipantsResponse.Participant> participants = memberships.stream()
                .filter(ChatRoomMembership::isActiveMember)
                .map(m -> ChatRoomParticipantsResponse.Participant.builder()
                        .userId(m.getUserId())
                        .nickname(userRepository.findById(m.getUserId())
                                .map(User::getNickname).orElse("알 수 없음"))
                        .isHost(Boolean.TRUE.equals(m.getIsHost()))
                        .build())
                .toList();

        return ChatRoomParticipantsResponse.builder()
                .chatRoomId(chatRoomId)
                .count(participants.size())
                .viewerIsHost(viewerIsHost)
                .participants(participants)
                .build();
    }

    // ─── 방 설정 / 참여자 관리 ────────────────────────────────────

    /**
     * /topic/chat.room.{roomId} 로 비영속 SYSTEM 이벤트를 브로드캐스트한다.
     * 일반 채팅 메시지(MongoDB 영속)와 달리 저장하지 않으며, FE가 멤버 목록/방정보를
     * 실시간 갱신하거나 강퇴 대상이 연결을 끊는 데 사용한다.
     */
    private void broadcastSystemEvent(final Long chatRoomId, final String event, final Map<String, Object> payload) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "SYSTEM");
        message.put("event", event);
        message.put("chatRoomId", chatRoomId);
        message.put("timestamp", LocalDateTime.now());
        if (payload != null) {
            message.putAll(payload);
        }
        final String destination = "/topic/chat.room." + chatRoomId;
        messagingTemplate.convertAndSend(destination, message);
        log.info("SYSTEM 이벤트 브로드캐스트. destination: {}, event: {}", destination, event);
    }

    /** 방장 멤버십을 조회한다(활성 방장만). 아니면 _FORBIDDEN. */
    private ChatRoomMembership requireHost(final Long chatRoomId, final Long userId) {
        return chatRoomMembershipRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .filter(ChatRoomMembership::isActiveMember)
                .filter(m -> Boolean.TRUE.equals(m.getIsHost()))
                .orElseThrow(() -> new GeneralException(ResponseCode._FORBIDDEN));
    }

    /**
     * 방 정보(제목/소개) 수정 — 방장만. ROOM_UPDATED 브로드캐스트.
     */
    @Transactional
    public void updateRoom(final Long chatRoomId, final Long userId,
                           final String title, final String description) {
        requireHost(chatRoomId, userId);

        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ResponseCode._BAD_REQUEST, "존재하지 않는 채팅방입니다."));

        room.setTitle(title);
        // description 은 선택값 — null 이면 유지가 아니라 명시적으로 비울 수 있도록 그대로 반영
        room.setDescription(description);

        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("description", description);
        broadcastSystemEvent(chatRoomId, "ROOM_UPDATED", payload);

        log.info("채팅방 정보 수정 완료. chatRoomId: {}, by userId: {}", chatRoomId, userId);
    }

    /**
     * 본인 멤버십 음소거 토글. 활성 멤버여야 함(아니면 403).
     * 브로드캐스트 없음(본인 클라이언트 로컬 상태).
     */
    @Transactional
    public void updateMute(final Long chatRoomId, final Long userId, final boolean muted) {
        ChatRoomMembership membership = chatRoomMembershipRepository
                .findByChatRoomIdAndUserId(chatRoomId, userId)
                .filter(ChatRoomMembership::isActiveMember)
                .orElseThrow(() -> new GeneralException(ResponseCode._FORBIDDEN));
        membership.setMuted(muted);
        log.info("음소거 토글. chatRoomId: {}, userId: {}, muted: {}", chatRoomId, userId, muted);
    }

    /**
     * 참여자 강퇴 — 방장만. 대상 멤버십을 isBanned/bannedAt/leftAt 설정하고
     * participationCount 감소. 방장 자신/비활성(비멤버) 대상은 _BAD_REQUEST.
     * PARTICIPANT_KICKED 브로드캐스트(kicked userId 포함).
     */
    @Transactional
    public void kickParticipant(final Long chatRoomId, final Long hostUserId, final Long targetUserId) {
        requireHost(chatRoomId, hostUserId);

        if (hostUserId.equals(targetUserId)) {
            throw new GeneralException(ResponseCode._BAD_REQUEST, "방장은 자신을 강퇴할 수 없습니다.");
        }

        ChatRoomMembership target = chatRoomMembershipRepository
                .findByChatRoomIdAndUserId(chatRoomId, targetUserId)
                .filter(ChatRoomMembership::isActiveMember)
                .orElseThrow(() -> new GeneralException(ResponseCode._BAD_REQUEST, "강퇴할 참여자가 아닙니다."));

        target.ban(LocalDateTime.now());

        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ResponseCode._BAD_REQUEST, "존재하지 않는 채팅방입니다."));
        room.setParticipationCount(Math.max(0, room.getParticipationCount() - 1));

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", targetUserId); // 강퇴 대상 — 해당 클라이언트가 연결을 끊는 데 사용
        broadcastSystemEvent(chatRoomId, "PARTICIPANT_KICKED", payload);

        log.info("참여자 강퇴 완료. chatRoomId: {}, target: {}, by host: {}", chatRoomId, targetUserId, hostUserId);
    }

    /**
     * 참여자 초대/추가 — 방장만. nickname 또는 email 로 사용자 식별(email 우선).
     * 이미 활성 멤버면/정원 초과면 _BAD_REQUEST. 이전 강퇴/탈퇴자는 재활성.
     * PARTICIPANT_JOINED 브로드캐스트.
     */
    @Transactional
    public void inviteParticipant(final Long chatRoomId, final Long hostUserId,
                                  final String nickname, final String email) {
        requireHost(chatRoomId, hostUserId);

        final User invitee = resolveInvitee(nickname, email);

        // 정원 체크~멤버십 등록 직렬화를 위해 방 행에 비관적 쓰기 락
        ChatRoom room = chatRoomRepository.findByIdForUpdate(chatRoomId)
                .orElseThrow(() -> new GeneralException(ResponseCode._BAD_REQUEST, "존재하지 않는 채팅방입니다."));

        ChatRoomMembership existing = chatRoomMembershipRepository
                .findByChatRoomIdAndUserId(chatRoomId, invitee.getId())
                .orElse(null);

        if (existing != null && existing.isActiveMember()) {
            throw new GeneralException(ResponseCode._BAD_REQUEST, "이미 참여 중인 사용자입니다.");
        }

        // 정원 초과 방지 — 락 보유 상태에서 활성 인원 카운트
        long activeCount = chatRoomMembershipRepository.findByChatRoomId(chatRoomId).stream()
                .filter(ChatRoomMembership::isActiveMember)
                .count();
        if (activeCount >= room.getMaxParticipants()) {
            throw new GeneralException(ResponseCode._BAD_REQUEST, "정원이 가득 찼습니다.");
        }

        if (existing != null) {
            // 이전 강퇴/탈퇴자 재활성
            existing.reactivate();
        } else {
            ChatRoomMembership membership = ChatRoomMembership.builder()
                    .userId(invitee.getId())
                    .chatRoom(room)
                    .isHost(false)
                    .isBanned(false)
                    .build();
            try {
                chatRoomMembershipRepository.saveAndFlush(membership);
            } catch (DataIntegrityViolationException e) {
                // (chat_room_id, user_id) unique 충돌 = 동시 초대 레이스
                throw new GeneralException(ResponseCode._BAD_REQUEST, e);
            }
        }
        room.setParticipationCount(room.getParticipationCount() + 1);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", invitee.getId());
        payload.put("nickname", invitee.getNickname());
        broadcastSystemEvent(chatRoomId, "PARTICIPANT_JOINED", payload);

        log.info("참여자 초대 완료. chatRoomId: {}, invitee: {}, by host: {}", chatRoomId, invitee.getId(), hostUserId);
    }

    /** email(우선) 또는 nickname 으로 초대 대상 사용자를 조회한다. */
    private User resolveInvitee(final String nickname, final String email) {
        if (email != null && !email.isBlank()) {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new GeneralException(ResponseCode.USER_NOT_FOUND));
        }
        if (nickname != null && !nickname.isBlank()) {
            // UserRepository(소유 외 파일)에 finder가 없어 findByNickname을 추가할 수 없으므로
            // 닉네임은 전체 스캔으로 매칭한다(초대는 방장 전용·저빈도 관리 동작이라 허용 가능한 비용).
            return userRepository.findAll().stream()
                    .filter(u -> nickname.equals(u.getNickname()))
                    .findFirst()
                    .orElseThrow(() -> new GeneralException(ResponseCode.USER_NOT_FOUND));
        }
        throw new GeneralException(ResponseCode._BAD_REQUEST, "닉네임 또는 이메일이 필요합니다.");
    }

    /**
     * 방장 위임 — 현 방장만. 대상이 활성 멤버여야 함. 기존 방장 isHost=false, 신규 isHost=true.
     * HOST_CHANGED 브로드캐스트.
     */
    @Transactional
    public void transferHost(final Long chatRoomId, final Long currentHostUserId, final Long newHostUserId) {
        ChatRoomMembership currentHost = requireHost(chatRoomId, currentHostUserId);

        if (currentHostUserId.equals(newHostUserId)) {
            throw new GeneralException(ResponseCode._BAD_REQUEST, "이미 방장입니다.");
        }

        ChatRoomMembership newHost = chatRoomMembershipRepository
                .findByChatRoomIdAndUserId(chatRoomId, newHostUserId)
                .filter(ChatRoomMembership::isActiveMember)
                .orElseThrow(() -> new GeneralException(ResponseCode._BAD_REQUEST, "활성 참여자만 방장이 될 수 있습니다."));

        currentHost.changeHost(false);
        newHost.changeHost(true);

        Map<String, Object> payload = new HashMap<>();
        payload.put("previousHostUserId", currentHostUserId);
        payload.put("newHostUserId", newHostUserId);
        broadcastSystemEvent(chatRoomId, "HOST_CHANGED", payload);

        log.info("방장 위임 완료. chatRoomId: {}, {} -> {}", chatRoomId, currentHostUserId, newHostUserId);
    }
}
