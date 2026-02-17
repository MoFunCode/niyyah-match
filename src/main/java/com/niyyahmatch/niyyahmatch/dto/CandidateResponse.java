package com.niyyahmatch.niyyahmatch.dto;

import com.niyyahmatch.niyyahmatch.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Period;

@Getter
@Setter
@NoArgsConstructor
public class CandidateResponse {

    private Long id;
    private String firstName;
    private int age;
    private String location;
    private String bio;
    private String profilePhotoUrl;

    public CandidateResponse(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.age = Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();
        this.location = user.getLocation();
        this.bio = user.getBio();
        this.profilePhotoUrl = user.getProfilePhotoUrl();
    }
}
