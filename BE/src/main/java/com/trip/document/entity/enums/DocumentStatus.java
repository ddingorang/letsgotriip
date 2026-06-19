package com.trip.document.entity.enums;

public enum DocumentStatus {
    PENDING,
    INGESTED,
    // 추출/전사 텍스트가 비어 색인할 내용이 없는 경우(빈 PDF/빈 텍스트 등).
    // 색인되지 않아 질문에 쓸 수 없으므로 '완료'와 구분한다.
    EMPTY,
    FAILED
}
