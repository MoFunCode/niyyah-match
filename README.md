 # NiyyahMatch

  An intentional Muslim matchmaking application that challenges the paradox of choice in modern dating apps through a
  unique "match lock" system - users can only have ONE active match at a time.

  ## About

  NiyyahMatch encourages meaningful connections by requiring users to make intentional decisions about their matches
  before moving on. No endless swiping, no paradox of choice - just focused, purposeful connections.

  ### Core Features (Planned)
  - **Match Lock System**: Only one active match at a time - must unmatch before browsing new profiles
  - **Daily Swipe Limit**: 12 swipes per day to encourage quality over quantity
  - **Seven Core Filters**: Age, distance, education, prayer frequency, sect, hijab preference
  - **In-App Messaging**: Text messaging for active matches only
  - **14-Day Soft Prompt**: Gentle nudge after two weeks to make a decision

  ## Tech Stack

  **Backend:**
  - Java 17+
  - Spring Boot 4.0.1
  - PostgreSQL 15
  - JPA/Hibernate
  - Maven

  **Planned Frontend:**
  - React (separate repository)

  ## Current Status

  🚧 **In Active Development** 🚧

  **Implemented:**
  - ✅ Spring Boot project initialization
  - ✅ PostgreSQL database configuration
  - ✅ User entity with profile fields
  - ✅ Package structure (layered architecture)

  **In Progress:**
  - 🔄 User authentication (JWT)
  - 🔄 Repository and Service layers
  - 🔄 REST API endpoints

  **Planned:**
  - ⏳ Match lock enforcement logic
  - ⏳ Daily swipe tracking
  - ⏳ Messaging system
  - ⏳ Filter preferences

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

  2. Set up PostgreSQL database
  createdb niyyahmatch
  3. Configure database connection

  3. Update src/main/resources/application.yml if your PostgreSQL username differs from postgres:
  spring:
    datasource:
      username: your_username
      password: your_password  # if you have one
  4. Run the application
  mvn spring-boot:run

  4. The application will start on http://localhost:8080
  5. Run tests
  mvn test

  Project Structure

  src/main/java/com/niyyahmatch/niyyahmatch/
  ├── config/         # Configuration classes (Security, CORS, etc.)
  ├── controller/     # REST API endpoints
  ├── service/        # Business logic layer
  ├── repository/     # Data access layer
  ├── entity/         # JPA entities (database models)
  └── dto/            # Data Transfer Objects

  Development Roadmap

  Phase 1: Foundation (Current)

  - Project setup
  - Database configuration
  - User entity
  - Authentication system

  Phase 2: Core Matching

  - Swipe functionality
  - Match creation logic
  - Match lock enforcement

  Phase 3: User Experience

  - Filter preferences
  - Profile management
  - Messaging system

  Phase 4: Polish & Launch

  - 14-day prompt system
  - Testing & bug fixes
  - Private beta (50-100 users)

  Contributing

  This is a personal learning project. Not currently accepting contributions, but feedback is welcome!
