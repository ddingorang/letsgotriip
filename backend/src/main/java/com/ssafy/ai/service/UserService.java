package com.ssafy.ai.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ssafy.ai.dao.UserDao;
import com.ssafy.ai.domain.GroupCountRow;
import com.ssafy.ai.domain.UserDto;
import com.ssafy.ai.domain.UserSearchCriteria;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserDao userDao;


    public List<UserDto> findRecentUsers(int limit) {
        return userDao.selectRecentUsers(normalizeLimit(limit));
    }

    public List<UserDto> findUsersByCriteria(UserSearchCriteria c) {
        UserSearchCriteria normalized = normalizeCriteria(c);
        return userDao.selectUsersByCriteria(normalized);
    }

    public int countUsersByCriteria(UserSearchCriteria c) {
        UserSearchCriteria normalized = normalizeCriteria(c);
        return userDao.countUsersByCriteria(normalized);
    }

    public List<GroupCountRow> groupCounts(UserSearchCriteria c, String groupBy) {
        UserSearchCriteria normalized = normalizeCriteria(c);
        String safeGroupBy = normalizeGroupBy(groupBy);
        return userDao.selectGroupCounts(normalized, safeGroupBy);
    }

    private UserSearchCriteria normalizeCriteria(UserSearchCriteria c) {
        if (c == null) c = new UserSearchCriteria();

        c.setRole(trimToNull(c.getRole()));
        c.setJobGroup(trimToNull(c.getJobGroup()));
        c.setRegion(trimToNull(c.getRegion()));

        Integer limit = c.getLimit();
        c.setLimit(normalizeLimit(limit == null ? 10 : limit));

        Integer from = c.getBirthFrom();
        Integer to = c.getBirthTo();
        if (from != null && to != null && from > to) {
            c.setBirthFrom(to);
            c.setBirthTo(from);
        }
        return c;
    }

    private String normalizeGroupBy(String groupBy) {
        if (groupBy == null) return "region";
        String g = groupBy.trim();
        return switch (g) {
            case "region", "jobGroup", "role", "birthDecade" -> g;
            default -> "region";
        };
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) return 10;
        return Math.min(limit, 100);
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
