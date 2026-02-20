package com.niyyahmatch.niyyahmatch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.niyyahmatch.niyyahmatch.dto.CreateUserRequest;
import com.niyyahmatch.niyyahmatch.dto.LoginRequest;
import com.niyyahmatch.niyyahmatch.entity.Gender;
import com.niyyahmatch.niyyahmatch.repository.MatchRepository;
import com.niyyahmatch.niyyahmatch.repository.MessageRepository;
import com.niyyahmatch.niyyahmatch.repository.SwipeRepository;
import com.niyyahmatch.niyyahmatch.repository.FilterPreferencesRepository;
import com.niyyahmatch.niyyahmatch.repository.UserRepository;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @SpringBootTest loads the full Spring application context (all beans, configs, etc.)
// @ActiveProfiles("test") tells Spring to use application-test.yml
@SpringBootTest
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    private MockMvc mockMvc; // Used to simulate HTTP requests

    @Autowired
    private WebApplicationContext context; // Spring web context for MockMvc setup

    private final ObjectMapper objectMapper = new ObjectMapper(); // Converts Java objects to JSON and vice versa

    @Autowired
    private UserRepository userRepository; // Access to database for setup/verification

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
        // Manually configure MockMvc with Spring Security filter chain
        // This is required in Spring Boot 4.0 (no longer auto-configured)
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity()) // Apply Spring Security filters
                .build();

        // Configure ObjectMapper for Java 8 Date/Time API
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        // Clean database in correct order (respecting foreign key constraints)
        messageRepository.deleteAll(); // Delete messages first (references matches)
        matchRepository.deleteAll(); // Delete matches second (references users)
        swipeRepository.deleteAll(); // Delete swipes (references users)
        filterPreferencesRepository.deleteAll(); // Delete filter preferences (references users)
        userRepository.deleteAll(); // Delete users last (referenced by all above)
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

    @Test
    void registerUser_WithValidData_Returns200AndUserResponse() throws Exception {
        // Arrange: Create valid registration request
        CreateUserRequest request = CreateUserRequest.builder()
                .email("ahmed@example.com")
                .password("securePassword123")
                .firstName("Ahmed")
                .lastName("Hassan")
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .gender(Gender.MALE)
                .location("New York")
                .bio("Looking for a meaningful connection")
                .build();

        // Act & Assert: Send POST request and verify response
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) // 200 OK
                .andExpect(jsonPath("$.email").value("ahmed@example.com"))
                .andExpect(jsonPath("$.firstName").value("Ahmed"))
                .andExpect(jsonPath("$.password").doesNotExist()); // Security: password never returned
    }

    @Test
    void registerUser_WithInvalidEmail_Returns400() throws Exception {
        // Arrange: Invalid email format
        CreateUserRequest request = CreateUserRequest.builder()
                .email("invalid-email") // Missing @ symbol
                .password("securePassword123")
                .firstName("Ahmed")
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .gender(Gender.MALE)
                .build();

        // Act & Assert: Expect validation error
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // 400 Bad Request
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void registerUser_WithShortPassword_Returns400() throws Exception {
        // Arrange: Password too short (less than 12 characters)
        CreateUserRequest request = CreateUserRequest.builder()
                .email("ahmed@example.com")
                .password("short") // Only 5 characters
                .firstName("Ahmed")
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .gender(Gender.MALE)
                .build();

        // Act & Assert: Expect validation error
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void registerUser_WithAgeUnder18_Returns400() throws Exception {
        // Arrange: User under 18 years old
        CreateUserRequest request = CreateUserRequest.builder()
                .email("young@example.com")
                .password("securePassword123")
                .firstName("Young")
                .dateOfBirth(LocalDate.now().minusYears(17)) // 17 years old
                .gender(Gender.MALE)
                .build();

        // Act & Assert: Expect age validation error
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.dateOfBirth").value(containsString("18 years old")));
    }

    @Test
    void registerUser_WithDuplicateEmail_Returns409() throws Exception {
        // Arrange: Register first user
        CreateUserRequest firstRequest = CreateUserRequest.builder()
                .email("duplicate@example.com")
                .password("securePassword123")
                .firstName("First")
                .lastName("User")
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .gender(Gender.MALE)
                .build();

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isOk()); // Verify first registration succeeded

        // Verify first user exists in database
        assertTrue(userRepository.existsByEmail("duplicate@example.com"));

        // Act: Try to register second user with same email
        CreateUserRequest duplicateRequest = CreateUserRequest.builder()
                .email("duplicate@example.com") // Same email
                .password("differentPassword123")
                .firstName("Second")
                .lastName("User")
                .dateOfBirth(LocalDate.of(1996, 6, 16))
                .gender(Gender.FEMALE)
                .build();

        // Assert: Expect conflict error
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict()) // 409 Conflict
                .andExpect(jsonPath("$.message").value(containsString("Email already exists")));
    }

    @Test
    void login_WithValidCredentials_Returns200AndJwtToken() throws Exception {
        // Arrange: Register a user first
        CreateUserRequest registerRequest = CreateUserRequest.builder()
                .email("login@example.com")
                .password("securePassword123")
                .firstName("Login")
                .lastName("User")
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .gender(Gender.MALE)
                .build();

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk()); // Verify registration succeeded

        // Verify user exists in database before attempting login
        assertTrue(userRepository.existsByEmail("login@example.com"));

        // Act: Login with correct credentials
        LoginRequest loginRequest = new LoginRequest("login@example.com", "securePassword123");

        // Assert: Expect JWT token in response
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_WithWrongPassword_Returns401() throws Exception {
        // Arrange: Register a user
        CreateUserRequest registerRequest = CreateUserRequest.builder()
                .email("wrongpass@example.com")
                .password("correctPassword123")
                .firstName("User")
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .gender(Gender.MALE)
                .build();

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // Act: Login with wrong password
        LoginRequest loginRequest = new LoginRequest("wrongpass@example.com", "wrongPassword123");

        // Assert: Expect unauthorized error
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()) // 401 Unauthorized
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void login_WithNonExistentEmail_Returns401() throws Exception {
        // Act: Login with email that doesn't exist in database
        LoginRequest loginRequest = new LoginRequest("notfound@example.com", "anyPassword123");

        // Assert: Expect unauthorized error (same as wrong password for security)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void accessProtectedEndpoint_WithoutToken_Returns403() throws Exception {
        // Act & Assert: Try to access protected endpoint without Authorization header
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isForbidden()); // 403 Forbidden (Spring Security default)
    }

    @Test
    void accessProtectedEndpoint_WithValidToken_Returns200() throws Exception {
        // Arrange: Register and login to get JWT token
        CreateUserRequest registerRequest = CreateUserRequest.builder()
                .email("protected@example.com")
                .password("securePassword123")
                .firstName("Protected")
                .lastName("User")
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .gender(Gender.MALE)
                .build();

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk()); // Verify registration succeeded

        // Verify user exists in database
        assertTrue(userRepository.existsByEmail("protected@example.com"));

        LoginRequest loginRequest = new LoginRequest("protected@example.com", "securePassword123");

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk()) // Verify login succeeded before extracting token
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract token from response (now guaranteed to exist)
        String token = objectMapper.readTree(response).get("token").asText();

        // Act & Assert: Access protected endpoint with valid JWT token
        mockMvc.perform(get("/api/matches/active")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()); // 404 because user has no active match (but auth worked!)
    }
}
