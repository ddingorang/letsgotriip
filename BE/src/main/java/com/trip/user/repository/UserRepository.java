package com.trip.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trip.user.entity.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
}
