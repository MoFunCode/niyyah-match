package com.niyyahmatch.niyyahmatch.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many messages belong to one match. @JoinColumn creates the FK column in the messages table.
    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    // Many messages can be sent by one user. Storing the full User entity (not just userId)
    // lets us access sender.getFirstName() without an extra DB lookup.
    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // length = 1000 maps to VARCHAR(1000) in PostgreSQL - Hibernate uses this to set the column size.
    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime sentAt;
}
