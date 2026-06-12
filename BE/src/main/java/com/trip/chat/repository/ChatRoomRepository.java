package com.trip.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trip.chat.entity.ChatRoom;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
}