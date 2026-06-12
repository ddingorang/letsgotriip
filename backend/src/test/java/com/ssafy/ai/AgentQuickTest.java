package com.ssafy.ai;

import com.ssafy.ai.controller.AgentController;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 에이전트 주요 기능 퀵 테스트
 * index.html의 'Quick Questions' 항목들을 단위 테스트로 검증합니다.
 */
@Slf4j
@SpringBootTest
class AgentQuickTest {

    @Autowired
    private AgentController agentController;

    @Test
    @DisplayName("날씨 조회 테스트: 서울의 현재 날씨와 기온 확인")
    void testWeatherQuickQuestion() {
        String question = "서울 날씨 어때?";
        executeAndVerify(question, "Standard Mode");
    }

    @Test
    @DisplayName("웹 검색 테스트: 최신 AI 기술 트렌드 정보 수집")
    void testSearchQuickQuestion() {
        String question = "최신 AI 기술 트렌드 검색해줘";
        executeAndVerify(question, "Standard Mode");
    }

    @Test
    @DisplayName("회원 통계 테스트: 특정 직군(개발)의 회원 수 확인")
    void testUserCountQuickQuestion() {
        String question = "우리 서비스 개발 직군 회원은 몇 명이야?";
        executeAndVerify(question, "Standard Mode");
    }

    @Test
    @DisplayName("회원 목록 테스트: 최근 가입한 상위 5명 정보 확인")
    void testUserListQuickQuestion() {
        String question = "최근에 가입한 회원 5명만 보여줘";
        executeAndVerify(question, "Standard Mode");
    }

    @Test
    @DisplayName("회원 분포 테스트: 특정 지역(서울)의 직군 분포 요약")
    void testUserStatsQuickQuestion() {
        String question = "서울 지역 회원들의 직군 분포를 요약해줘";
        executeAndVerify(question, "Standard Mode");
    }

    @Test
    @DisplayName("복합 미션(AI Planning) 테스트: 날씨 확인, 행사 판단 및 공지문 작성")
    void testPlanningQuickQuestion() {
        String question = "부산 날씨 확인하고, 야외 행사하기 좋은지 판단해서 기획 직군에게 공유할 공지문 작성해줘";
        log.info(">>> [AI Planning Mode] 질문 실행: {}", question);

        AgentController.ChatRequest req = new AgentController.ChatRequest();
        req.setMessage(question);

        // Smart AI Planning 모드(smartPlanChat) 호출
        String response = agentController.smartPlanChat(req);

        log.info("<<< AI 응답 (Smart Planning): \n{}", response);
        assertThat(response).as("응답이 비어있지 않아야 합니다.").isNotEmpty();
        assertThat(response).as("부산에 대한 정보가 포함되어야 합니다.").contains("부산");
    }

    /**
     * 공통 실행 및 검증 로직 (Standard 모드용)
     */
    private void executeAndVerify(String question, String mode) {
        log.info(">>> [{}] 질문 실행: {}", mode, question);

        AgentController.ChatRequest req = new AgentController.ChatRequest();
        req.setMessage(question);

        // Standard 모드(chat) 호출
        String response = agentController.chat(req);

        log.info("<<< AI 응답: \n{}", response);
        assertThat(response).as("응답이 비어있지 않아야 합니다.").isNotEmpty();
    }
}
