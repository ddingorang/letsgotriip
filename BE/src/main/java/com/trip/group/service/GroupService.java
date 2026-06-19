package com.trip.group.service;

import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.group.dto.GroupCreateRequest;
import com.trip.group.dto.GroupDiscountResponse;
import com.trip.group.dto.GroupMemberResponse;
import com.trip.group.dto.GroupResponse;
import com.trip.group.entity.GroupMember;
import com.trip.group.entity.TravelGroup;
import com.trip.group.repository.GroupMemberRepository;
import com.trip.group.repository.TravelGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

    private final TravelGroupRepository travelGroupRepository;
    private final GroupMemberRepository groupMemberRepository;

    /** 단체할인 데모 목록 — 실제 제휴 연동 없이 고정 데이터로 제공 (demo=true) */
    private static final List<GroupDiscountResponse> DISCOUNTS = List.of(
            new GroupDiscountResponse("○○리조트 단체 15% 할인",
                    "10인 이상 예약 시 객실 요금 15% 할인", "○○리조트", 15, true),
            new GroupDiscountResponse("△△렌터카 단체 20% 할인",
                    "5대 이상 동시 대여 시 대여료 20% 할인", "△△렌터카", 20, true),
            new GroupDiscountResponse("□□테마파크 단체 입장권 25% 할인",
                    "20인 이상 단체 입장권 25% 할인", "□□테마파크", 25, true),
            new GroupDiscountResponse("◇◇한식당 단체 식사 10% 할인",
                    "8인 이상 단체 예약 시 식사비 10% 할인", "◇◇한식당", 10, true),
            new GroupDiscountResponse("☆☆고속버스 단체 12% 할인",
                    "15인 이상 단체 예매 시 운임 12% 할인", "☆☆고속버스", 12, true)
    );

    // ─── 그룹 ──────────────────────────────────────────────────

    @Transactional
    public GroupResponse create(Long userId, GroupCreateRequest request) {
        TravelGroup group = TravelGroup.builder()
                .ownerId(userId)
                .name(request.name())
                .description(request.description())
                .maxMembers(request.maxMembers() != null ? request.maxMembers() : 10)
                .build();

        travelGroupRepository.save(group);

        GroupMember owner = GroupMember.builder()
                .groupId(group.getId())
                .userId(userId)
                .role(GroupMember.ROLE_OWNER)
                .build();
        groupMemberRepository.save(owner);

        return GroupResponse.from(group, groupMemberRepository.countByGroupId(group.getId()));
    }

    /** 내가 속한(소유 포함) 그룹 목록 */
    public List<GroupResponse> list(Long userId) {
        return groupMemberRepository.findByUserId(userId).stream()
                .map(GroupMember::getGroupId)
                .distinct()
                .map(travelGroupRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(g -> GroupResponse.from(g, groupMemberRepository.countByGroupId(g.getId())))
                .toList();
    }

    public GroupResponse get(Long userId, Long id) {
        TravelGroup group = findGroup(id);
        // IDOR 방어: 그룹 멤버(소유자 포함)만 상세 조회 가능
        requireMember(id, userId);
        return GroupResponse.from(group, groupMemberRepository.countByGroupId(group.getId()));
    }

    @Transactional
    public void join(Long userId, Long groupId) {
        TravelGroup group = findGroup(groupId);

        // 중복 가입 방지
        if (groupMemberRepository.findByGroupIdAndUserId(groupId, userId).isPresent()) {
            throw new GeneralException(ResponseCode._BAD_REQUEST);
        }
        // 정원 초과 방지
        if (groupMemberRepository.countByGroupId(groupId) >= group.getMaxMembers()) {
            throw new GeneralException(ResponseCode._BAD_REQUEST);
        }

        GroupMember member = GroupMember.builder()
                .groupId(groupId)
                .userId(userId)
                .role(GroupMember.ROLE_MEMBER)
                .build();
        groupMemberRepository.save(member);
    }

    @Transactional
    public void leave(Long userId, Long groupId) {
        // 그룹 존재 확인
        findGroup(groupId);

        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new GeneralException(ResponseCode._BAD_REQUEST));

        // 소유자는 탈퇴 불가 (그룹 해체는 별도 정책)
        if (GroupMember.ROLE_OWNER.equals(member.getRole())) {
            throw new GeneralException(ResponseCode._BAD_REQUEST);
        }

        groupMemberRepository.delete(member);
    }

    public List<GroupMemberResponse> members(Long userId, Long groupId) {
        findGroup(groupId);
        // IDOR 방어: 그룹 멤버(소유자 포함)만 멤버 목록 조회 가능
        requireMember(groupId, userId);
        return groupMemberRepository.findByGroupId(groupId).stream()
                .map(GroupMemberResponse::from)
                .toList();
    }

    // ─── 단체할인 (데모) ───────────────────────────────────────

    public List<GroupDiscountResponse> discounts() {
        return DISCOUNTS;
    }

    // ─── 내부 헬퍼 ─────────────────────────────────────────────

    private TravelGroup findGroup(Long id) {
        return travelGroupRepository.findById(id)
                .orElseThrow(() -> new GeneralException(ResponseCode._BAD_REQUEST));
    }

    /** 비멤버의 그룹 상세/멤버 접근 차단 (IDOR 방어). 멤버가 아니면 403. */
    private void requireMember(Long groupId, Long userId) {
        if (groupMemberRepository.findByGroupIdAndUserId(groupId, userId).isEmpty()) {
            throw new GeneralException(ResponseCode._FORBIDDEN);
        }
    }
}
