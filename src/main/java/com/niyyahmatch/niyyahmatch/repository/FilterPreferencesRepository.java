package com.niyyahmatch.niyyahmatch.repository;

import com.niyyahmatch.niyyahmatch.entity.FilterPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FilterPreferencesRepository extends JpaRepository<FilterPreferences, Long> {

    // Find preferences for a specific user
    Optional<FilterPreferences> findByUserId(Long userId);
}
