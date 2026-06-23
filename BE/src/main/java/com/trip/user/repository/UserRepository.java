package com.trip.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trip.user.entity.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);

    /** 시드 마커 계정 조회 — 시드 reset 시 @seed.triip 소유 데이터만 정리하기 위해 사용. */
    List<User> findByEmailEndingWith(String suffix);
}
