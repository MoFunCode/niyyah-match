# NiyyahMatch

An intentional Muslim matchmaking application that challenges the paradox of choice in modern dating apps through a unique **"match lock" system** - users can only have ONE active match at a time.

## What Makes This Different

NiyyahMatch encourages meaningful connections by requiring users to make intentional decisions about their matches before moving on. No endless swiping, no paradox of choice - just focused, purposeful connections.

### The Match Lock System
- Users can only maintain **ONE active match** at a time
- Cannot browse new profiles while an active match exists
- Must resolve current match (continue or unmatch) before proceeding
- Forces intentional connection over constant browsing

### Additional Features
- **Daily Swipe Limit**: 12 swipes per day to encourage quality over quantity
- **Seven Core Filters**: Age, distance, education, prayer frequency, sect, hijab preference
- **In-App Messaging**: Text messaging for active matches only
- **14-Day Soft Prompt**: Gentle nudge after two weeks to make a decision

## Tech Stack

**Backend:**
- Java 17
- Spring Boot 4.0.1
- Spring Security + JWT Authentication
- Bean Validation (JSR 380)
- PostgreSQL 15
- JPA/Hibernate
- Maven

**Planned Frontend:**
- React (separate repository)

## Current Status

🚧 **Backend MVP in Progress** 🚧

**Completed:**
- ✅ User authentication system with JWT tokens
- ✅ BCrypt password hashing (NIST SP 800-63B compliant)
- ✅ Complete CRUD operations for user profiles
- ✅ Input validation with custom validators
- ✅ Global exception handling with field-level errors
- ✅ Layered architecture (Controller → Service → Repository → Entity)
- ✅ DTO pattern for API security (passwords never exposed)
- ✅ **Match lock system - THE CORE DIFFERENTIATOR!** 🔒
- ✅ Match and Swipe entities with JPA relationships
- ✅ Swipe functionality with mutual match detection
- ✅ Match lock enforcement (users blocked from swiping with active match)
- ✅ GET /api/matches/active endpoint
- ✅ POST /api/matches/swipes endpoint with full validation
- ✅ POST /api/matches/{matchId}/unmatch endpoint (match lock release)
- ✅ **Complete match lifecycle working end-to-end!** (swipe → match → unmatch → swipe again)
- ✅ Filter preferences system (age range, location)
- ✅ GET /api/swipes/candidates endpoint with smart filtering
- ✅ **Phase 2 complete!**

**Next Up:**
- 🔄 Daily swipe quota system (12 swipes/day limit)

**Planned:**
- ⏳ Messaging system
- ⏳ 14-day prompt system

## Features Showcase

### 1. JWT Authentication with BCrypt Password Hashing

Users register with secure password hashing and authenticate using JWT tokens:

```java
@Service
public class UserService {
    private final PasswordEncoder passwordEncoder;

    public User registerUser(User user) {
        // Hash password with BCrypt before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }
}
```

**API Example:**
```bash
# Register new user
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "ahmed@example.com",
    "password": "securepassword123",
    "firstName": "Ahmed",
    "lastName": "Hassan",
    "dateOfBirth": "1995-03-15",
    "gender": "MALE"
  }'

# Login and receive JWT token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "ahmed@example.com",
    "password": "securepassword123"
  }'

# Response:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "email": "ahmed@example.com"
}
```

### 2. Custom Input Validation

Built a custom `@MinAge` annotation for age validation, demonstrating advanced Spring Boot knowledge:

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MinAgeValidator.class)
public @interface MinAge {
    int value();
    String message() default "Age requirement not met";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class MinAgeValidator implements ConstraintValidator<MinAge, LocalDate> {
    private int minAge;

    @Override
    public void initialize(MinAge constraintAnnotation) {
        this.minAge = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(LocalDate dateOfBirth, ConstraintValidatorContext context) {
        if (dateOfBirth == null) return true;
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        return age >= minAge;
    }
}
```

**Usage in DTO:**
```java
@Getter
@Setter
@Builder
public class CreateUserRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 254)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters")
    private String password;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @MinAge(value = 18, message = "You must be at least 18 years old to register")
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    // Additional fields...
}
```

**Validation in Action:**
```bash
# Invalid registration (user under 18)
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "young@example.com",
    "password": "securepass123",
    "firstName": "Ali",
    "lastName": "Ahmed",
    "dateOfBirth": "2010-01-01",
    "gender": "MALE"
  }'

# Response (400 Bad Request):
{
  "message": "Validation failed",
  "errors": {
    "dateOfBirth": "You must be at least 18 years old to register"
  }
}
```

### 3. DTO Pattern for API Security

Passwords are never exposed in API responses using the DTO pattern:

```java
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String email;
    // NO password field - security by design
    private String location;
    private String bio;
    private LocalDateTime createdAt;

    public UserResponse(User user) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.dateOfBirth = user.getDateOfBirth();
        this.gender = user.getGender();
        this.email = user.getEmail();
        this.location = user.getLocation();
        this.bio = user.getBio();
        this.createdAt = user.getCreatedAt();
    }
}
```

### 4. Global Exception Handling

Centralized exception handling with field-level error details:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );

        ErrorResponse errorResponse = new ErrorResponse("Validation failed", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
```

### 5. Match Lock System 🔒 (THE CORE DIFFERENTIATOR!)

The match lock enforces NiyyahMatch's unique approach - users can only have ONE active match at a time:

```java
@Service
public class MatchService {

    public boolean hasActiveMatch(Long userId) {
        // Check both user1 and user2 positions (bidirectional)
        return matchRepository.existsByUser1IdAndStatusOrUser2IdAndStatus(
            userId, MatchStatus.ACTIVE,
            userId, MatchStatus.ACTIVE
        );
    }

    @Transactional
    public Optional<Match> recordSwipe(Long userId, Long targetUserId, SwipeDirection direction) {
        // Validation 1: Can't swipe on yourself
        if (userId.equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot swipe on yourself");
        }

        // Validation 2: THE MATCH LOCK - enforces one match at a time
        if (hasActiveMatch(userId)) {
            throw new IllegalStateException("Cannot swipe while you have an active match");
        }

        // Validation 3: Can't swipe twice on same person
        if (swipeRepository.existsByUserIdAndTargetUserId(userId, targetUserId)) {
            throw new IllegalStateException("You already swiped on this user");
        }

        // Record the swipe
        Swipe swipe = Swipe.builder()
            .userId(userId)
            .targetUserId(targetUserId)
            .direction(direction)
            .swipedAt(LocalDateTime.now())
            .build();
        swipeRepository.save(swipe);

        // Check for mutual match (both swiped RIGHT)
        if (direction == SwipeDirection.RIGHT) {
            return checkAndCreateMatch(userId, targetUserId);
        }

        return Optional.empty();
    }
}
```

**API Examples:**

```bash
# Get current active match
curl -X GET http://localhost:8080/api/matches/active \
  -H "Authorization: Bearer <JWT_TOKEN>"

# Response if match exists:
{
  "matchId": 2,
  "status": "ACTIVE",
  "matchedAt": "2026-02-14T16:34:46.722514",
  "matchedUser": {
    "id": 10,
    "firstName": "Fatima",
    "lastName": "Ahmed",
    "email": "fatima@test.com",
    ...
  }
}

# Response if no active match (404):
{
  "status": 404,
  "error": "Resource Not Found",
  "message": "No active match found"
}

# Swipe RIGHT on a user
curl -X POST http://localhost:8080/api/matches/swipes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{
    "targetUserId": 11,
    "direction": "RIGHT"
  }'

# Response if mutual match created:
{
  "matched": true,
  "matchDetails": {
    "matchId": 2,
    "status": "ACTIVE",
    "matchedAt": "2026-02-14T16:34:46.722514",
    "matchedUser": { ... }
  }
}

# Response if no mutual match yet:
{
  "matched": false,
  "matchDetails": null
}

# Try to swipe while having active match (BLOCKED!):
curl -X POST http://localhost:8080/api/matches/swipes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{
    "targetUserId": 12,
    "direction": "RIGHT"
  }'

# Response (400 Bad Request):
{
  "timestamp": "2026-02-14T16:42:06.897026",
  "status": 400,
  "error": "Invalid Request",
  "message": "Cannot swipe while you have an active match"
}
```

**Validations Enforced:**
- ✅ Match lock enforcement (cannot swipe with active match)
- ✅ Self-swipe prevention (cannot swipe on yourself)
- ✅ Duplicate swipe prevention (cannot swipe twice on same user)
- ✅ Mutual match detection (both users swipe RIGHT → match created)

### 6. Filter Preferences & Candidate Discovery

Users set their preferences once - the system applies them automatically on every candidates request:

```bash
# Set filter preferences
curl -X PUT http://localhost:8080/api/users/preferences \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"minAge": 24, "maxAge": 32, "location": "New York"}'

# Response:
{
  "minAge": 24,
  "maxAge": 32,
  "location": "New York",
  "updatedAt": "2026-02-17T12:27:22.129068"
}

# Get candidates - preferences applied automatically
curl -X GET "http://localhost:8080/api/swipes/candidates?page=0" \
  -H "Authorization: Bearer <JWT_TOKEN>"

# Response:
{
  "content": [
    {
      "id": 8,
      "firstName": "Zahra",
      "age": 28,
      "location": "New York",
      "bio": "Medical student seeking sincere partnership",
      "profilePhotoUrl": null
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 10
}
```

**Filtering logic applied automatically:**
- Opposite gender only
- Excludes users already swiped on (never see the same profile twice)
- Excludes active match partner
- Age range filter (18+ hard floor always enforced)
- Location filter (skipped if no preference set)
- Paginated - 10 candidates per page

## Project Structure

```
src/main/java/com/niyyahmatch/niyyahmatch/
├── config/                          # Security & JWT configuration
│   ├── JwtUtil.java
│   ├── JwtAuthenticationFilter.java
│   └── SecurityConfig.java
├── controller/                      # REST API endpoints
│   ├── AuthController.java          # Login endpoint
│   ├── MatchController.java         # Match and swipe endpoints
│   ├── SwipeController.java         # Candidates endpoint
│   └── UserController.java          # User CRUD + preferences
├── dto/                             # Data Transfer Objects
│   ├── CandidateResponse.java       # Candidate profile (no sensitive data)
│   ├── CreateUserRequest.java
│   ├── ErrorResponse.java
│   ├── FilterPreferencesRequest.java
│   ├── FilterPreferencesResponse.java
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── MatchResponse.java
│   ├── SwipeRequest.java
│   ├── SwipeResponse.java
│   ├── UpdateUserRequest.java
│   └── UserResponse.java
├── entity/                          # JPA entities
│   ├── FilterPreferences.java       # User filter preferences
│   ├── Gender.java
│   ├── Match.java
│   ├── MatchStatus.java             # ACTIVE, UNMATCHED, EXPIRED
│   ├── Swipe.java
│   ├── SwipeDirection.java          # LEFT, RIGHT
│   └── User.java
├── exception/                       # Custom exceptions & global handler
│   ├── DuplicateResourceException.java
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
├── repository/                      # Data access layer
│   ├── FilterPreferencesRepository.java
│   ├── MatchRepository.java
│   ├── SwipeRepository.java
│   └── UserRepository.java          # Includes findCandidates JPQL query
├── service/                         # Business logic
│   ├── CandidateService.java        # Candidate filtering & pagination
│   ├── MatchService.java            # Match lock enforcement & swipe logic
│   └── UserService.java             # User management & preferences
├── validation/                      # Custom validators
│   ├── MinAge.java
│   └── MinAgeValidator.java
└── NiyyahmatchApplication.java
```

## API Endpoints

### Authentication
- `POST /api/auth/login` - Authenticate and receive JWT token

### User Management
- `POST /api/users/register` - Register new user (public)
- `GET /api/users/{id}` - Get user profile (requires JWT)
- `PUT /api/users/{id}` - Update user profile (requires JWT)
- `DELETE /api/users/{id}` - Delete user account (requires JWT)

### Match & Swipe System 🔒
- `GET /api/matches/active` - Get current active match (requires JWT)
- `POST /api/matches/swipes` - Record a swipe (LIKE/PASS) with match lock enforcement (requires JWT)
- `POST /api/matches/{matchId}/unmatch` - End current match and release match lock (requires JWT)

### Candidate Discovery
- `GET /api/swipes/candidates?page=0` - Get paginated candidates with filters applied (requires JWT)

### Filter Preferences
- `GET /api/users/preferences` - Get current filter preferences (requires JWT)
- `PUT /api/users/preferences` - Set or update filter preferences (requires JWT)

## Local Setup

### Prerequisites
- Java 17 or higher
- PostgreSQL 15
- Maven 3.6+

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/MoFunCode/niyyah-match.git
   cd niyyah-match
   ```

2. **Set up PostgreSQL database**
   ```bash
   createdb niyyahmatch
   ```

3. **Configure database connection**

   Update `src/main/resources/application.yml` if your PostgreSQL username differs from `postgres`:
   ```yaml
   spring:
     datasource:
       username: your_username
       password: your_password  # if you have one
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

5. **The application will start on** `http://localhost:8080`

6. **Run tests**
   ```bash
   mvn test
   ```

## Development Roadmap

### Phase 1: Foundation ✅ COMPLETE
- [x] Project setup and architecture
- [x] Database configuration
- [x] User entity and authentication
- [x] JWT-based security
- [x] Input validation system
- [x] Global exception handling

### Phase 2: Core Matching ✅ COMPLETE
- [x] Match entity with relationships
- [x] Swipe entity with swipe history
- [x] Swipe functionality (POST /api/matches/swipes)
- [x] Match creation logic with mutual detection
- [x] **Match lock enforcement - THE CORE DIFFERENTIATOR!**
- [x] GET /api/matches/active endpoint
- [x] POST /api/matches/{matchId}/unmatch endpoint
- [x] **Complete match lifecycle (swipe → match → unmatch → swipe again)**
- [x] Filter preferences system (age range, location)
- [x] GET /api/swipes/candidates endpoint with smart filtering
- [x] GET/PUT /api/users/preferences endpoints

### Phase 3: Engagement Features 🔄 IN PROGRESS
- [ ] Daily swipe quota (12 swipes/day limit)
- [ ] Daily quota reset scheduler
- [ ] Messaging system (send & receive within active match)
- [ ] Profile management enhancements

### Phase 4: Polish & Launch
- [ ] 14-day soft prompt system
- [ ] Comprehensive testing
- [ ] Performance optimization
- [ ] Private beta (50-100 users)

## Design Decisions

### Why Match Lock?
The paradox of choice in modern dating apps leads to endless swiping without meaningful connections. By limiting users to one active match at a time, NiyyahMatch encourages:
- Focused conversation with one person
- Intentional decision-making
- Reduced decision fatigue
- Quality over quantity in matches

### Security Standards
- **Password Storage**: BCrypt hashing following NIST SP 800-63B guidelines (12-128 character minimum, length over complexity)
- **Authentication**: JWT tokens with 24-hour expiration
- **API Security**: DTOs prevent password exposure, Spring Security protects endpoints
- **Input Validation**: Bean Validation with custom validators for domain-specific rules (e.g., 18+ age requirement)

### Unicode Support for International Names
Name validation uses `\p{L}\p{M}` regex patterns to support Arabic names (محمد, فاطمة) and other Unicode characters, ensuring cultural inclusivity.

## Contributing

This is a personal learning and portfolio project. Not currently accepting contributions, but feedback and suggestions are welcome!

## License

MIT License - see LICENSE file for details

---

**Built with intention.** Follow the journey: [GitHub](https://github.com/MoFunCode)
