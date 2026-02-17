package com.niyyahmatch.niyyahmatch.service;

import com.niyyahmatch.niyyahmatch.entity.FilterPreferences;
import com.niyyahmatch.niyyahmatch.entity.User;
import com.niyyahmatch.niyyahmatch.exception.DuplicateResourceException;
import com.niyyahmatch.niyyahmatch.exception.ResourceNotFoundException;
import com.niyyahmatch.niyyahmatch.repository.FilterPreferencesRepository;
import com.niyyahmatch.niyyahmatch.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FilterPreferencesRepository filterPreferencesRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, FilterPreferencesRepository filterPreferencesRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.filterPreferencesRepository = filterPreferencesRepository;
    }

    public User registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User updateUser(Long id, User updatedUser) {
        // Find existing user or throw exception
        User existingUser = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Update fields
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setFirstName(updatedUser.getFirstName());
        existingUser.setLastName(updatedUser.getLastName());
        existingUser.setDateOfBirth(updatedUser.getDateOfBirth());
        existingUser.setGender(updatedUser.getGender());
        existingUser.setLocation(updatedUser.getLocation());
        existingUser.setBio(updatedUser.getBio());
        existingUser.setProfilePhotoUrl(updatedUser.getProfilePhotoUrl());

        // Save and return updated user
        return userRepository.save(existingUser);
    }

    public void deleteUser(Long id) {
        // Check if user exists, throw exception if not found
        User existingUser = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userRepository.delete(existingUser);
    }

    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }

    public Optional<FilterPreferences> getPreferences(Long userId) {
        return filterPreferencesRepository.findByUserId(userId);
    }

    public FilterPreferences savePreferences(Long userId, Integer minAge, Integer maxAge, String location) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Update existing preferences or create new ones
        FilterPreferences preferences = filterPreferencesRepository.findByUserId(userId)
                .orElse(FilterPreferences.builder()
                        .user(user)
                        .createdAt(LocalDateTime.now())
                        .build());

        preferences.setMinAge(minAge);
        preferences.setMaxAge(maxAge);
        preferences.setLocation(location);
        preferences.setUpdatedAt(LocalDateTime.now());

        return filterPreferencesRepository.save(preferences);
    }
}
