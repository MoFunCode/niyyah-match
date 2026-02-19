package com.niyyahmatch.niyyahmatch.dto;

import com.niyyahmatch.niyyahmatch.entity.EducationLevel;
import com.niyyahmatch.niyyahmatch.entity.Gender;
import com.niyyahmatch.niyyahmatch.entity.HijabPreference;
import com.niyyahmatch.niyyahmatch.entity.PrayerFrequency;
import com.niyyahmatch.niyyahmatch.entity.Sect;
import com.niyyahmatch.niyyahmatch.validation.MinAge;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {
    // Account credentials
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 254, message = "Email cannot exceed 254 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters")
    private String password;

    // Personal information
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(
            regexp = "^[\\p{L}\\p{M}'-]+(\\s[\\p{L}\\p{M}'-]+)*$",
            message = "First name can only contain letters, hyphens, apostrophes, and spaces"
    )
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Pattern(
            regexp = "^[\\p{L}\\p{M}'-]+(\\s[\\p{L}\\p{M}'-]+)*$",
            message = "Last name can only contain letters, hyphens, apostrophes, and spaces"
    )
    private String lastName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @MinAge(value = 18, message = "You must be at least 18 years old to register")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    // Profile details (optional at registration)
    @Size(max = 100, message = "Location cannot exceed 100 characters")
    private String location;

    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;

    @Size(max = 500, message = "Profile photo URL cannot exceed 500 characters")
    private String profilePhotoUrl;

    // Islamic identity and lifestyle fields (all optional at registration)
    private Sect sect;
    private PrayerFrequency prayerFrequency;
    private EducationLevel educationLevel;
    private HijabPreference hijabStatus;
}
