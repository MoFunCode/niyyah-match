package com.niyyahmatch.niyyahmatch.service;

import com.niyyahmatch.niyyahmatch.entity.*;
import com.niyyahmatch.niyyahmatch.exception.ResourceNotFoundException;
import com.niyyahmatch.niyyahmatch.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MessageService {

    // Constant instead of hardcoding 20 everywhere - one place to change if needed.
    private static final int PAGE_SIZE = 20;

    private final MessageRepository messageRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;

    public MessageService(MessageRepository messageRepository, MatchRepository matchRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Message sendMessage(Long senderId, Long matchId, String content) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));

        // Authorization check BEFORE status check - don't tell an outsider anything about the match state.
        if (!isUserInMatch(senderId, match)) {
            throw new IllegalArgumentException("You are not part of this match");
        }

        // Business rule: messaging is only allowed while the match is ACTIVE.
        // After unmatch, this gate closes - no new messages, no read access.
        if (match.getStatus() != MatchStatus.ACTIVE) {
            throw new IllegalStateException("Cannot send messages to an inactive match");
        }

        // We need the full User entity here (not just the ID) because Message.sender is @ManyToOne User.
        // JPA needs the entity reference to set the FK correctly when saving.
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + senderId));

        Message message = Message.builder()
                .match(match)
                .sender(sender)
                .content(content)
                .sentAt(LocalDateTime.now())
                .build();

        return messageRepository.save(message);
    }

    // No @Transactional needed here - this is a read-only query, no data is being changed.
    public Page<Message> getMessages(Long userId, Long matchId, int page) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + matchId));

        if (!isUserInMatch(userId, match)) {
            throw new IllegalArgumentException("You are not part of this match");
        }

        if (match.getStatus() != MatchStatus.ACTIVE) {
            throw new IllegalStateException("Cannot access messages for an inactive match");
        }

        // PageRequest.of(page, PAGE_SIZE) tells Spring Data which page and how many results per page.
        // The repository returns a Page<Message> which includes the data + pagination metadata (total pages, etc.).
        return messageRepository.findByMatchIdOrderBySentAtAsc(matchId, PageRequest.of(page, PAGE_SIZE));
    }

    // Private helper - this logic is reused by both sendMessage and getMessages.
    // Checks both sides because either user could be user1 or user2 in the match.
    private boolean isUserInMatch(Long userId, Match match) {
        return match.getUser1().getId().equals(userId) || match.getUser2().getId().equals(userId);
    }
}
