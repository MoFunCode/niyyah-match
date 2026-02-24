package com.niyyahmatch.niyyahmatch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.niyyahmatch.niyyahmatch.dto.CreateUserRequest;
import com.niyyahmatch.niyyahmatch.dto.FilterPreferencesRequest;
import com.niyyahmatch.niyyahmatch.dto.LoginRequest;
import com.niyyahmatch.niyyahmatch.dto.UpdateUserRequest;
import com.niyyahmatch.niyyahmatch.entity.*;
import com.niyyahmatch.niyyahmatch.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SwipeRepository swipeRepository;

    @Autowired
    private FilterPreferencesRepository filterPreferencesRepository;

    @BeforeEach
    void setUp() {
        // Configure MockMvc with Spring Security
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Configure ObjectMapper for Java 8 Date/Time API
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        // Clean database in correct order (respecting foreign key constraints)
        messageRepository.deleteAll();
        matchRepository.deleteAll();
        swipeRepository.deleteAll();
        filterPreferencesRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test
        messageRepository.deleteAll();
        matchRepository.deleteAll();
        swipeRepository.deleteAll();
        filterPreferencesRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ========== Helper Methods ==========

    /**
     * Helper method to register a user and return their JWT token
     * Note: password must be at least 12 characters per validation rules
     */
    private String registerAndLogin(String email, String password, String firstName, String lastName) throws Exception {
        // Register user
        CreateUserRequest registerRequest = CreateUserRequest.builder()
                .email(email)
                .password(password)
                .firstName(firstName)
                .lastName(lastName)
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .gender(Gender.MALE)
                .location("New York")
                .bio("Test user bio for testing purposes")
                .build();

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Verify user exists
        assertTrue(userRepository.existsByEmail(email));

        // Login and get JWT token
        LoginRequest loginRequest = new LoginRequest(email, password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract and return token
        return objectMapper.readTree(response).get("token").asText();
    }

    /**
     * Helper method to get userId from email
     */
    private Long getUserIdByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow().getId();
    }

    // ========== GET /api/users/{id} Tests ==========

    @Test
    void getUserById_WithValidId_Returns200AndUserDetails() throws Exception {
        // Arrange: Register user
        String token = registerAndLogin("user@example.com", "securePassword123", "John", "Doe");
        Long userId = getUserIdByEmail("user@example.com");

        // Act & Assert: Get user by ID
        mockMvc.perform(get("/api/users/" + userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.password").doesNotExist()); // Password must not be exposed
    }

    @Test
    void getUserById_WithInvalidId_Returns404() throws Exception {
        // Arrange: Register user to get valid token
        String token = registerAndLogin("user@example.com", "securePassword123", "John", "Doe");

        // Act & Assert: Try to get non-existent user
        mockMvc.perform(get("/api/users/9999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 9999"));
    }

    @Test
    void getUserById_WithoutToken_Returns403() throws Exception {
        // Arrange: Register user
        registerAndLogin("user@example.com", "securePassword123", "John", "Doe");
        Long userId = getUserIdByEmail("user@example.com");

        // Act & Assert: Try to access endpoint without Authorization header
        mockMvc.perform(get("/api/users/" + userId))
                .andExpect(status().isForbidden());
    }

    // ========== PUT /api/users/{id} Tests ==========

    @Test
    void updateUser_WithValidData_Returns200AndUpdatedUser() throws Exception {
        // Arrange: Register user
        String token = registerAndLogin("user@example.com", "securePassword123", "John", "Doe");
        Long userId = getUserIdByEmail("user@example.com");

        // Create update request
        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .firstName("UpdatedJohn")
                .lastName("UpdatedDoe")
                .location("San Francisco")
                .bio("Updated bio with more than 10 characters")
                .build();

        // Act & Assert: Update user profile
        mockMvc.perform(put("/api/users/" + userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("UpdatedJohn"))
                .andExpect(jsonPath("$.lastName").value("UpdatedDoe"))
                .andExpect(jsonPath("$.location").value("San Francisco"))
                .andExpect(jsonPath("$.bio").value("Updated bio with more than 10 characters"));
    }

    @Test
    void updateUser_WithInvalidEmail_Returns400() throws Exception {
        // Arrange: Register user
        String token = registerAndLogin("user@example.com", "securePassword123", "John", "Doe");
        Long userId = getUserIdByEmail("user@example.com");

        // Create update request with invalid email
        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .email("invalid-email-format")
                .build();

        // Act & Assert: Validation should fail
        mockMvc.perform(put("/api/users/" + userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void updateUser_WithShortBio_Returns400() throws Exception {
        // Arrange: Register user
        String token = registerAndLogin("user@example.com", "securePassword123", "John", "Doe");
        Long userId = getUserIdByEmail("user@example.com");

        // Create update request with bio less than 10 characters
        UpdateUserRequest updateRequest = UpdateUserRequest.builder()
                .bio("Short")
                .build();

        // Act & Assert: Validation should fail
        mockMvc.perform(put("/api/users/" + userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.bio").exists());
    }

    // ========== DELETE /api/users/{id} Tests ==========

    @Test
    void deleteUser_SuccessfulDelete_Returns204() throws Exception {
        // Arrange: Register user
        String token = registerAndLogin("user@example.com", "securePassword123", "John", "Doe");
        Long userId = getUserIdByEmail("user@example.com");

        // Act: Delete user
        mockMvc.perform(delete("/api/users/" + userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Assert: Verify user was deleted
        mockMvc.perform(get("/api/users/" + userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_NonExistentUser_Returns404() throws Exception {
        // Arrange: Register user to get valid token
        String token = registerAndLogin("user@example.com", "securePassword123", "John", "Doe");

        // Act & Assert: Try to delete non-existent user
        mockMvc.perform(delete("/api/users/9999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 9999"));
    }

    // ========== GET /api/users/preferences Tests ==========

    @Test
    void getPreferences_WhenNoPreferencesSet_Returns404() throws Exception {
        // Arrange: Register user without setting preferences
        String token = registerAndLogin("user@example.com", "securePassword123", "John", "Doe");

        // Act & Assert: Try to get preferences that don't exist
        mockMvc.perform(get("/api/users/preferences")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No preferences found. Use PUT /api/users/preferences to set them."));
    }

    // ========== PUT /api/users/preferences Tests ==========

    @Test
    void savePreferences_WithValidData_Returns200AndPreferences() throws Exception {
        // Arrange: Register user
        String token = registerAndLogin("user@example.com", "securePassword123", "John", "Doe");

        // Create filter preferences request
        FilterPreferencesRequest preferencesRequest = new FilterPreferencesRequest();
        preferencesRequest.setMinAge(25);
        preferencesRequest.setMaxAge(35);
        preferencesRequest.setLocation("New York");
        preferencesRequest.setSect(Sect.SUNNI);
        preferencesRequest.setMinPrayerFrequency(PrayerFrequency.MOST_PRAYERS);
        preferencesRequest.setMinEducationLevel(EducationLevel.BACHELORS);
        preferencesRequest.setHijabPreference(HijabPreference.NO_PREFERENCE);

        // Act & Assert: Save preferences
        mockMvc.perform(put("/api/users/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(preferencesRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minAge").value(25))
                .andExpect(jsonPath("$.maxAge").value(35))
                .andExpect(jsonPath("$.location").value("New York"))
                .andExpect(jsonPath("$.sect").value("SUNNI"))
                .andExpect(jsonPath("$.minPrayerFrequency").value("MOST_PRAYERS"))
                .andExpect(jsonPath("$.minEducationLevel").value("BACHELORS"))
                .andExpect(jsonPath("$.hijabPreference").value("NO_PREFERENCE"));

        // Verify: Can now retrieve preferences
        mockMvc.perform(get("/api/users/preferences")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minAge").value(25));
    }

    @Test
    void savePreferences_WithInvalidAgeRange_Returns400() throws Exception {
        // Arrange: Register user
        String token = registerAndLogin("user@example.com", "securePassword123", "John", "Doe");

        // Create preferences with minAge < 18 (invalid)
        FilterPreferencesRequest preferencesRequest = new FilterPreferencesRequest();
        preferencesRequest.setMinAge(16); // Invalid - must be >= 18
        preferencesRequest.setMaxAge(30);

        // Act & Assert: Validation should fail
        mockMvc.perform(put("/api/users/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(preferencesRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.minAge").exists());
    }

    @Test
    void updatePreferences_OverwritesExisting_Returns200() throws Exception {
        // Arrange: Register user and save initial preferences
        String token = registerAndLogin("user@example.com", "securePassword123", "John", "Doe");

        FilterPreferencesRequest initialPreferences = new FilterPreferencesRequest();
        initialPreferences.setMinAge(25);
        initialPreferences.setMaxAge(35);
        initialPreferences.setLocation("New York");

        mockMvc.perform(put("/api/users/preferences")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(initialPreferences)));

        // Act: Update preferences with new values
        FilterPreferencesRequest updatedPreferences = new FilterPreferencesRequest();
        updatedPreferences.setMinAge(30);
        updatedPreferences.setMaxAge(40);
        updatedPreferences.setLocation("Los Angeles");

        // Assert: New values should overwrite old ones
        mockMvc.perform(put("/api/users/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedPreferences)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minAge").value(30))
                .andExpect(jsonPath("$.maxAge").value(40))
                .andExpect(jsonPath("$.location").value("Los Angeles"));
    }
}
