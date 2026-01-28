package com.niyyahmatch.niyyahmatch.dto;

import com.niyyahmatch.niyyahmatch.entity.Gender;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterUserRequest {
    // Account credentials
    private String email;
    private String password;

    // Personal information
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private Gender gender;

    // Profile details (optional at registration)
    private String location;
    private String bio;
    private String profilePhotoUrl;
}
