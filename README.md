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

**Next Up:**
- 🔄 Match entity and relationships
- 🔄 Match lock enforcement logic
- 🔄 Swipe system with mutual match detection

**Planned:**
- ⏳ Daily swipe tracking and limits
- ⏳ Messaging system
- ⏳ Filter preferences
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
}
```

## Project Structure

```
src/main/java/com/niyyahmatch/niyyahmatch/
├── config/                    # Security & JWT configuration
│   ├── JwtUtil.java          # JWT token generation and validation
│   ├── JwtAuthenticationFilter.java
│   └── SecurityConfig.java
├── controller/                # REST API endpoints
│   ├── AuthController.java   # Login endpoint
│   └── UserController.java   # User CRUD operations
├── dto/                       # Data Transfer Objects
│   ├── CreateUserRequest.java
│   ├── UpdateUserRequest.java
│   ├── UserResponse.java
│   ├── LoginRequest.java
│   └── LoginResponse.java
├── entity/                    # JPA entities
│   ├── User.java
│   └── Gender.java
├── exception/                 # Custom exceptions & global handler
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── DuplicateResourceException.java
├── repository/                # Data access layer
│   └── UserRepository.java
├── service/                   # Business logic
│   └── UserService.java
├── validation/                # Custom validators
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

### Coming Soon
- `GET /api/swipes/candidates` - Get profiles to swipe on
- `POST /api/swipes` - Record a swipe (LIKE/PASS)
- `GET /api/matches/active` - Get current active match
- `POST /api/matches/{matchId}/unmatch` - End current match

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

### Phase 2: Core Matching (IN PROGRESS)
- [ ] Match entity with relationships
- [ ] Swipe functionality
- [ ] Match creation logic
- [ ] Match lock enforcement
- [ ] Mutual match detection

### Phase 3: User Experience
- [ ] Filter preferences system
- [ ] Profile management enhancements
- [ ] Messaging system
- [ ] Daily swipe limit tracking

### Phase 4: Polish & Launch
- [ ] 14-day prompt system
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
