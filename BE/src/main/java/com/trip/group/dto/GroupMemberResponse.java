package com.trip.group.dto;

import com.trip.group.entity.GroupMember;

import java.time.LocalDateTime;

public record GroupMemberResponse(
        Long userId,
        String role,
        LocalDateTime joinedAt
) {
    public static GroupMemberResponse from(GroupMember member) {
        return new GroupMemberResponse(
                member.getUserId(),
                member.getRole(),
                member.getJoinedAt()
        );
    }
}
