package com.niyyahmatch.niyyahmatch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "matches", indexes = {
    @Index(name = "idx_match_user1_status", columnList = "user1_id, status"),
    @Index(name = "idx_match_user2_status", columnList = "user2_id, status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    @ManyToOne
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    @Column(nullable = false)
    private LocalDateTime matchedAt;

    @ManyToOne
    @JoinColumn(name = "unmatched_by_user_id")
    private User unmatchedBy;

    private LocalDateTime unmatchedAt;
}
