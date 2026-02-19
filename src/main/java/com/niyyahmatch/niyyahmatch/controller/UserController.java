package com.niyyahmatch.niyyahmatch.controller;

import com.niyyahmatch.niyyahmatch.dto.CreateUserRequest;
import com.niyyahmatch.niyyahmatch.dto.FilterPreferencesRequest;
import com.niyyahmatch.niyyahmatch.dto.FilterPreferencesResponse;
import com.niyyahmatch.niyyahmatch.dto.UpdateUserRequest;
import com.niyyahmatch.niyyahmatch.dto.UserResponse;
import com.niyyahmatch.niyyahmatch.entity.FilterPreferences;
import com.niyyahmatch.niyyahmatch.entity.User;
import com.niyyahmatch.niyyahmatch.exception.ResourceNotFoundException;
import com.niyyahmatch.niyyahmatch.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@RestController()
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    public UserController(UserService userService){
        this.userService = userService;
    }

    @PostMapping("/register")
        public UserResponse registerNewUser(@Valid @RequestBody CreateUserRequest request){
        // Step 1: Convert DTO → Entity
        User user = User.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .location(request.getLocation())
                .bio(request.getBio())
                .profilePhotoUrl(request.getProfilePhotoUrl())
                .sect(request.getSect())
                .prayerFrequency(request.getPrayerFrequency())
                .educationLevel(request.getEducationLevel())
                .hijabStatus(request.getHijabStatus())
                .build();

        User savedUser = userService.registerUser(user);
        // Convert Entity → DTO and return
        return new UserResponse(savedUser);
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable Long id){
        Optional<User> userOptional = userService.findUserById(id);
        if(userOptional.isPresent()){
            return new UserResponse(userOptional.get());
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: " + id);
        }
    }

    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request){
        // Convert DTO → Entity
        User updatedUser = User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .location(request.getLocation())
                .bio(request.getBio())
                .profilePhotoUrl(request.getProfilePhotoUrl())
                .sect(request.getSect())
                .prayerFrequency(request.getPrayerFrequency())
                .educationLevel(request.getEducationLevel())
                .hijabStatus(request.getHijabStatus())
                .build();

        // Call service to update
        User savedUser = userService.updateUser(id, updatedUser);

        // Convert Entity → DTO and return
        return new UserResponse(savedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id){
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences")
    public FilterPreferencesResponse getPreferences() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        FilterPreferences preferences = userService.getPreferences(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No preferences found. Use PUT /api/users/preferences to set them."));
        return new FilterPreferencesResponse(preferences);
    }

    @PutMapping("/preferences")
    public FilterPreferencesResponse savePreferences(@Valid @RequestBody FilterPreferencesRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        FilterPreferences saved = userService.savePreferences(userId, request.getMinAge(), request.getMaxAge(), request.getLocation(),
                request.getSect(), request.getMinPrayerFrequency(), request.getMinEducationLevel(), request.getHijabPreference());
        return new FilterPreferencesResponse(saved);
    }
}
