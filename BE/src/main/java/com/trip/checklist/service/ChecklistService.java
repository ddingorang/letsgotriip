package com.trip.checklist.service;

import com.trip.checklist.dto.ChecklistItemCreateRequest;
import com.trip.checklist.dto.ChecklistItemResponse;
import com.trip.checklist.dto.ChecklistItemUpdateRequest;
import com.trip.checklist.dto.ChecklistTemplateResponse;
import com.trip.checklist.entity.ChecklistItem;
import com.trip.checklist.repository.ChecklistItemRepository;
import com.trip.global.error.GeneralException;
import com.trip.global.error.ResponseCode;
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

    /** 목록 조회 — planId가 있으면 해당 계획 한정, 없으면 전체 */
    @Transactional(readOnly = true)
    public List<ChecklistItemResponse> list(Long userId, Long planId) {
        List<ChecklistItem> items = (planId == null)
                ? checklistItemRepository.findByUserIdOrderBySortOrderAscIdAsc(userId)
                : checklistItemRepository.findByUserIdAndPlanIdOrderBySortOrderAscIdAsc(userId, planId);
        return items.stream()
                .map(ChecklistItemResponse::from)
                .toList();
    }

    /** 항목 생성 */
    public ChecklistItemResponse create(Long userId, ChecklistItemCreateRequest request) {
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
