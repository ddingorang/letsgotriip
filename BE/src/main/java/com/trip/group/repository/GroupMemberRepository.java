package com.trip.group.repository;

import com.trip.group.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    List<GroupMember> findByGroupId(Long groupId);

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    int countByGroupId(Long groupId);

    List<GroupMember> findByUserId(Long userId);

    /**
     * 여러 그룹의 멤버 수를 한 번에 집계 (N+1 방지).
     * 반환: [groupId, memberCount] 행 목록 — 멤버가 0인 그룹은 결과에 포함되지 않음.
     */
    @Query("select m.groupId, count(m) from GroupMember m where m.groupId in :groupIds group by m.groupId")
    List<Object[]> countByGroupIdIn(@Param("groupIds") Collection<Long> groupIds);
}
