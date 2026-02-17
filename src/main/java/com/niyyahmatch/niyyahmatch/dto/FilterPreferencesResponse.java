package com.niyyahmatch.niyyahmatch.dto;

import com.niyyahmatch.niyyahmatch.entity.FilterPreferences;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class FilterPreferencesResponse {

    private Integer minAge;
    private Integer maxAge;
    private String location;
    private LocalDateTime updatedAt;

    public FilterPreferencesResponse(FilterPreferences preferences) {
        this.minAge = preferences.getMinAge();
        this.maxAge = preferences.getMaxAge();
        this.location = preferences.getLocation();
        this.updatedAt = preferences.getUpdatedAt();
    }
}
