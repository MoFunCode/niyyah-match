package com.niyyahmatch.niyyahmatch.dto;

import com.niyyahmatch.niyyahmatch.entity.Gender;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {
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
}
