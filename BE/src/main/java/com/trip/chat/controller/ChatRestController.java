// Created: 2026-06-18 18:17:07
package com.trip.chat.controller;

import com.trip.chat.dto.MessageResponseDto;
import com.trip.chat.entity.ChatMessage;
import com.trip.chat.entity.enums.MessageType;
import com.trip.chat.repository.mongo.ChatMessageRepository;
import com.trip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// chat.enabled=true 일 때는 ChatHistoryController 가 이 경로를 담당한다(인증 포함).
// chat 기능이 비활성화된 경우에만 이 컨트롤러를 로드해 URL 충돌을 방지한다.
@ConditionalOnProperty(name = "chat.enabled", havingValue = "false")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<MessageResponseDto>> getMessages(@PathVariable Long roomId) {
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdOrderByTimestampDesc(roomId);

        // 닉네임 일괄 조회
        Set<Long> senderIds = messages.stream()
                .map(ChatMessage::getSenderId)
                .collect(Collectors.toSet());
        Map<Long, String> nicknameMap = userRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(u -> u.getId(), u -> u.getNickname()));

        List<MessageResponseDto> result = messages.stream()
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .map(m -> MessageResponseDto.builder()
                        .messageTSID(m.getMessageTSID())
                        .chatRoomId(m.getChatRoomId())
                        .senderId(m.getSenderId())
                        .senderNickname(nicknameMap.getOrDefault(m.getSenderId(), "알 수 없음"))
                        .messageType(m.getMessageType() != null ? m.getMessageType() : MessageType.TEXT)
                        .content(m.getContent())
                        .timestamp(m.getTimestamp())
                        .unreadCount(0)
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
