package com.ssafy.ai.tool;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.ssafy.ai.domain.GroupCountRow;
import com.ssafy.ai.domain.UserDto;
import com.ssafy.ai.domain.UserSearchCriteria;
import com.ssafy.ai.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserTools {

    private final UserService userService;

    // -------------------------
    // 1) 요약
    // -------------------------
    @Tool(description = """
            [역할]
            - 자연어 조건(예: '1950년대생', '서울 개발', 'ADMIN 제외')을 해석해 회원을 조회하고 요약한다.
            [입력]
            - criteriaText: 조건 문장
            - limit: 1~100 (없으면 10)
            [정책]
            - 조건 일부만 있어도 동작. 없는 조건은 ALL.
            - 결과 없으면 "조회된 회원이 없습니다."
            - 이메일은 마스킹
            """)
    public String summarizeUsersByText(String criteriaText, Integer limit) {
        log.debug("### TOOL CALLED: summarizeUsersByText, criteriaText={}", criteriaText);

        int safeLimit = safeLimit(limit);

        UserSearchCriteria c = parseCriteria(criteriaText);
        c.setLimit(safeLimit);

        List<UserDto> users = userService.findUsersByCriteria(c);
        if (users.isEmpty())
            return "조회된 회원이 없습니다.";

        return renderSummary(c, users);
    }

    // -------------------------
    // 2) 카운트
    // -------------------------
    @Tool(description = """
            [역할]
            - 자연어 조건에 맞는 회원 수를 반환한다. (예: '1950년대생 몇 명이야?')
            [입력]
            - criteriaText: 조건 문장
            [출력]
            - "조건에 맞는 회원 수: N명"
            [정책]
            - 조건 일부만 있어도 동작. 결과는 추측하지 않는다.
            """)
    public String countUsersByText(String criteriaText) {
        log.debug("### TOOL CALLED: countUsersByText, criteriaText={}", criteriaText);

        UserSearchCriteria c = parseCriteria(criteriaText);
        // count는 limit 의미 없음
        int cnt = userService.countUsersByCriteria(c);

        return "조건에 맞는 회원 수: " + cnt + "명";
    }

    // -------------------------
    // 3) 그룹 통계
    // -------------------------
    @Tool(description = """
            [역할]
            - 조건에 맞는 회원들을 그룹별로 집계한다. (지역별/직군별/역할별/출생 10년대별)
            [입력]
            - criteriaText: 조건 문장 (없으면 전체)
            - groupBy: "region" | "jobGroup" | "role" | "birthDecade" (없으면 region)
            [출력]
            - 예: "[region 분포]\\n- 서울: 10\\n- 경기: 7 ..."
            [정책]
            - 결과가 비어있으면 "조회된 회원이 없습니다."
            """)
    public String getUserGroupStatsByText(String criteriaText, String groupBy) {
        log.debug("### TOOL CALLED: getUserGroupStatsByText, criteriaText={}, groupBy={}", criteriaText, groupBy);

        UserSearchCriteria c = parseCriteria(criteriaText);
        List<GroupCountRow> rows = userService.groupCounts(c, groupBy);

        if (rows == null || rows.isEmpty())
            return "조회된 회원이 없습니다.";

        String g = (groupBy == null || groupBy.isBlank()) ? "region" : groupBy.trim();

        StringBuilder sb = new StringBuilder();
        sb.append("[").append(g).append(" 분포]\n");
        for (GroupCountRow r : rows) {
            sb.append("- ").append(r.getGroupKey()).append(": ").append(r.getCnt()).append("\n");
        }
        return sb.toString().trim();
    }

    // -------------------------
    // 4) 최근 가입자
    // -------------------------
    @Tool(description = """
            [역할]
            - 최근 가입자 목록을 요약한다. 조건 파싱 없이 최신 가입순으로 조회한다.
            [입력]
            - limit: 1~100 (없으면 10)
            [정책]
            - 이메일 마스킹
            - 결과가 없으면 "조회된 회원이 없습니다."
            """)
    public String listRecentUsers(Integer limit) {
        log.debug("### TOOL CALLED: listRecentUsers, limit={}", limit);

        int safeLimit = safeLimit(limit);
        List<UserDto> users = userService.findRecentUsers(safeLimit);
        if (users.isEmpty())
            return "조회된 회원이 없습니다.";

        StringBuilder sb = new StringBuilder();
        sb.append("[최근 가입자 ").append(users.size()).append("명]\n");
        for (UserDto u : users) {
            sb.append("- ").append(u.getName()).append(" (").append(maskEmail(u.getEmail())).append(")")
                    .append(", role=").append(nullToDash(u.getRole())).append(", job=")
                    .append(nullToDash(u.getJobGroup())).append(", region=").append(nullToDash(u.getRegion()))
                    .append(", birth=").append(u.getBirthday() == null ? "-" : u.getBirthday()).append(", joined=")
                    .append(u.getJoinedAt() == null ? "-" : u.getJoinedAt().toLocalDate()).append("\n");
        }
        return sb.toString().trim();
    }

    // =========================================================
    // Parsing (자연어 -> UserSearchCriteria)
    // =========================================================
    private UserSearchCriteria parseCriteria(String text) {
        UserSearchCriteria c = new UserSearchCriteria();
        if (text == null)
            return c;

        String t = text.trim();

        // role
        if (containsAny(t, "admin 제외", "관리자 제외", "admin 빼고", "관리자 빼고")) {
            c.setRole("USER");
        } else if (containsAny(t.toLowerCase(), "admin", "관리자")) {
            c.setRole("ADMIN");
        } else if (containsAny(t.toLowerCase(), "user", "일반")) {
            c.setRole("USER");
        }

        // job_group
        for (String job : List.of("개발", "기획", "디자인", "마케팅", "영업")) {
            if (t.contains(job)) {
                c.setJobGroup(job);
                break;
            }
        }

        // region
        for (String region : List.of("서울", "경기", "인천", "부산", "대구", "대전", "광주", "울산", "충남", "충북", "강원", "전남", "전북", "경남",
                "경북", "제주")) {
            if (t.contains(region)) {
                c.setRegion(region);
                break;
            }
        }

        // birth range / decade
        parseBirthYearRange(t, c);

        return c;
    }

    private void parseBirthYearRange(String t, UserSearchCriteria c) {
        // YYYY~YYYY or YYYY-YYYY
        Pattern range = Pattern.compile("(19\\d{2}|20\\d{2})\\s*[~\\-]\\s*(19\\d{2}|20\\d{2})");
        Matcher m1 = range.matcher(t);
        if (m1.find()) {
            int from = Integer.parseInt(m1.group(1));
            int to = Integer.parseInt(m1.group(2));
            if (from > to) {
                int tmp = from;
                from = to;
                to = tmp;
            }
            c.setBirthFrom(from);
            c.setBirthTo(to);
            return;
        }

        // "1950년대생" / "2000년대생"
        Pattern decade4 = Pattern.compile("((?:19|20)\\d0)\\s*년대\\s*생?");
        Matcher m2 = decade4.matcher(t);
        if (m2.find()) {
            int decade = Integer.parseInt(m2.group(1));
            c.setBirthFrom(decade);
            c.setBirthTo(decade + 9);
            return;
        }

        // "90년대생" => 1990~1999
        Pattern decade2 = Pattern.compile("(\\d{2})\\s*년대\\s*생?");
        Matcher m3 = decade2.matcher(t);
        if (m3.find()) {
            int d = Integer.parseInt(m3.group(1));
            int base = (d <= 10) ? 2000 : 1900;
            int from = base + d;
            c.setBirthFrom(from);
            c.setBirthTo(from + 9);
        }
    }

    private boolean containsAny(String t, String... keys) {
        String low = t.toLowerCase();
        for (String k : keys) {
            if (low.contains(k.toLowerCase()))
                return true;
        }
        return false;
    }

    // =========================================================
    // Render helpers
    // =========================================================
    private String renderSummary(UserSearchCriteria c, List<UserDto> users) {
        int count = users.size();

        int ageKnown = 0;
        int ageMin = Integer.MAX_VALUE, ageMax = Integer.MIN_VALUE;
        for (UserDto u : users) {
            if (u.getBirthday() != null) {
                int age = calcAge(u.getBirthday());
                ageMin = Math.min(ageMin, age);
                ageMax = Math.max(ageMax, age);
                ageKnown++;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[요약]\n");
        sb.append("- 조회 인원: ").append(count).append("명\n");
        sb.append("- 조건: role=").append(nullToAll(c.getRole())).append(", jobGroup=").append(nullToAll(c.getJobGroup()))
                .append(", region=").append(nullToAll(c.getRegion())).append(", birthFrom=")
                .append(c.getBirthFrom() == null ? "ALL" : c.getBirthFrom()).append(", birthTo=")
                .append(c.getBirthTo() == null ? "ALL" : c.getBirthTo()).append("\n");

        if (ageKnown > 0) {
            sb.append("- 연령대(추정): ").append(ageMin).append("~").append(ageMax).append("세 (생년월일 있는 ").append(ageKnown)
                    .append("명 기준)\n");
        }

        sb.append("\n[회원 목록]\n");
        for (UserDto u : users) {
            sb.append("- ").append(u.getName()).append(" (").append(maskEmail(u.getEmail())).append(")")
                    .append(", role=").append(nullToDash(u.getRole())).append(", job=")
                    .append(nullToDash(u.getJobGroup())).append(", region=").append(nullToDash(u.getRegion()))
                    .append(", birth=").append(u.getBirthday() == null ? "-" : u.getBirthday()).append(", joined=")
                    .append(u.getJoinedAt() == null ? "-" : u.getJoinedAt().toLocalDate()).append("\n");
        }
        return sb.toString().trim();
    }

    private int safeLimit(Integer limit) {
        if (limit == null || limit <= 0)
            return 10;
        return Math.min(limit, 100);
    }

    private String nullToAll(String s) {
        return (s == null || s.isBlank()) ? "ALL" : s;
    }

    private String nullToDash(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private int calcAge(LocalDate birth) {
        LocalDate today = LocalDate.now();
        int age = today.getYear() - birth.getYear();
        if (today.getMonthValue() < birth.getMonthValue()
                || (today.getMonthValue() == birth.getMonthValue() && today.getDayOfMonth() < birth.getDayOfMonth())) {
            age--;
        }
        return age;
    }

    private String maskEmail(String email) {
        if (email == null)
            return "";
        int at = email.indexOf('@');
        if (at <= 1)
            return "***" + (at >= 0 ? email.substring(at) : "");
        String prefix = email.substring(0, at);
        String domain = email.substring(at);
        String head = prefix.length() >= 2 ? prefix.substring(0, 2) : prefix.substring(0, 1);
        return head + "***" + domain;
    }

}
