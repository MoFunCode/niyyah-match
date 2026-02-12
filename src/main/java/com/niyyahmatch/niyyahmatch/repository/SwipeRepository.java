package com.niyyahmatch.niyyahmatch.repository;

import com.niyyahmatch.niyyahmatch.entity.Swipe;
import com.niyyahmatch.niyyahmatch.entity.SwipeDirection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface SwipeRepository extends JpaRepository<Swipe, Long> {

    // Check if user already swiped on target (prevents duplicate swipes)
    boolean existsByUserIdAndTargetUserId(Long userId, Long targetUserId);

    // Check if user swiped RIGHT on target (detects mutual matches)
    boolean existsByUserIdAndTargetUserIdAndDirection(
            Long userId,
            Long targetUserId,
            SwipeDirection direction
    );

    // Count swipes after a time (enforces 12 swipes per day limit)
    int countByUserIdAndSwipedAtAfter(Long userId, LocalDateTime startTime);
}
