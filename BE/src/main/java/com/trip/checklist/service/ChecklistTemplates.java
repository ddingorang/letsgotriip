package com.trip.checklist.service;

import com.trip.checklist.dto.ChecklistTemplateResponse;
import com.trip.checklist.dto.ChecklistTemplateResponse.Item;

import java.util.List;
import java.util.Optional;

/**
 * 내장 체크리스트 템플릿 모음.
 * 국내여행 / 해외여행 / 캠핑 — 키로 조회하거나 전체 목록을 제공한다.
 */
public final class ChecklistTemplates {

    private ChecklistTemplates() {
    }

    /** 국내여행 기본 */
    public static final ChecklistTemplateResponse DOMESTIC = new ChecklistTemplateResponse(
            "domestic",
            "국내여행 기본",
            List.of(
                    new Item("신분증", "서류"),
                    new Item("교통편 예매 확인 (KTX/버스/항공)", "예약"),
                    new Item("숙소 예약 확인", "예약"),
                    new Item("휴대폰 충전기 / 보조배터리", "준비물"),
                    new Item("세면도구", "준비물"),
                    new Item("계절에 맞는 옷", "준비물"),
                    new Item("상비약", "준비물"),
                    new Item("현금 / 카드", "기타")
            )
    );

    /** 해외여행 */
    public static final ChecklistTemplateResponse OVERSEAS = new ChecklistTemplateResponse(
            "overseas",
            "해외여행",
            List.of(
                    new Item("여권 (유효기간 6개월 이상)", "서류"),
                    new Item("항공권 e-티켓", "서류"),
                    new Item("비자 / 입국서류", "서류"),
                    new Item("여행자보험 가입", "예약"),
                    new Item("숙소 바우처", "예약"),
                    new Item("환전 / 해외결제 카드", "기타"),
                    new Item("멀티 어댑터 / 변환 플러그", "준비물"),
                    new Item("로밍 / 유심 / 이심 준비", "준비물"),
                    new Item("상비약 / 처방약", "준비물"),
                    new Item("여권 사본 / 비상 연락처", "서류")
            )
    );

    /** 캠핑 */
    public static final ChecklistTemplateResponse CAMPING = new ChecklistTemplateResponse(
            "camping",
            "캠핑",
            List.of(
                    new Item("캠핑장 예약 확인", "예약"),
                    new Item("텐트 / 타프", "준비물"),
                    new Item("침낭 / 매트", "준비물"),
                    new Item("랜턴 / 손전등", "준비물"),
                    new Item("버너 / 코펠 / 식기", "준비물"),
                    new Item("식재료 / 식수", "준비물"),
                    new Item("아이스박스 / 쿨러", "준비물"),
                    new Item("구급함 / 벌레 기피제", "기타")
            )
    );

    /** 전체 템플릿 목록 */
    public static List<ChecklistTemplateResponse> all() {
        return List.of(DOMESTIC, OVERSEAS, CAMPING);
    }

    /** 키로 단건 조회 */
    public static Optional<ChecklistTemplateResponse> byKey(String key) {
        return all().stream()
                .filter(t -> t.key().equals(key))
                .findFirst();
    }
}
