package com.trip.checklist.service;

import com.trip.checklist.dto.ChecklistItemCreateRequest;
import com.trip.checklist.dto.ChecklistItemResponse;
import com.trip.checklist.dto.ChecklistItemUpdateRequest;
import com.trip.checklist.dto.ChecklistTemplateResponse;
import com.trip.checklist.entity.ChecklistItem;
import com.trip.checklist.repository.ChecklistItemRepository;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
import com.trip.plan.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChecklistService {

    private final ChecklistItemRepository checklistItemRepository;
    private final PlanService planService;

    /**
     * planId 소유권 검증 — planId가 있으면 그 계획이 인증 사용자 본인 것인지 확인한다.
     * getDetail은 비소유/부재 시 PLAN_FORBIDDEN/PLAN_NOT_FOUND 예외를 던지므로,
     * 남의 계획 ID로 체크리스트를 묶거나 조회하는 것을 원천 차단한다.
     * planId가 null이면(=여행 미지정) 검증할 대상이 없으므로 통과시킨다.
     */
    private void verifyPlanOwnership(Long userId, Long planId) {
        if (planId != null) {
            planService.getDetail(userId, planId); // 비소유/부재 시 예외 → 조회·생성 거부
        }
    }

    /** 목록 조회 — planId가 있으면 해당 계획 한정, 없으면 전체 */
    @Transactional(readOnly = true)
    public List<ChecklistItemResponse> list(Long userId, Long planId) {
        // 보안: planId로 필터하기 전에 그 계획이 본인 소유인지 검증(남의 여행 항목 노출 방지)
        verifyPlanOwnership(userId, planId);
        List<ChecklistItem> items = (planId == null)
                ? checklistItemRepository.findByUserIdOrderBySortOrderAscIdAsc(userId)
                : checklistItemRepository.findByUserIdAndPlanIdOrderBySortOrderAscIdAsc(userId, planId);
        return items.stream()
                .map(ChecklistItemResponse::from)
                .toList();
    }

    /** 항목 생성 */
    public ChecklistItemResponse create(Long userId, ChecklistItemCreateRequest request) {
        // 보안: planId를 받을 때는 그 계획이 본인 소유인지 검증(남의 계획에 항목 묶기 방지)
        verifyPlanOwnership(userId, request.planId());
        ChecklistItem item = ChecklistItem.builder()
                .userId(userId)
                .planId(request.planId())
                .dayNo(request.dayNo())
                .title(request.title())
                .category(request.category())
                .checked(false)
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
                .build();
        return ChecklistItemResponse.from(checklistItemRepository.save(item));
    }

    /** 항목 부분 수정 — 소유자만 가능 */
    public ChecklistItemResponse update(Long userId, Long id, ChecklistItemUpdateRequest request) {
        ChecklistItem item = checklistItemRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new GeneralException(ResponseCode._FORBIDDEN));
        item.update(request.title(), request.category(), request.checked(), request.sortOrder());
        return ChecklistItemResponse.from(item);
    }

    /** 완료/미완료 토글 — 소유자만 가능 */
    public ChecklistItemResponse toggle(Long userId, Long id) {
        ChecklistItem item = checklistItemRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new GeneralException(ResponseCode._FORBIDDEN));
        item.toggle();
        return ChecklistItemResponse.from(item);
    }

    /** 항목 삭제 — 소유자만 가능 */
    public void delete(Long userId, Long id) {
        ChecklistItem item = checklistItemRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new GeneralException(ResponseCode._FORBIDDEN));
        checklistItemRepository.delete(item);
    }

    /** 내장 템플릿 전체 조회 */
    @Transactional(readOnly = true)
    public List<ChecklistTemplateResponse> templates() {
        return ChecklistTemplates.all();
    }

    /** 템플릿을 사용자/계획 항목으로 일괄 생성 */
    public List<ChecklistItemResponse> applyTemplate(Long userId, String templateKey, Long planId) {
        // 보안: 템플릿을 특정 계획에 붙일 때도 그 계획이 본인 소유인지 검증
        verifyPlanOwnership(userId, planId);
        ChecklistTemplateResponse template = ChecklistTemplates.byKey(templateKey)
                .orElseThrow(() -> new GeneralException(ResponseCode._BAD_REQUEST));

        List<ChecklistItem> toSave = new ArrayList<>();
        int order = 0;
        for (ChecklistTemplateResponse.Item templateItem : template.items()) {
            toSave.add(ChecklistItem.builder()
                    .userId(userId)
                    .planId(planId)
                    .dayNo(null)
                    .title(templateItem.title())
                    .category(templateItem.category())
                    .checked(false)
                    .sortOrder(order++)
                    .build());
        }

        return checklistItemRepository.saveAll(toSave).stream()
                .map(ChecklistItemResponse::from)
                .toList();
    }
}
