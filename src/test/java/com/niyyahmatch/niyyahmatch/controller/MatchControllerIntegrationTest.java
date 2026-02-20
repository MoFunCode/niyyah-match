package com.niyyahmatch.niyyahmatch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.niyyahmatch.niyyahmatch.dto.CreateUserRequest;
import com.niyyahmatch.niyyahmatch.dto.LoginRequest;
import com.niyyahmatch.niyyahmatch.dto.SwipeRequest;
import com.niyyahmatch.niyyahmatch.entity.Gender;
import com.niyyahmatch.niyyahmatch.entity.SwipeDirection;
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
class MatchControllerIntegrationTest {

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
                .bio("Test user bio")
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

    // ========== Tests ==========

    @Test
    void getActiveMatch_WhenUserHasNoMatch_Returns404() throws Exception {
        // Arrange: Create and login user
        String token = registerAndLogin("user1@example.com", "securePassword123", "User", "One");

        // Act & Assert: Get active match - should return 404
        mockMvc.perform(get("/api/matches/active")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No active match found"));
    }

    @Test
    void getActiveMatch_WhenUserHasActiveMatch_Returns200WithMatchDetails() throws Exception {
        // Arrange: Create two users
        String token1 = registerAndLogin("user1@example.com", "securePassword123", "User", "One");
        String token2 = registerAndLogin("user2@example.com", "securePassword123", "User", "Two");

        Long user2Id = getUserIdByEmail("user2@example.com");

        // User1 swipes RIGHT on User2
        SwipeRequest swipe1 = SwipeRequest.builder()
                .targetUserId(user2Id)
                .direction(SwipeDirection.RIGHT)
                .build();

        mockMvc.perform(post("/api/matches/swipes")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(swipe1)))
                .andExpect(status().isOk());

        // User2 swipes RIGHT on User1 (creates mutual match)
        Long user1Id = getUserIdByEmail("user1@example.com");

        SwipeRequest swipe2 = SwipeRequest.builder()
                .targetUserId(user1Id)
                .direction(SwipeDirection.RIGHT)
                .build();

        mockMvc.perform(post("/api/matches/swipes")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(swipe2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(true));

        // Act & Assert: User1 gets active match
        mockMvc.perform(get("/api/matches/active")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").exists())
                .andExpect(jsonPath("$.matchedUser").exists())
                .andExpect(jsonPath("$.matchedUser.email").value("user2@example.com"));
    }

    @Test
    void recordSwipe_SwipeRight_NoMutualMatch_Returns200WithMatchedFalse() throws Exception {
        // Arrange: Create two users
        String token1 = registerAndLogin("user1@example.com", "securePassword123", "User", "One");
        registerAndLogin("user2@example.com", "securePassword123", "User", "Two");

        Long user2Id = getUserIdByEmail("user2@example.com");

        // Act: User1 swipes RIGHT on User2 (User2 hasn't swiped yet)
        SwipeRequest swipeRequest = SwipeRequest.builder()
                .targetUserId(user2Id)
                .direction(SwipeDirection.RIGHT)
                .build();

        // Assert: No match created
        mockMvc.perform(post("/api/matches/swipes")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(swipeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(false));
    }

    @Test
    void recordSwipe_MutualMatch_Returns200WithMatchedTrueAndMatchDetails() throws Exception {
        // Arrange: Create two users
        String token1 = registerAndLogin("user1@example.com", "securePassword123", "User", "One");
        String token2 = registerAndLogin("user2@example.com", "securePassword123", "User", "Two");

        Long user1Id = getUserIdByEmail("user1@example.com");
        Long user2Id = getUserIdByEmail("user2@example.com");

        // User1 swipes RIGHT on User2
        SwipeRequest swipe1 = SwipeRequest.builder()
                .targetUserId(user2Id)
                .direction(SwipeDirection.RIGHT)
                .build();

        mockMvc.perform(post("/api/matches/swipes")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(swipe1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(false));

        // Act: User2 swipes RIGHT on User1 (creates mutual match)
        SwipeRequest swipe2 = SwipeRequest.builder()
                .targetUserId(user1Id)
                .direction(SwipeDirection.RIGHT)
                .build();

        // Assert: Match created
        mockMvc.perform(post("/api/matches/swipes")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(swipe2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(true))
                .andExpect(jsonPath("$.matchDetails.matchId").exists())
                .andExpect(jsonPath("$.matchDetails.matchedUser.email").value("user1@example.com"));
    }

    @Test
    void recordSwipe_UserWithActiveMatch_Returns400() throws Exception {
        // Arrange: Create three users and a match between user1 and user2
        String token1 = registerAndLogin("user1@example.com", "securePassword123", "User", "One");
        String token2 = registerAndLogin("user2@example.com", "securePassword123", "User", "Two");
        registerAndLogin("user3@example.com", "securePassword123", "User", "Three");

        Long user1Id = getUserIdByEmail("user1@example.com");
        Long user2Id = getUserIdByEmail("user2@example.com");
        Long user3Id = getUserIdByEmail("user3@example.com");

        // Create match between user1 and user2
        SwipeRequest swipe1 = SwipeRequest.builder()
                .targetUserId(user2Id)
                .direction(SwipeDirection.RIGHT)
                .build();

        mockMvc.perform(post("/api/matches/swipes")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(swipe1)));

        SwipeRequest swipe2 = SwipeRequest.builder()
                .targetUserId(user1Id)
                .direction(SwipeDirection.RIGHT)
                .build();

        mockMvc.perform(post("/api/matches/swipes")
                .header("Authorization", "Bearer " + token2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(swipe2)));

        // Act: User1 tries to swipe on User3 while having active match
        SwipeRequest swipe3 = SwipeRequest.builder()
                .targetUserId(user3Id)
                .direction(SwipeDirection.RIGHT)
                .build();

        // Assert: Match lock enforcement - cannot swipe
        mockMvc.perform(post("/api/matches/swipes")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(swipe3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot swipe while you have an active match"));
    }

    @Test
    void recordSwipe_SwipeOnSelf_Returns400() throws Exception {
        // Arrange: Create user
        String token = registerAndLogin("user1@example.com", "securePassword123", "User", "One");
        Long userId = getUserIdByEmail("user1@example.com");

        // Act: User tries to swipe on themselves
        SwipeRequest swipeRequest = SwipeRequest.builder()
                .targetUserId(userId)
                .direction(SwipeDirection.RIGHT)
                .build();

        // Assert: Self-swipe prevention
        mockMvc.perform(post("/api/matches/swipes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(swipeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot swipe on yourself"));
    }

    @Test
    void recordSwipe_DuplicateSwipe_Returns400() throws Exception {
        // Arrange: Create two users
        String token1 = registerAndLogin("user1@example.com", "securePassword123", "User", "One");
        registerAndLogin("user2@example.com", "securePassword123", "User", "Two");

        Long user2Id = getUserIdByEmail("user2@example.com");

        // User1 swipes RIGHT on User2
        SwipeRequest swipe1 = SwipeRequest.builder()
                .targetUserId(user2Id)
                .direction(SwipeDirection.RIGHT)
                .build();

        mockMvc.perform(post("/api/matches/swipes")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(swipe1)));

        // Act: User1 tries to swipe on User2 again
        SwipeRequest swipe2 = SwipeRequest.builder()
                .targetUserId(user2Id)
                .direction(SwipeDirection.LEFT)
                .build();

        // Assert: Duplicate swipe prevention
        mockMvc.perform(post("/api/matches/swipes")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(swipe2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You already swiped on this user"));
    }

    @Test
    void unmatch_SuccessfulUnmatch_Returns204() throws Exception {
        // Arrange: Create match between user1 and user2
        String token1 = registerAndLogin("user1@example.com", "securePassword123", "User", "One");
        String token2 = registerAndLogin("user2@example.com", "securePassword123", "User", "Two");

        Long user1Id = getUserIdByEmail("user1@example.com");
        Long user2Id = getUserIdByEmail("user2@example.com");

        // Create mutual match
        SwipeRequest swipe1 = SwipeRequest.builder()
                .targetUserId(user2Id)
                .direction(SwipeDirection.RIGHT)
                .build();

        mockMvc.perform(post("/api/matches/swipes")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(swipe1)));

        SwipeRequest swipe2 = SwipeRequest.builder()
                .targetUserId(user1Id)
                .direction(SwipeDirection.RIGHT)
                .build();

        String matchResponse = mockMvc.perform(post("/api/matches/swipes")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(swipe2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long matchId = objectMapper.readTree(matchResponse).get("matchDetails").get("matchId").asLong();

        // Act & Assert: User1 unmatches
        mockMvc.perform(post("/api/matches/" + matchId + "/unmatch")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isNoContent());

        // Verify: User1 no longer has active match
        mockMvc.perform(get("/api/matches/active")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isNotFound());
    }

    @Test
    void unmatch_UnauthorizedUser_Returns400() throws Exception {
        // Arrange: Create match between user1 and user2, and user3 (not in match)
        String token1 = registerAndLogin("user1@example.com", "securePassword123", "User", "One");
        String token2 = registerAndLogin("user2@example.com", "securePassword123", "User", "Two");
        String token3 = registerAndLogin("user3@example.com", "securePassword123", "User", "Three");

        Long user1Id = getUserIdByEmail("user1@example.com");
        Long user2Id = getUserIdByEmail("user2@example.com");

        // Create mutual match between user1 and user2
        SwipeRequest swipe1 = SwipeRequest.builder()
                .targetUserId(user2Id)
                .direction(SwipeDirection.RIGHT)
                .build();

        mockMvc.perform(post("/api/matches/swipes")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(swipe1)));

        SwipeRequest swipe2 = SwipeRequest.builder()
                .targetUserId(user1Id)
                .direction(SwipeDirection.RIGHT)
                .build();

        String matchResponse = mockMvc.perform(post("/api/matches/swipes")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(swipe2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long matchId = objectMapper.readTree(matchResponse).get("matchDetails").get("matchId").asLong();

        // Act & Assert: User3 (not part of match) tries to unmatch
        mockMvc.perform(post("/api/matches/" + matchId + "/unmatch")
                        .header("Authorization", "Bearer " + token3))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You are not part of this match"));
    }

    @Test
    void unmatch_AlreadyUnmatched_Returns400() throws Exception {
        // Arrange: Create match and unmatch it
        String token1 = registerAndLogin("user1@example.com", "securePassword123", "User", "One");
        String token2 = registerAndLogin("user2@example.com", "securePassword123", "User", "Two");

        Long user1Id = getUserIdByEmail("user1@example.com");
        Long user2Id = getUserIdByEmail("user2@example.com");

        // Create mutual match
        SwipeRequest swipe1 = SwipeRequest.builder()
                .targetUserId(user2Id)
                .direction(SwipeDirection.RIGHT)
                .build();

        mockMvc.perform(post("/api/matches/swipes")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(swipe1)));

        SwipeRequest swipe2 = SwipeRequest.builder()
                .targetUserId(user1Id)
                .direction(SwipeDirection.RIGHT)
                .build();

        String matchResponse = mockMvc.perform(post("/api/matches/swipes")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(swipe2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long matchId = objectMapper.readTree(matchResponse).get("matchDetails").get("matchId").asLong();

        // Unmatch once
        mockMvc.perform(post("/api/matches/" + matchId + "/unmatch")
                .header("Authorization", "Bearer " + token1));

        // Act & Assert: Try to unmatch again
        mockMvc.perform(post("/api/matches/" + matchId + "/unmatch")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Match is not active"));
    }
}
