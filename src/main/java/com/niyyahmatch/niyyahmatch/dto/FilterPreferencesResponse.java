package com.niyyahmatch.niyyahmatch.dto;

import com.niyyahmatch.niyyahmatch.entity.EducationLevel;
import com.niyyahmatch.niyyahmatch.entity.FilterPreferences;
import com.niyyahmatch.niyyahmatch.entity.HijabPreference;
import com.niyyahmatch.niyyahmatch.entity.PrayerFrequency;
import com.niyyahmatch.niyyahmatch.entity.Sect;
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
    private Sect sect;
    private PrayerFrequency minPrayerFrequency;
    private EducationLevel minEducationLevel;
    private HijabPreference hijabPreference;
    private LocalDateTime updatedAt;

    public FilterPreferencesResponse(FilterPreferences preferences) {
        this.minAge = preferences.getMinAge();
        this.maxAge = preferences.getMaxAge();
        this.location = preferences.getLocation();
        this.sect = preferences.getSect();
        this.minPrayerFrequency = preferences.getMinPrayerFrequency();
        this.minEducationLevel = preferences.getMinEducationLevel();
        this.hijabPreference = preferences.getHijabPreference();
        this.updatedAt = preferences.getUpdatedAt();
    }
}
