package com.niyyahmatch.niyyahmatch.service;

import com.niyyahmatch.niyyahmatch.entity.*;
import com.niyyahmatch.niyyahmatch.exception.ResourceNotFoundException;
import com.niyyahmatch.niyyahmatch.repository.MatchRepository;
import com.niyyahmatch.niyyahmatch.repository.SwipeRepository;
import com.niyyahmatch.niyyahmatch.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private SwipeRepository swipeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MatchService matchService;

    private User user1;
    private User user2;
    private Match activeMatch;

    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .id(1L)
                .firstName("Ahmed")
                .lastName("Hassan")
                .email("ahmed@example.com")
                .dateOfBirth(LocalDate.of(1995, 1, 1))
                .gender(Gender.MALE)
                .build();

        user2 = User.builder()
                .id(2L)
                .firstName("Fatima")
                .lastName("Ali")
                .email("fatima@example.com")
                .dateOfBirth(LocalDate.of(1996, 2, 2))
                .gender(Gender.FEMALE)
                .build();

        activeMatch = Match.builder()
                .id(1L)
                .user1(user1)
                .user2(user2)
                .status(MatchStatus.ACTIVE)
                .matchedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void hasActiveMatch_WhenUserHasActiveMatch_ReturnsTrue() {
        when(matchRepository.existsByUser1IdAndStatusOrUser2IdAndStatus(
                1L, MatchStatus.ACTIVE, 1L, MatchStatus.ACTIVE))
                .thenReturn(true);

        boolean result = matchService.hasActiveMatch(1L);

        assertTrue(result);
        verify(matchRepository).existsByUser1IdAndStatusOrUser2IdAndStatus(
                1L, MatchStatus.ACTIVE, 1L, MatchStatus.ACTIVE);
    }

    @Test
    void hasActiveMatch_WhenUserHasNoActiveMatch_ReturnsFalse() {
        when(matchRepository.existsByUser1IdAndStatusOrUser2IdAndStatus(
                1L, MatchStatus.ACTIVE, 1L, MatchStatus.ACTIVE))
                .thenReturn(false);

        boolean result = matchService.hasActiveMatch(1L);

        assertFalse(result);
    }

    @Test
    void recordSwipe_WhenSwipingOnSelf_ThrowsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.recordSwipe(1L, 1L, SwipeDirection.RIGHT)
        );

        assertEquals("Cannot swipe on yourself", exception.getMessage());
        verify(swipeRepository, never()).save(any());
    }

    @Test
    void recordSwipe_WhenUserHasActiveMatch_ThrowsException() {
        when(matchRepository.existsByUser1IdAndStatusOrUser2IdAndStatus(
                1L, MatchStatus.ACTIVE, 1L, MatchStatus.ACTIVE))
                .thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> matchService.recordSwipe(1L, 2L, SwipeDirection.RIGHT)
        );

        assertEquals("Cannot swipe while you have an active match", exception.getMessage());
        verify(swipeRepository, never()).save(any());
    }

    @Test
    void recordSwipe_WhenAlreadySwipedOnTarget_ThrowsException() {
        when(matchRepository.existsByUser1IdAndStatusOrUser2IdAndStatus(
                1L, MatchStatus.ACTIVE, 1L, MatchStatus.ACTIVE))
                .thenReturn(false);
        when(swipeRepository.existsByUserIdAndTargetUserId(1L, 2L))
                .thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> matchService.recordSwipe(1L, 2L, SwipeDirection.RIGHT)
        );

        assertEquals("You already swiped on this user", exception.getMessage());
        verify(swipeRepository, never()).save(any());
    }

    @Test
    void recordSwipe_WhenSwipingLeft_SavesSwipeAndReturnsEmpty() {
        when(matchRepository.existsByUser1IdAndStatusOrUser2IdAndStatus(
                1L, MatchStatus.ACTIVE, 1L, MatchStatus.ACTIVE))
                .thenReturn(false);
        when(swipeRepository.existsByUserIdAndTargetUserId(1L, 2L))
                .thenReturn(false);

        Optional<Match> result = matchService.recordSwipe(1L, 2L, SwipeDirection.LEFT);

        assertTrue(result.isEmpty());
        verify(swipeRepository).save(any(Swipe.class));
        verify(swipeRepository, never()).existsByUserIdAndTargetUserIdAndDirection(
                anyLong(), anyLong(), any());
    }

    @Test
    void recordSwipe_WhenSwipingRightWithNoMutualMatch_SavesSwipeAndReturnsEmpty() {
        when(matchRepository.existsByUser1IdAndStatusOrUser2IdAndStatus(
                1L, MatchStatus.ACTIVE, 1L, MatchStatus.ACTIVE))
                .thenReturn(false);
        when(swipeRepository.existsByUserIdAndTargetUserId(1L, 2L))
                .thenReturn(false);
        when(swipeRepository.existsByUserIdAndTargetUserIdAndDirection(
                2L, 1L, SwipeDirection.RIGHT))
                .thenReturn(false);

        Optional<Match> result = matchService.recordSwipe(1L, 2L, SwipeDirection.RIGHT);

        assertTrue(result.isEmpty());
        verify(swipeRepository).save(any(Swipe.class));
        verify(swipeRepository).existsByUserIdAndTargetUserIdAndDirection(
                2L, 1L, SwipeDirection.RIGHT);
        verify(matchRepository, never()).save(any(Match.class));
    }

    @Test
    void recordSwipe_WhenSwipingRightWithMutualMatch_CreatesMatchAndReturnsIt() {
        when(matchRepository.existsByUser1IdAndStatusOrUser2IdAndStatus(
                1L, MatchStatus.ACTIVE, 1L, MatchStatus.ACTIVE))
                .thenReturn(false);
        when(swipeRepository.existsByUserIdAndTargetUserId(1L, 2L))
                .thenReturn(false);
        when(swipeRepository.existsByUserIdAndTargetUserIdAndDirection(
                2L, 1L, SwipeDirection.RIGHT))
                .thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(matchRepository.save(any(Match.class))).thenReturn(activeMatch);

        Optional<Match> result = matchService.recordSwipe(1L, 2L, SwipeDirection.RIGHT);

        assertTrue(result.isPresent());
        assertEquals(MatchStatus.ACTIVE, result.get().getStatus());
        verify(swipeRepository).save(any(Swipe.class));
        verify(matchRepository).save(any(Match.class));
    }

    @Test
    void getActiveMatch_WhenMatchExists_ReturnsMatch() {
        when(matchRepository.findByUser1IdAndStatusOrUser2IdAndStatus(
                1L, MatchStatus.ACTIVE, 1L, MatchStatus.ACTIVE))
                .thenReturn(Optional.of(activeMatch));

        Optional<Match> result = matchService.getActiveMatch(1L);

        assertTrue(result.isPresent());
        assertEquals(activeMatch, result.get());
    }

    @Test
    void getActiveMatch_WhenNoMatchExists_ReturnsEmpty() {
        when(matchRepository.findByUser1IdAndStatusOrUser2IdAndStatus(
                1L, MatchStatus.ACTIVE, 1L, MatchStatus.ACTIVE))
                .thenReturn(Optional.empty());

        Optional<Match> result = matchService.getActiveMatch(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void unmatch_WhenMatchDoesNotExist_ThrowsException() {
        when(matchRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> matchService.unmatch(1L, 1L)
        );

        assertEquals("Match not found with id: 1", exception.getMessage());
        verify(matchRepository, never()).save(any());
    }

    @Test
    void unmatch_WhenUserNotPartOfMatch_ThrowsException() {
        when(matchRepository.findById(1L)).thenReturn(Optional.of(activeMatch));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> matchService.unmatch(1L, 999L)
        );

        assertEquals("You are not part of this match", exception.getMessage());
        verify(matchRepository, never()).save(any());
    }

    @Test
    void unmatch_WhenMatchNotActive_ThrowsException() {
        Match unmatchedMatch = Match.builder()
                .id(1L)
                .user1(user1)
                .user2(user2)
                .status(MatchStatus.UNMATCHED)
                .matchedAt(LocalDateTime.now())
                .build();

        when(matchRepository.findById(1L)).thenReturn(Optional.of(unmatchedMatch));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> matchService.unmatch(1L, 1L)
        );

        assertEquals("Match is not active", exception.getMessage());
        verify(matchRepository, never()).save(any());
    }

    @Test
    void unmatch_WhenValid_UpdatesMatchStatusToUnmatched() {
        when(matchRepository.findById(1L)).thenReturn(Optional.of(activeMatch));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(matchRepository.save(any(Match.class))).thenReturn(activeMatch);

        matchService.unmatch(1L, 1L);

        verify(matchRepository).save(argThat(match ->
            match.getStatus() == MatchStatus.UNMATCHED &&
            match.getUnmatchedBy() != null &&
            match.getUnmatchedAt() != null
        ));
    }

    @Test
    void unmatch_WhenUser2Unmatches_UpdatesMatchStatusToUnmatched() {
        when(matchRepository.findById(1L)).thenReturn(Optional.of(activeMatch));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(matchRepository.save(any(Match.class))).thenReturn(activeMatch);

        matchService.unmatch(1L, 2L);

        verify(matchRepository).save(argThat(match ->
            match.getStatus() == MatchStatus.UNMATCHED &&
            match.getUnmatchedBy().getId().equals(2L)
        ));
    }
}
