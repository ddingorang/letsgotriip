package com.ssafy.ai.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchCriteria {

    private String role; // USER/ADMIN
    private String jobGroup; // 개발/기획/디자인/마케팅/영업
    private String region; // 서울/경기/부산 등

    private Integer birthFrom; // YYYY
    private Integer birthTo; // YYYY

    private Integer limit; // 1~100
}
