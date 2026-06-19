package com.trip.group.repository;

import com.trip.group.entity.TravelGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TravelGroupRepository extends JpaRepository<TravelGroup, Long> {

    Optional<TravelGroup> findByIdAndOwnerId(Long id, Long ownerId);

    List<TravelGroup> findByOwnerId(Long ownerId);
}
