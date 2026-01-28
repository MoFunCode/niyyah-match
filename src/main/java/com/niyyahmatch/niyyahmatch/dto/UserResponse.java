package com.niyyahmatch.niyyahmatch.dto;

import com.niyyahmatch.niyyahmatch.entity.Gender;
import com.niyyahmatch.niyyahmatch.entity.User;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    // User identifier
    private Long id;

    // Account information
    private String email;

    // Personal information
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private Gender gender;

    // Profile details
    private String location;
    private String bio;
    private String profilePhotoUrl;

    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime lastActive;

    // Convenience constructor: converts User entity to UserResponse DTO
    public UserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.dateOfBirth = user.getDateOfBirth();
        this.gender = user.getGender();
        this.location = user.getLocation();
        this.bio = user.getBio();
        this.profilePhotoUrl = user.getProfilePhotoUrl();
        this.createdAt = user.getCreatedAt();
        this.lastActive = user.getLastActive();
    }
}
