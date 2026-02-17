package com.niyyahmatch.niyyahmatch.repository;

import com.niyyahmatch.niyyahmatch.entity.Gender;
import com.niyyahmatch.niyyahmatch.entity.MatchStatus;
import com.niyyahmatch.niyyahmatch.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Find swipeable candidates for a user, applying all active filters:
    // - Opposite gender only
    // - Exclude self
    // - Exclude users already swiped on
    // - Exclude current active match partner
    // - Optional: age range (converted to birth date range by the service layer)
    // - Optional: location match
    @Query("""
            SELECT u FROM User u
            WHERE u.gender = :gender
            AND u.id != :userId
            AND u.id NOT IN (
                SELECT s.targetUserId FROM Swipe s WHERE s.userId = :userId
            )
            AND NOT EXISTS (
                SELECT m FROM Match m
                WHERE m.status = :activeStatus
                AND (
                    (m.user1.id = :userId AND m.user2.id = u.id)
                    OR (m.user2.id = :userId AND m.user1.id = u.id)
                )
            )
            AND (CAST(:location AS String) IS NULL OR u.location = :location)
            AND (CAST(:minBirthDate AS LocalDate) IS NULL OR u.dateOfBirth >= :minBirthDate)
            AND (CAST(:maxBirthDate AS LocalDate) IS NULL OR u.dateOfBirth <= :maxBirthDate)
            """)
    Page<User> findCandidates(
            @Param("userId") Long userId,
            @Param("gender") Gender gender,
            @Param("location") String location,
            @Param("minBirthDate") LocalDate minBirthDate,
            @Param("maxBirthDate") LocalDate maxBirthDate,
            @Param("activeStatus") MatchStatus activeStatus,
            Pageable pageable
    );
}
