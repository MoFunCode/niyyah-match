package com.niyyahmatch.niyyahmatch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.niyyahmatch.niyyahmatch.dto.CreateUserRequest;
import com.niyyahmatch.niyyahmatch.dto.LoginRequest;
import com.niyyahmatch.niyyahmatch.dto.SendMessageRequest;
import com.niyyahmatch.niyyahmatch.dto.SwipeRequest;
import com.niyyahmatch.niyyahmatch.entity.Gender;
import com.niyyahmatch.niyyahmatch.entity.Match;
import com.niyyahmatch.niyyahmatch.entity.MatchStatus;
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
class MessageControllerIntegrationTest {

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
    private String registerAndLogin(String email, String password, String firstName, String lastName, Gender gender) throws Exception {
        // Register user
        CreateUserRequest registerRequest = CreateUserRequest.builder()
                .email(email)
                .password(password)
                .firstName(firstName)
                .lastName(lastName)
                .dateOfBirth(LocalDate.of(1995, 5, 15))
                .gender(gender)
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

    /**
     * Helper method to create a mutual match between two users
     * Returns the matchId
     */
    private Long createMatch(String token1, String token2, String email1, String email2) throws Exception {
        Long user1Id = getUserIdByEmail(email1);
        Long user2Id = getUserIdByEmail(email2);

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
        SwipeRequest swipe2 = SwipeRequest.builder()
                .targetUserId(user1Id)
                .direction(SwipeDirection.RIGHT)
                .build();

        String matchResponse = mockMvc.perform(post("/api/matches/swipes")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(swipe2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(matchResponse).get("matchDetails").get("matchId").asLong();
    }

    // ========== POST /api/matches/{matchId}/messages Tests ==========

    @Test
    void sendMessage_WithValidMatch_Returns201WithMessageDetails() throws Exception {
        // Arrange: Create two users and match them
        String token1 = registerAndLogin("user1@example.com", "securePassword123", "User", "One", Gender.MALE);
        String token2 = registerAndLogin("user2@example.com", "securePassword123", "User", "Two", Gender.FEMALE);

        Long matchId = createMatch(token1, token2, "user1@example.com", "user2@example.com");

        // Act: User1 sends a message
        SendMessageRequest request = SendMessageRequest.builder()
                .content("Hello! Nice to match with you.")
                .build();

        // Assert: Message sent successfully with 201 status
        mockMvc.perform(post("/api/matches/" + matchId + "/messages")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.content").value("Hello! Nice to match with you."))
                .andExpect(jsonPath("$.senderId").value(getUserIdByEmail("user1@example.com")))
                .andExpect(jsonPath("$.senderFirstName").value("User"))
                .andExpect(jsonPath("$.sentAt").exists());
    }

    @Test
    void sendMessage_WithBlankContent_Returns400() throws Exception {
        // Arrange: Create two users and match them
        String token1 = registerAndLogin("user1@example.com", "securePassword123", "User", "One", Gender.MALE);
        String token2 = registerAndLogin("user2@example.com", "securePassword123", "User", "Two", Gender.FEMALE);

        Long matchId = createMatch(token1, token2, "user1@example.com", "user2@example.com");

        // Act: User1 tries to send blank message
        SendMessageRequest request = SendMessageRequest.builder()
                .content("   ")  // whitespace only
                .build();

        // Assert: Validation error
        mockMvc.perform(post("/api/matches/" + matchId + "/messages")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.content").exists());
    }

    @Test
    void sendMessage_WithContentExceeding1000Chars_Returns400() throws Exception {
        // Arrange: Create two users and match them
        String token1 = registerAndLogin("user1@example.com", "securePassword123", "User", "One", Gender.MALE);
        String token2 = registerAndLogin("user2@example.com", "securePassword123", "User", "Two", Gender.FEMALE);

        Long matchId = createMatch(token1, token2, "user1@example.com", "user2@example.com");

        // Act: User1 tries to send message > 1000 characters
        String longContent = "a".repeat(1001);
        SendMessageRequest request = SendMessageRequest.builder()
                .content(longContent)
                .build();

        // Assert: Validation error
        mockMvc.perform(post("/api/matches/" + matchId + "/messages")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.content").exists());
    }

    @Test
    void sendMessage_WhenNotPartOfMatch_Returns400() throws Exception {
        // Arrange: Create three users, match user1 and user2
        String token1 = registerAndLogin("user1@example.com", "securePassword123", "User", "One", Gender.MALE);
        String token2 = registerAndLogin("user2@example.com", "securePassword123", "User", "Two", Gender.FEMALE);
        String token3 = registerAndLogin("user3@example.com", "securePassword123", "User", "Three", Gender.MALE);

        Long matchId = createMatch(token1, token2, "user1@example.com", "user2@example.com");

        // Act: User3 (not part of match) tries to send message
        SendMessageRequest request = SendMessageRequest.builder()
                .content("I'm not part of this match")
                .build();

        // Assert: Authorization error
        mockMvc.perform(post("/api/matches/" + matchId + "/messages")
                        .header("Authorization", "Bearer " + token3)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You are not part of this match"));
    }

    @Test
    void sendMessage_WhenMatchNotFound_Returns404() throws Exception {
        // Arrange: Create user
        String token = registerAndLogin("user1@example.com", "securePassword123", "User", "One", Gender.MALE);

        // Act: Try to send message to non-existent match
        SendMessageRequest request = SendMessageRequest.builder()
                .content("Message to non-existent match")
                .build();

        // Assert: Match not found error
        mockMvc.perform(post("/api/matches/9999/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Match not found with id: 9999"));
    }

    @Test
    void sendMessage_WhenMatchIsUnmatched_Returns400() throws Exception {
        // Arrange: Create match, then unmatch it
        String token1 = registerAndLogin("user1@example.com", "securePassword123", "User", "One", Gender.MALE);
        String token2 = registerAndLogin("user2@example.com", "securePassword123", "User", "Two", Gender.FEMALE);

        Long matchId = createMatch(token1, token2, "user1@example.com", "user2@example.com");

        // Unmatch
        mockMvc.perform(post("/api/matches/" + matchId + "/unmatch")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isNoContent());

        // Act: Try to send message after unmatch
        SendMessageRequest request = SendMessageRequest.builder()
                .content("Message after unmatch")
                .build();

        // Assert: Cannot send to inactive match
        mockMvc.perform(post("/api/matches/" + matchId + "/messages")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot send messages to an inactive match"));
    }

    @Test
    void sendMessage_WithoutAuthentication_Returns403() throws Exception {
        // Act: Try to send message without authentication
        SendMessageRequest request = SendMessageRequest.builder()
                .content("Unauthenticated message")
                .build();

        // Assert: Forbidden (Spring Security blocks unauthenticated requests)
        mockMvc.perform(post("/api/matches/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void sendMessage_WithInvalidToken_Returns403() throws Exception {
        // Act: Try to send message with invalid JWT token
        SendMessageRequest request = SendMessageRequest.builder()
                .content("Message with bad token")
                .build();

        // Assert: Forbidden (invalid/malformed token)
        mockMvc.perform(post("/api/matches/1/messages")
                        .header("Authorization", "Bearer invalid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ========== GET /api/matches/{matchId}/messages Tests ==========

    @Test
    void getMessages_WithExistingMessages_Returns200WithPagedMessages() throws Exception {
        // Arrange: Create match and send 3 messages
        String token1 = registerAndLogin("user1@example.com", "securePassword123", "User", "One", Gender.MALE);
        String token2 = registerAndLogin("user2@example.com", "securePassword123", "User", "Two", Gender.FEMALE);

        Long matchId = createMatch(token1, token2, "user1@example.com", "user2@example.com");

        // Send 3 messages
        SendMessageRequest msg1 = SendMessageRequest.builder().content("First message").build();
        SendMessageRequest msg2 = SendMessageRequest.builder().content("Second message").build();
        SendMessageRequest msg3 = SendMessageRequest.builder().content("Third message").build();

        mockMvc.perform(post("/api/matches/" + matchId + "/messages")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(msg1)));

        mockMvc.perform(post("/api/matches/" + matchId + "/messages")
                .header("Authorization", "Bearer " + token2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(msg2)));

        mockMvc.perform(post("/api/matches/" + matchId + "/messages")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(msg3)));

        // Act & Assert: Retrieve messages
        mockMvc.perform(get("/api/matches/" + matchId + "/messages")
                        .header("Authorization", "Bearer " + token1)
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].content").value("First message"))
                .andExpect(jsonPath("$.content[1].content").value("Second message"))
                .andExpect(jsonPath("$.content[2].content").value("Third message"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void getMessages_WithEmptyConversation_Returns200WithEmptyPage() throws Exception {
        // Arrange: Create match but don't send any messages
        String token1 = registerAndLogin("user1@example.com", "securePassword123", "User", "One", Gender.MALE);
        String token2 = registerAndLogin("user2@example.com", "securePassword123", "User", "Two", Gender.FEMALE);

        Long matchId = createMatch(token1, token2, "user1@example.com", "user2@example.com");

        // Act & Assert: Retrieve messages from empty conversation
        mockMvc.perform(get("/api/matches/" + matchId + "/messages")
                        .header("Authorization", "Bearer " + token1)
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void getMessages_WithPagination_Returns200WithCorrectPage() throws Exception {
        // Arrange: Create match and send 25 messages (more than PAGE_SIZE of 20)
        String token1 = registerAndLogin("user1@example.com", "securePassword123", "User", "One", Gender.MALE);
        String token2 = registerAndLogin("user2@example.com", "securePassword123", "User", "Two", Gender.FEMALE);

        Long matchId = createMatch(token1, token2, "user1@example.com", "user2@example.com");

        // Send 25 messages
        for (int i = 1; i <= 25; i++) {
            SendMessageRequest msg = SendMessageRequest.builder()
                    .content("Message " + i)
                    .build();

            mockMvc.perform(post("/api/matches/" + matchId + "/messages")
                    .header("Authorization", "Bearer " + token1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(msg)));
        }

        // Act & Assert: Request page 0 (first 20 messages)
        mockMvc.perform(get("/api/matches/" + matchId + "/messages")
                        .header("Authorization", "Bearer " + token1)
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(0));

        // Act & Assert: Request page 1 (last 5 messages)
        mockMvc.perform(get("/api/matches/" + matchId + "/messages")
                        .header("Authorization", "Bearer " + token1)
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(1));
    }

    @Test
    void getMessages_WhenNotPartOfMatch_Returns400() throws Exception {
        // Arrange: Create three users, match user1 and user2
        String token1 = registerAndLogin("user1@example.com", "securePassword123", "User", "One", Gender.MALE);
        String token2 = registerAndLogin("user2@example.com", "securePassword123", "User", "Two", Gender.FEMALE);
        String token3 = registerAndLogin("user3@example.com", "securePassword123", "User", "Three", Gender.MALE);

        Long matchId = createMatch(token1, token2, "user1@example.com", "user2@example.com");

        // Act: User3 (not part of match) tries to read messages
        // Assert: Authorization error
        mockMvc.perform(get("/api/matches/" + matchId + "/messages")
                        .header("Authorization", "Bearer " + token3))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You are not part of this match"));
    }

    @Test
    void getMessages_WhenMatchNotFound_Returns404() throws Exception {
        // Arrange: Create user
        String token = registerAndLogin("user1@example.com", "securePassword123", "User", "One", Gender.MALE);

        // Act & Assert: Try to read messages from non-existent match
        mockMvc.perform(get("/api/matches/9999/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Match not found with id: 9999"));
    }

    @Test
    void getMessages_WhenMatchIsUnmatched_Returns400() throws Exception {
        // Arrange: Create match with messages, then unmatch it
        String token1 = registerAndLogin("user1@example.com", "securePassword123", "User", "One", Gender.MALE);
        String token2 = registerAndLogin("user2@example.com", "securePassword123", "User", "Two", Gender.FEMALE);

        Long matchId = createMatch(token1, token2, "user1@example.com", "user2@example.com");

        // Send a message
        SendMessageRequest msg = SendMessageRequest.builder().content("Test message").build();
        mockMvc.perform(post("/api/matches/" + matchId + "/messages")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(msg)));

        // Unmatch
        mockMvc.perform(post("/api/matches/" + matchId + "/unmatch")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isNoContent());

        // Act & Assert: Try to read messages after unmatch
        mockMvc.perform(get("/api/matches/" + matchId + "/messages")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot access messages for an inactive match"));
    }

    @Test
    void getMessages_WithoutAuthentication_Returns403() throws Exception {
        // Act & Assert: Try to read messages without authentication
        mockMvc.perform(get("/api/matches/1/messages"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMessages_WithInvalidToken_Returns403() throws Exception {
        // Act & Assert: Try to read messages with invalid JWT token
        mockMvc.perform(get("/api/matches/1/messages")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMessages_BothUsersCanReadSameConversation() throws Exception {
        // Arrange: Create match and send message from user1
        String token1 = registerAndLogin("user1@example.com", "securePassword123", "User", "One", Gender.MALE);
        String token2 = registerAndLogin("user2@example.com", "securePassword123", "User", "Two", Gender.FEMALE);

        Long matchId = createMatch(token1, token2, "user1@example.com", "user2@example.com");

        SendMessageRequest msg = SendMessageRequest.builder().content("Hello from User One").build();
        mockMvc.perform(post("/api/matches/" + matchId + "/messages")
                .header("Authorization", "Bearer " + token1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(msg)));

        // Act & Assert: Both users can read the same conversation
        // User1 reads
        mockMvc.perform(get("/api/matches/" + matchId + "/messages")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].content").value("Hello from User One"));

        // User2 reads (same conversation)
        mockMvc.perform(get("/api/matches/" + matchId + "/messages")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].content").value("Hello from User One"));
    }
}
