package com.niyyahmatch.niyyahmatch.service;

import com.niyyahmatch.niyyahmatch.entity.EducationLevel;
import com.niyyahmatch.niyyahmatch.entity.FilterPreferences;
import com.niyyahmatch.niyyahmatch.entity.Gender;
import com.niyyahmatch.niyyahmatch.entity.HijabPreference;
import com.niyyahmatch.niyyahmatch.entity.MatchStatus;
import com.niyyahmatch.niyyahmatch.entity.PrayerFrequency;
import com.niyyahmatch.niyyahmatch.entity.Sect;
import com.niyyahmatch.niyyahmatch.entity.User;
import com.niyyahmatch.niyyahmatch.exception.ResourceNotFoundException;
import com.niyyahmatch.niyyahmatch.repository.FilterPreferencesRepository;
import com.niyyahmatch.niyyahmatch.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class CandidateService {

    private static final int PAGE_SIZE = 10;
    private static final int MINIMUM_AGE = 18;

    private final UserRepository userRepository;
    private final FilterPreferencesRepository filterPreferencesRepository;

    public CandidateService(UserRepository userRepository, FilterPreferencesRepository filterPreferencesRepository) {
        this.userRepository = userRepository;
        this.filterPreferencesRepository = filterPreferencesRepository;
    }

    public Page<User> getCandidates(Long userId, int page) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Gender targetGender = currentUser.getGender() == Gender.MALE ? Gender.FEMALE : Gender.MALE;

        Optional<FilterPreferences> prefs = filterPreferencesRepository.findByUserId(userId);

        // Age is stored as dateOfBirth, so we convert age preferences to birth date bounds.
        // Earlier date = older person. The 18+ cutoff is always enforced as a hard floor.
        LocalDate eighteenPlusCutoff = LocalDate.now().minusYears(MINIMUM_AGE);

        LocalDate maxBirthDate = prefs
                .map(FilterPreferences::getMinAge)
                .map(minAge -> LocalDate.now().minusYears(minAge))
                .map(prefDate -> prefDate.isBefore(eighteenPlusCutoff) ? prefDate : eighteenPlusCutoff)
                .orElse(eighteenPlusCutoff);

        LocalDate minBirthDate = prefs
                .map(FilterPreferences::getMaxAge)
                .map(maxAge -> LocalDate.now().minusYears(maxAge))
                .orElse(null);

        // Extract Islamic lifestyle filters - null if no preference set
        String location = prefs.map(FilterPreferences::getLocation).orElse(null);
        Sect sect = prefs.map(FilterPreferences::getSect).orElse(null);
        PrayerFrequency prayerFrequency = prefs.map(FilterPreferences::getMinPrayerFrequency).orElse(null);
        EducationLevel educationLevel = prefs.map(FilterPreferences::getMinEducationLevel).orElse(null);
        HijabPreference hijabPreference = prefs.map(FilterPreferences::getHijabPreference).orElse(null);

        Pageable pageable = PageRequest.of(page, PAGE_SIZE);
        return userRepository.findCandidates(userId, targetGender, location, minBirthDate, maxBirthDate,
                MatchStatus.ACTIVE, sect, prayerFrequency, educationLevel, hijabPreference, pageable);
    }
}
