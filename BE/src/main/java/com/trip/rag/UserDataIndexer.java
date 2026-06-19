package com.trip.rag;

import com.trip.plan.entity.TripDay;
import com.trip.plan.entity.TripPlace;
import com.trip.plan.entity.TripPlan;
import com.trip.plan.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용자의 저장된 여행 계획(plan)을 요약 텍스트로 변환해 RAG 벡터스토어에 적재한다.
 *
 * <p>어시스턴트가 "내 지난 여행", "예전에 갔던 곳"처럼 사용자의 여행기록을 참조해
 * 답변/일정 제안을 할 수 있도록 한다. plan 1건 = 문서 1건(docId="plan:{id}")으로 인덱싱된다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDataIndexer {

    private static final String SOURCE_LABEL = "내 여행기록";
    private static final String ANALYSIS_SOURCE_LABEL = "내 분석데이터";
    private static final int MAX_PLANS = 50;

    private final PlanRepository planRepository;
    private final IngestionService ingestionService;

    /** 사용자의 모든 plan을 재인덱싱한다(읽기 트랜잭션 내에서 lazy 컬렉션 로딩). */
    @Transactional(readOnly = true)
    public void indexUserPlans(Long userId) {
        Pageable pageable = PageRequest.of(0, MAX_PLANS);
        List<TripPlan> plans = planRepository
                .findByUserIdOrderByUpdatedAtDesc(userId, pageable)
                .getContent();

        for (TripPlan plan : plans) {
            try {
                String text = toSummaryText(plan);
                ingestionService.ingest(userId, "plan:" + plan.getId(), SOURCE_LABEL, text);
            } catch (Exception e) {
                log.warn("plan 인덱싱 실패 — planId={}, error={}", plan.getId(), e.getMessage());
            }
        }
    }

    /** 단일 plan 인덱싱(plan 저장 직후 호출용). */
    @Transactional(readOnly = true)
    public void indexPlan(Long userId, Long planId) {
        planRepository.findByIdWithDays(planId).ifPresent(plan -> {
            if (!plan.getUserId().equals(userId)) {
                return;
            }
            String text = toSummaryText(plan);
            ingestionService.ingest(userId, "plan:" + plan.getId(), SOURCE_LABEL, text);
        });
    }

    /**
     * STT/카카오톡 분석데이터(PII 마스킹된 텍스트)를 RAG 벡터스토어에 적재한다.
     *
     * <p>분석데이터 1건 = 문서 1건(docId="analysis:{id}")으로 인덱싱되어, 챗봇이
     * 사용자의 통화/대화 맥락("내가 통화에서 가고 싶다 했던 곳" 등)을 참조할 수 있게 한다.
     * 빈 텍스트는 IngestionService.ingest 내부에서 스킵된다.</p>
     */
    public void indexAnalysis(Long userId, Long analysisId, String text) {
        ingestionService.ingest(userId, "analysis:" + analysisId, ANALYSIS_SOURCE_LABEL, text);
    }

    /** plan을 임베딩하기 좋은 한국어 요약 텍스트로 변환한다. */
    private String toSummaryText(TripPlan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append("여행 제목: ").append(plan.getTitle()).append('\n');
        sb.append("기간: ").append(plan.getStartDate()).append(" ~ ").append(plan.getEndDate()).append('\n');
        if (plan.getCompanions() != null) {
            sb.append("동행: ").append(plan.getCompanions()).append('\n');
        }
        if (plan.getBudget() != null) {
            sb.append("예산: ").append(plan.getBudget()).append("원\n");
        }

        for (TripDay day : plan.getDays()) {
            sb.append("\n[Day ").append(day.getDayNo()).append("]\n");
            if (day.getMemo() != null && !day.getMemo().isBlank()) {
                sb.append("메모: ").append(day.getMemo()).append('\n');
            }
            for (TripPlace place : day.getPlaces()) {
                sb.append("- ");
                if (place.getVisitTime() != null) {
                    sb.append(place.getVisitTime()).append(' ');
                }
                if (place.getAttraction() != null && place.getAttraction().getTitle() != null) {
                    sb.append(place.getAttraction().getTitle());
                    if (place.getAttraction().getAddr() != null) {
                        sb.append(" (").append(place.getAttraction().getAddr()).append(')');
                    }
                }
                if (place.getMemo() != null && !place.getMemo().isBlank()) {
                    sb.append(" — ").append(place.getMemo());
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
