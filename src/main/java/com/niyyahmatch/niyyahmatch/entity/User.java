package com.niyyahmatch.niyyahmatch.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder // Generates builder pattern for clean object creation: User.builder().firstName("Mo").email("...").build()
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String firstName;
    private String lastName;
    @Column(nullable = false)
    private LocalDate dateOfBirth;
    @Enumerated(EnumType.STRING)
    private Gender gender;
    @Column(unique = true, nullable = false)
    private String email;
    private String password;
    private String location;
    private String bio;
    private String profilePhotoUrl;
    private LocalDateTime createdAt;
    private LocalDateTime lastActive;

    // Islamic identity and lifestyle fields
    @Enumerated(EnumType.STRING)
    private Sect sect;

    @Enumerated(EnumType.STRING)
    private PrayerFrequency prayerFrequency;

    @Enumerated(EnumType.STRING)
    private EducationLevel educationLevel;

    // Hijab is relevant for female users and for male users' preferences
    @Enumerated(EnumType.STRING)
    private HijabPreference hijabStatus;

}
