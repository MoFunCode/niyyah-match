package com.niyyahmatch.niyyahmatch.service;

import com.niyyahmatch.niyyahmatch.entity.Match;
import com.niyyahmatch.niyyahmatch.entity.MatchStatus;
import com.niyyahmatch.niyyahmatch.entity.Swipe;
import com.niyyahmatch.niyyahmatch.entity.SwipeDirection;
import com.niyyahmatch.niyyahmatch.entity.User;
import com.niyyahmatch.niyyahmatch.exception.ResourceNotFoundException;
import com.niyyahmatch.niyyahmatch.repository.MatchRepository;
import com.niyyahmatch.niyyahmatch.repository.SwipeRepository;
import com.niyyahmatch.niyyahmatch.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class MatchService {

    private static final int DAILY_SWIPE_LIMIT = 12;

    private final MatchRepository matchRepository;
    private final SwipeRepository swipeRepository;
    private final UserRepository userRepository;

    public MatchService(MatchRepository matchRepository, SwipeRepository swipeRepository, UserRepository userRepository) {
        this.matchRepository = matchRepository;
        this.swipeRepository = swipeRepository;
        this.userRepository = userRepository;
    }

    public boolean hasActiveMatch(Long userId) {
        return matchRepository.existsByUser1IdAndStatusOrUser2IdAndStatus(
            userId, MatchStatus.ACTIVE,
            userId, MatchStatus.ACTIVE
        );
    }

    @Transactional
    public Optional<Match> recordSwipe(Long userId, Long targetUserId, SwipeDirection direction) {
        if (userId.equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot swipe on yourself");
        }

        if (hasActiveMatch(userId)) {
            throw new IllegalStateException("Cannot swipe while you have an active match");
        }

        LocalDateTime todayMidnightUTC = LocalDate.now(ZoneOffset.UTC).atStartOfDay();
        int swipesToday = swipeRepository.countByUserIdAndSwipedAtAfter(userId, todayMidnightUTC);
        if (swipesToday >= DAILY_SWIPE_LIMIT) {
            throw new IllegalStateException("Daily swipe limit reached. Try again tomorrow.");
        }

        if (swipeRepository.existsByUserIdAndTargetUserId(userId, targetUserId)) {
            throw new IllegalStateException("You already swiped on this user");
        }

        Swipe swipe = Swipe.builder()
            .userId(userId)
            .targetUserId(targetUserId)
            .direction(direction)
            .swipedAt(LocalDateTime.now())
            .build();

        swipeRepository.save(swipe);

        if (direction == SwipeDirection.RIGHT) {
            return checkAndCreateMatch(userId, targetUserId);
        }

        return Optional.empty();
    }

    private Optional<Match> checkAndCreateMatch(Long userId, Long targetUserId) {
        boolean targetSwipedRight = swipeRepository.existsByUserIdAndTargetUserIdAndDirection(
            targetUserId,
            userId,
            SwipeDirection.RIGHT
        );

        if (targetSwipedRight) {
            return Optional.of(createMatch(userId, targetUserId));
        }

        return Optional.empty();
    }

    private Match createMatch(Long user1Id, Long user2Id) {
        User user1 = userRepository.findById(user1Id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + user1Id));

        User user2 = userRepository.findById(user2Id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + user2Id));

        Match match = Match.builder()
            .user1(user1)
            .user2(user2)
            .status(MatchStatus.ACTIVE)
            .matchedAt(LocalDateTime.now())
            .build();

        return matchRepository.save(match);
    }

    public int getRemainingSwipes(Long userId) {
        LocalDateTime todayMidnightUTC = LocalDate.now(ZoneOffset.UTC).atStartOfDay();
        int swipesToday = swipeRepository.countByUserIdAndSwipedAtAfter(userId, todayMidnightUTC);
        return Math.max(0, DAILY_SWIPE_LIMIT - swipesToday);
    }

    public Optional<Match> getActiveMatch(Long userId) {
        return matchRepository.findByUser1IdAndStatusOrUser2IdAndStatus(
            userId, MatchStatus.ACTIVE,
            userId, MatchStatus.ACTIVE
        );
    }

    @Transactional
    public void unmatch(Long matchId, Long userId) {
        Match match = matchRepository.findById(matchId)
            .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));

        if (!match.getUser1().getId().equals(userId) && !match.getUser2().getId().equals(userId)) {
            throw new IllegalArgumentException("You are not part of this match");
        }

        if (match.getStatus() != MatchStatus.ACTIVE) {
            throw new IllegalStateException("Match is not active");
        }

        User unmatchingUser = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        match.setStatus(MatchStatus.UNMATCHED);
        match.setUnmatchedBy(unmatchingUser);
        match.setUnmatchedAt(LocalDateTime.now());

        matchRepository.save(match);
    }
}