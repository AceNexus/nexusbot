# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

NexusBot is a LINE Bot application built with Spring Boot 3.4.3 and Java 17/21. It integrates with LINE Messaging API (SDK 6.0.0) to handle various message types and provides AI-powered responses through the Groq API. Features a simplified architecture with Flex Message interactive menus.

## Common Development Commands

### Build & Run
- **Build**: `./gradlew build`
- **Run tests**: `./gradlew test`
- **Build and test**: `./gradlew build test`
- **Run locally**: `./gradlew bootRun`
- **Create executable JAR**: `./gradlew bootJar` (outputs to `build/libs/`)

### Testing
- **Single test class**: `./gradlew test --tests ClassName`
- **Test with debug**: `./gradlew test --debug-jvm`

### Local Development
- Uses H2 in-memory database by default (local profile)
- H2 Console available at: `http://localhost:5001/h2-console`
- Application runs on port 5001 (configurable via `SERVER_PORT`)
- **Java Environment**: Requires Java 17+ (configured for Java 17, tested with Java 21)
- **Windows Development**: May need to set JAVA_HOME for Gradle: 
  ```bash
  export JAVA_HOME="/c/Program Files/Java/jdk-17"
  export PATH="/c/Program Files/Java/jdk-17/bin:$PATH"
  ```

### Database Migration
- **Flyway Integration**: Database schema managed through Flyway migrations
- **Cross-Database Compatibility**: Single migration script works across H2 (local/test) and MySQL (dev/prod)
- **Migration Scripts**: Located in `src/main/resources/db/migration`
- **DDL Auto Strategy**: 
  - All environments: `validate` mode (no auto-generation)
  - Local/Test: `baseline-on-migrate: true` for existing databases
  - Dev/Prod: `baseline-on-migrate: false` for strict migration validation
- **Schema Consistency**: Identical schema structure across all environments
- **SQL Compatibility**: Uses standard SQL syntax supported by both H2 and MySQL

## Architecture Overview

### Core Components

**Event Processing Flow**:
1. `LineBotController` receives LINE webhook requests
2. `EventHandlerService` routes events to specific handlers
3. Event-specific handlers process different event types:
   - `MessageEventHandler` - handles all message types
   - `PostbackEventHandler` - handles button interactions
   - `FollowEventHandler` - handles follow/unfollow events
   - `GroupEventHandler` - handles group join/leave events

**Message Processing Pipeline**:
1. `MessageEventHandler` receives message events
2. Routes to `MessageProcessorService` based on message type
3. For text messages:
   - First checks for predefined commands (menu, 選單)
   - Falls back to AI processing via `GroqService`
   - Async processing with fallback responses

**Domain-Driven Package Structure**:
- `ai/` - AI service interface (`AIService`) and Groq implementation (`AIServiceImpl`)
- `chatroom/` - Chat room management (`ChatRoomManager` interface, `ChatRoomManagerImpl`)
- `template/` - Message template generation (`MessageTemplateProvider` interface, `MessageTemplateProviderImpl`)
- `service/` - Core application services (`MessageService`, `MessageProcessorService`, `EventHandlerService`)

**Service Layer Architecture**:
- All major services follow interface-implementation pattern with `Impl` suffix
- `MessageProcessorService` orchestrates message processing (predefined commands → AI fallback)
- Dependency injection uses interfaces for loose coupling and testability

### Configuration Management

**Profile-based Configuration**:
- `bootstrap.yml` - base configuration
- `bootstrap-local.yml` - local development (H2, no Eureka)
- `bootstrap-dev.yml` - development environment
- `bootstrap-prod.yml` - production environment

**Key Configuration Classes**:
- `LineBotConfig` - LINE Bot SDK 6.0.0 configuration with `LineMessagingClient`
- `ConfigValidator` - validates configuration on startup
- Properties classes in `config.properties` package

### Message Types Supported
- Text messages (with AI responses)
- Images, stickers, videos, audio files
- File uploads, location sharing
- All responses handled by `MessageTemplateProvider` implementations

### Security & Validation
- `SignatureValidator` - validates LINE webhook signatures
- Request signature verification (can be disabled for development)

### AI Integration
- Groq API integration for chat responses
- Configurable model (default: llama-3.1-8b-instant)
- System prompt configured for Traditional Chinese responses
- 15-second timeout with graceful fallback

## Development Guidelines

### Code Structure
- **Domain-based packages**: `ai/`, `chatroom/`, `template/` for domain logic; `service/`, `handler/`, `controller/` for technical concerns
- **Interface-implementation pattern**: All major services have interfaces with `Impl` suffix implementations
- Use Lombok for boilerplate reduction (`@RequiredArgsConstructor`, etc.)
- Event handlers should be lightweight and delegate to services
- All external API calls should have timeout and error handling
- **Simplicity over complexity**: Prefer direct switch statements over complex abstractions
- **Interface segregation**: Keep interfaces focused on specific domain responsibilities

### SOLID Principles

**S - Single Responsibility Principle (SRP)**
- Each class should have only one reason to change
- Examples: `MessageService` only handles LINE API communication, `AIServiceImpl` only handles AI integration
- Event handlers focus solely on routing, business logic stays in services

**O - Open/Closed Principle (OCP)**
- Classes should be open for extension, closed for modification
- Use interfaces for external integrations (e.g., `AIService` can have different implementations beyond Groq)
- Template method pattern in `MessageTemplateProviderImpl` allows extending message types without modification

**L - Liskov Substitution Principle (LSP)**
- Derived classes must be substitutable for their base classes
- All service implementations should honor their interface contracts
- Mock services in tests should behave identically to production services

**I - Interface Segregation Principle (ISP)**
- Clients should not depend on interfaces they don't use
- Keep service interfaces focused and specific to their domain
- Avoid monolithic service interfaces with unrelated methods

**D - Dependency Inversion Principle (DIP)**
- High-level modules should not depend on low-level modules - both should depend on abstractions
- Use Spring's dependency injection with `@RequiredArgsConstructor`
- Depend on interfaces, not concrete implementations where possible
- Example: Services depend on Repository interfaces, not JPA implementations

### Configuration
- Add new configuration properties to appropriate `Properties` classes
- Validate critical configuration in `ConfigValidator`
- Use profile-specific files for environment differences

### LINE Bot SDK Considerations
- **Current Version**: 6.0.0 (downgraded from 9.8.0 for stability)
- **API Changes**: Uses `LineMessagingClient` instead of `MessagingApiClient`
- **Flex Message Support**: Full Flex Message capabilities with SDK 6.x APIs
- **Import Paths**: Use `com.linecorp.bot.model.*` instead of `com.linecorp.bot.messaging.*`
- **Message Construction**: Uses `ReplyMessage` class for sending responses

### Testing
- Tests located in `src/test/java`
- Use `application-test.yml` for test configuration
- JUnit 5 with Spring Boot Test support

### Deployment
- Automated deployment via GitHub Actions on version tags (`v*.*.*`)
- Builds with Java 21, deploys to EC2 via Docker
- JAR versioning based on Git tags
- Uses `./gradlew bootJar` for executable JAR creation

### Database Design

**Entity Structure**:
- `ChatRoom` - Per-room AI settings with lazy record creation
  - Tracks AI enabled/disabled state for each LINE user/group
  - Supports both individual and group conversations
- `ConversationHistory` - Multi-turn conversation tracking (planned)

**Data Access**:
- JPA/Hibernate with `@Entity` annotations
- Repository pattern with `ChatRoomRepository`
- Database migration managed by Flyway
- Schema validation via `ddl-auto: validate` (no auto-generation in any environment)

### Menu Architecture

**Flex Message System** - Template-based approach:
- **Template Pattern**: `MessageTemplateProvider` creates different message types
- **Direct Handler Processing**: `PostbackEventHandler` processes button interactions
- **Material Design Theme**: Consistent color scheme across all menus
- **Menu Types**: Main menu, AI settings, help menu
- **Postback Actions**: Structured data format (`action=toggle_ai`, `action=main_menu`)

**Event Processing Flow**:
```
PostbackEventHandler
├── Parse action from postback data
├── Delegate to ChatRoomManager for state changes
└── Return appropriate template via MessageTemplateProvider
```