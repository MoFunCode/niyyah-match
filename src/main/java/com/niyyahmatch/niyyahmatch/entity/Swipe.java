package com.niyyahmatch.niyyahmatch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "swipe_history", indexes = {
    @Index(name = "idx_swipe_user_target", columnList = "userId, targetUserId"),
    @Index(name = "idx_swipe_user_time", columnList = "userId, swipedAt")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Swipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long targetUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SwipeDirection direction;

    @Column(nullable = false)
    private LocalDateTime swipedAt;
}
