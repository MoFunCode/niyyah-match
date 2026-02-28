# NiyyahMatch

I'm building a Muslim matchmaking app that works differently. Instead of endless swiping, you can only have **one active match at a time**. No browsing while you're talking to someone. Make a decision, then move forward.

## Why This Exists

Modern dating apps have a paradox of choice problem - too many options leads to decision paralysis and shallow connections. I wanted to build something that encourages intentional decision-making instead of endless browsing.

## What's Working

**The backend is production-ready.** All core features are complete, 73/73 tests passing, ready for frontend integration.

**Core Features:**
- ✅ **Match lock system** - can only have one active match at a time (the main differentiator)
- ✅ **JWT authentication** - BCrypt password hashing, secure token-based auth
- ✅ **Smart filtering** - 7 Islamic filters (age, location, sect, prayer frequency, education, hijab preference)
- ✅ **Daily swipe quota** - 12 swipes per day, resets at midnight UTC
- ✅ **In-match messaging** - paginated conversations, blocked after unmatch
- ✅ **73 passing tests** - 57 integration tests + 15 unit tests + 1 app test
- ✅ **Production-ready setup** - CORS configured, environment-based secrets, database indexes

**Tech Stack:** Java 17, Spring Boot 4.0.1, Spring Security, PostgreSQL 15, JPA/Hibernate, Maven

**What's Next:** React frontend, then private beta with 50-100 users.

## What I Learned

### Going from Tutorials to Production

The biggest difference between tutorial projects and production code? You have to think about things that tutorials skip.

**Here's what I figured out:**

**Authentication & Security:**
- JWT tokens are just JSON with a signature - the signature proves nobody tampered with the data
- BCrypt is intentionally slow (protects against brute force attacks)
- Never send passwords in API responses - that's what DTOs are for
- Environment variables for secrets (not hardcoding in repos)
- 12+ character passwords based on NIST guidelines (length > complexity)

**Custom Validators:**
- Built a `@MinAge` annotation from scratch using `Period.between()` to calculate age
- Learned about Java annotations (`@interface`, `@Retention`, `@Target`)
- Bean Validation runs before controller methods execute

**Complex Database Queries:**
- JPQL is similar to SQL but uses entity field names instead of column names
- Nullable parameters need `CAST` for PostgreSQL's type inference
- Composite indexes speed up queries that filter on multiple columns
- Created indexes on `(userId, swipedAt)` for quota counting, `(user1_id, status)` for match lock checks

**Testing:**
- Integration tests test the whole stack (HTTP → Controller → Service → Database)
- Unit tests test business logic in isolation (used Mockito for mocking)
- Wrote 73 tests because I wanted to catch bugs before they reach users

**Production Concerns:**
- CORS configuration (so React frontend can make requests)
- Pagination (loading all messages at once doesn't scale)
- UTC timestamps (timezone bugs are subtle and annoying)
- Database indexes on frequently queried columns
- Security audit checklist before deployment

### Cool Technical Problems I Solved

**Unicode Support for Arabic Names:**
Name validation uses `\p{L}\p{M}` regex to support names like محمد and فاطمة. This is a Muslim app - had to handle Arabic characters properly.

**Bidirectional Match Queries:**
The match lock has to check both `user1_id` and `user2_id` because either column could contain your user ID. Took me a while to realize I needed to check both sides.

**Swipe Quota Timezone Bug:**
Originally stored swipes in local time but queried based on UTC midnight. Caused quota counts to be wrong. Fixed by storing everything in UTC from the start.

## How It Works

### The Match Lock (Core Differentiator)

When you have an active match, you **cannot swipe on anyone else**. The system enforces this at the service layer before recording any swipe.

Here's the validation logic:

```java
@Transactional
public Optional<Match> recordSwipe(Long userId, Long targetUserId, SwipeDirection direction) {
    // Can't swipe on yourself
    if (userId.equals(targetUserId)) {
        throw new IllegalArgumentException("Cannot swipe on yourself");
    }

    // THE MATCH LOCK - enforces one match at a time
    if (hasActiveMatch(userId)) {
        throw new IllegalStateException("Cannot swipe while you have an active match");
    }

    // Can't swipe twice on same person
    if (swipeRepository.existsByUserIdAndTargetUserId(userId, targetUserId)) {
        throw new IllegalStateException("You already swiped on this user");
    }

    // Record the swipe and check for mutual match
    // ...
}
```

**Try to swipe while locked:**
```bash
curl -X POST http://localhost:8080/api/matches/swipes \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"targetUserId": 12, "direction": "RIGHT"}'

# Response (400 Bad Request):
{
  "message": "Cannot swipe while you have an active match"
}
```

You have to unmatch first. One conversation at a time.

### Smart Candidate Filtering

Built a JPQL query that applies 7 filters automatically:
- Opposite gender only
- Exclude users you've already swiped on
- Exclude your active match partner (enforces match lock)
- Age range (18+ always enforced as legal requirement)
- Location, sect, prayer frequency, education, hijab preference (all optional)

Users set their preferences once using `PUT /api/users/preferences`, and the system applies them on every candidate request. Paginated 10 per page.

### Testing the Business Logic

73 tests to make sure everything actually works:

**Integration Tests (57 tests):**
- AuthController: registration, login, JWT validation, protected endpoints
- UserController: CRUD operations, filter preferences, validation errors
- MatchController: match lock enforcement, mutual matches, unmatching
- SwipeController: candidate filtering, swipe quota
- MessageController: sending/reading messages, authorization checks

**Unit Tests (15 tests):**
- MatchService business logic: match lock, duplicate swipes, mutual match detection

The integration tests hit the full stack - they make real HTTP requests, go through Spring Security, hit the database. If the match lock breaks, the tests catch it.

## How I Built This

### Phase 1: Foundation
Learned Spring Boot project structure. Set up PostgreSQL, configured JPA, created the entity/repository/service/controller layers. Implemented JWT authentication and BCrypt password hashing.

### Phase 2: Core Matching
Built the match lock system - the main differentiator. Implemented mutual match detection, swipe lifecycle, and all the validations (self-swipe prevention, duplicate swipe prevention, match lock enforcement).

### Phase 3: Engagement Features
Added daily swipe quota (12/day with UTC midnight reset), in-match messaging with pagination, and the full Islamic filtering system (sect, prayer frequency, education, hijab).

### Phase 4: Production Prep
Wrote 73 integration and unit tests. Added CORS for frontend integration. Moved JWT secret to environment variables. Created database indexes on frequent queries. Completed security audit checklist.

### Phase 5: Launch (Next)
Build React frontend, run private beta with 50-100 users, iterate based on feedback.

## Why I Made Certain Decisions

**Match Lock Philosophy:**
Dating apps show you unlimited options, which creates decision paralysis. Limiting users to one match at a time forces intentional decisions. Either keep talking or move on - no browsing while you're in a conversation.

**PostgreSQL over H2:**
Building for production from the start. H2 is great for learning, but I wanted real-world deployment experience.

**Integration Tests over Just Unit Tests:**
Unit tests are faster, but integration tests catch more bugs. They test the whole flow - authentication, authorization, database queries, error handling.

**JPQL for Complex Queries:**
Spring Data's derived method names are clean for simple queries, but the candidate filtering query was too complex. JPQL gave me full control.

**UTC Timestamps Everywhere:**
Learned this the hard way after hitting a timezone bug with swipe quotas. Store everything in UTC, convert to user's timezone in the frontend.

## Project Structure

```
src/main/java/com/niyyahmatch/niyyahmatch/
├── config/                   # JWT & Spring Security setup
├── controller/               # REST API endpoints (Auth, User, Match, Swipe, Message)
├── dto/                      # Request/Response objects (passwords never exposed)
├── entity/                   # JPA entities (User, Match, Swipe, Message, FilterPreferences)
├── exception/                # Custom exceptions & global error handler
├── repository/               # Data access with Spring Data JPA
├── service/                  # Business logic (match lock, swipe quota, filtering)
└── validation/               # Custom validators (@MinAge)
```

## API Endpoints

> **Interactive docs:** `http://localhost:8080/swagger-ui.html` when the app is running.

**Authentication:**
- `POST /api/auth/login` - Get JWT token

**User Management:**
- `POST /api/users/register` - Create account
- `GET /api/users/{id}` - Get profile
- `PUT /api/users/{id}` - Update profile
- `DELETE /api/users/{id}` - Delete account

**Filtering:**
- `GET /api/users/preferences` - Get filter preferences
- `PUT /api/users/preferences` - Set filter preferences

**Matching:**
- `GET /api/matches/active` - Get current match
- `POST /api/matches/swipes` - Swipe (with match lock enforcement)
- `POST /api/matches/{matchId}/unmatch` - End match

**Candidates:**
- `GET /api/swipes/candidates?page=0` - Get filtered candidates
- `GET /api/swipes/remaining` - Check remaining swipes

**Messaging:**
- `POST /api/matches/{matchId}/messages` - Send message
- `GET /api/matches/{matchId}/messages?page=0` - Get conversation history

## Running This Locally

**Requirements:** Java 17+, PostgreSQL 15, Maven 3.6+

```bash
# Clone and setup database
git clone https://github.com/MoFunCode/niyyah-match.git
cd niyyah-match
createdb niyyahmatch

# Update src/main/resources/application.yml with your PostgreSQL username if needed
# Default is set to "postgres"

# Run the app
mvn spring-boot:run

# Run tests
mvn test
```

App starts at `http://localhost:8080`
API docs at `http://localhost:8080/swagger-ui.html`

