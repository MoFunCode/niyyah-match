package com.niyyahmatch.niyyahmatch.repository;

import com.niyyahmatch.niyyahmatch.entity.Match;
import com.niyyahmatch.niyyahmatch.entity.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    boolean existsByUser1IdAndStatusOrUser2IdAndStatus(
            Long user1Id,
            MatchStatus status1,
            Long user2Id,
            MatchStatus status2
    );

    Optional<Match> findByUser1IdAndStatusOrUser2IdAndStatus(
            Long user1Id,
            MatchStatus status1,
            Long user2Id,
            MatchStatus status2
    );
}
