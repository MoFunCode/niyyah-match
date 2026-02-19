package com.niyyahmatch.niyyahmatch.dto;

import com.niyyahmatch.niyyahmatch.entity.EducationLevel;
import com.niyyahmatch.niyyahmatch.entity.HijabPreference;
import com.niyyahmatch.niyyahmatch.entity.PrayerFrequency;
import com.niyyahmatch.niyyahmatch.entity.Sect;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FilterPreferencesRequest {

    @Min(value = 18, message = "Minimum age must be at least 18")
    @Max(value = 100, message = "Minimum age cannot exceed 100")
    private Integer minAge;

    @Min(value = 18, message = "Maximum age must be at least 18")
    @Max(value = 100, message = "Maximum age cannot exceed 100")
    private Integer maxAge;

    private String location;

    // All Islamic lifestyle filters are optional - null means no preference
    private Sect sect;

    private PrayerFrequency minPrayerFrequency;

    private EducationLevel minEducationLevel;

    private HijabPreference hijabPreference;
}
