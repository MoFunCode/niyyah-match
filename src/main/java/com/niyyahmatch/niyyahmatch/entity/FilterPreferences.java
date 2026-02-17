package com.niyyahmatch.niyyahmatch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "filter_preferences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    private Integer minAge;

    private Integer maxAge;

    private String location;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
