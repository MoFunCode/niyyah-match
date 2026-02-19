package com.niyyahmatch.niyyahmatch.repository;

import com.niyyahmatch.niyyahmatch.entity.Match;
import com.niyyahmatch.niyyahmatch.entity.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // @Query is used when the derived method name syntax can't express the logic we need.
    // Here we need OR with two different column pairs - that's too complex for a method name.
    // JPQL (the query language) references entity field names (user1.id), not column names (user1_id).
    @Query("SELECT m FROM Match m WHERE (m.user1.id = :userAId AND m.user2.id = :userBId) OR (m.user1.id = :userBId AND m.user2.id = :userAId)")
    Optional<Match> findByBothUserIds(@Param("userAId") Long userAId, @Param("userBId") Long userBId);
}
