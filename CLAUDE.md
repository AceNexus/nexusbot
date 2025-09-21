# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

NexusBot is a LINE Bot application built with Spring Boot 3.4.3 and Java 17/21. It integrates with LINE Messaging API (
SDK 6.0.0) to handle various message types and provides AI-powered responses through the Groq API. Features a simplified
architecture with Flex Message interactive menus.

## Common Development Commands

### Build & Run

- **Build**: `./gradlew build`
- **Run tests**: `./gradlew test`
- **Build and test**: `./gradlew build test`
- **Run locally**: `./gradlew bootRun`
- **Create executable JAR**: `./gradlew bootJar` (outputs to `build/libs/`)
- **Clean build**: `./gradlew clean build`

### Testing

- **Single test class**: `./gradlew test --tests ClassName`
- **Test with debug**: `./gradlew test --debug-jvm`

### Local Development

- Uses H2 in-memory database by default (local profile)
- H2 Console available at: `http://localhost:5001/h2-console`
- Application runs on port 5001 (configurable via `SERVER_PORT`)
- **Java Environment**: Requires Java 17+ (configured for Java 17, tested with Java 21)
- **Critical**: Gradle must use Java 17+. Common issue is Gradle using Java 8, causing build failures
- **Windows Development**: Set correct Java environment for Gradle:
  ```bash
  # For Windows Command Prompt
  set JAVA_HOME="C:\Program Files\Java\jdk-17"
  
  # For Git Bash/WSL
  export JAVA_HOME="/c/Program Files/Java/jdk-17"
  export PATH="/c/Program Files/Java/jdk-17/bin:$PATH"
  
  # Then run Gradle commands
  ./gradlew build
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

1. `MessageEventHandler` receives message events and extracts `userId` from LINE payload
2. Routes to `MessageProcessorService` based on message type
3. For text messages:
    - Stores user message to `chat_messages` table immediately
    - First processes admin authentication commands via `AdminService`
    - Then checks for predefined commands (menu, 選單)
    - Falls back to AI processing via `AIService`
    - Async processing with fallback responses
    - Stores AI responses with analytics (processing time, model used)

**Domain-Driven Package Structure**:

- `ai/` - AI service interface (`AIService`) and Groq implementation (`AIServiceImpl`)
- `chatroom/` - Chat room management (`ChatRoomManager` interface, `ChatRoomManagerImpl`)
- `template/` - Message template generation (`MessageTemplateProvider` interface, `MessageTemplateProviderImpl`)
- `reminder/` - Reminder domain services (`ReminderService`, `ReminderStateManager`)
- `service/` - Core application services (`MessageService`, `MessageProcessorService`, `EventHandlerService`, `AdminService`, `DynamicPasswordService`, `SystemStatsService`)
- `util/` - Utility classes (`AnalyzerUtil` for AI time parsing, `SignatureValidator` for LINE webhook validation)
- `config/properties/` - Configuration properties classes (`AdminProperties`)
- `constants/` - Global constants management (`Actions` for postback actions)
- `handler/` - Event processing handlers (`PostbackEventHandler`, `MessageEventHandler`)
- `entity/` - JPA entities (`ChatRoom`, `ChatMessage`, `Reminder`, `ReminderState`)
- `repository/` - Data access repositories (`ChatRoomRepository`, `ChatMessageRepository`, `ReminderRepository`, `ReminderStateRepository`)

**Service Layer Architecture**:

- All major services follow interface-implementation pattern with `Impl` suffix
- `MessageProcessorService` orchestrates message processing (admin auth → predefined commands → AI fallback)
- `AdminService` handles two-step authentication flow with state tracking
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
- `AdminProperties` - admin authentication configuration with password seed

### Message Types Supported

- Text messages (with AI responses)
- Images, stickers, videos, audio files
- File uploads, location sharing
- All responses handled by `MessageTemplateProvider` implementations

### Security & Validation

- `SignatureValidator` - validates LINE webhook signatures
- Request signature verification (can be disabled for development)

### AI Integration

- **Groq API Integration**: Primary AI service for chat responses
- **Model Configuration**: Configurable model via `groq.model` property (default: llama-3.1-8b-instant)
- **Character Design**: Natural conversational friend character in Traditional Chinese
- **Response Style**: Knowledge-rich friend with casual, direct communication style
- **Timeout Handling**: 15-second timeout with graceful fallback to default responses
- **Message Storage**: All user messages and AI responses stored in `chat_messages` table with analytics
- **Async Processing**: AI requests processed asynchronously via `CompletableFuture` to avoid blocking LINE webhook responses
- **Conversation History**: Configurable history limit via `ai.conversation.history-limit` (default: 15 messages)
- **Soft Delete**: AI reply messages support soft delete functionality for conversation management
- **Natural Language Processing**: `AnalyzerUtil` provides AI-powered semantic analysis for natural language time expressions
  - **Performance Optimization**: Standard format detection (`yyyy-MM-dd HH:mm`) bypasses AI parsing to reduce API calls
  - **Format Validation**: Regex-based pre-validation before AI semantic analysis
  - **User Feedback**: Real-time parsing results displayed to users for transparency
  - **Timezone Support**: Defaults to Asia/Taipei timezone for consistent time handling

### Admin Authentication

- **Two-Step Authentication**: `/auth` command followed by password input
- **Dynamic Password**: Date-based password generation (YYYYMMDD + seed)
- **Room-Based Permissions**: Each chat room can be independently authenticated as admin
- **Authentication State**: Uses `auth_pending` flag to track authentication flow
- **Password Configuration**: Configurable via `admin.password-seed` (default: "1103")
- **Simple Flow**: User types `/auth` → "請輸入密碼" → User enters password → Authentication complete
- **Service Architecture**:
    - `AdminService` handles authentication commands and state management
    - `DynamicPasswordService` generates current valid password
    - `ChatRoomManager` stores admin status per room
    - `SystemStatsService` provides comprehensive system analytics for admins
- **Database Fields**: `is_admin` and `auth_pending` in `chat_rooms` table
- **Admin Commands**: Authentication flow and system stats reporting with detailed metrics

### Reminder System

- **Smart Reminders**: Scheduled reminder system with repeat options (ONCE, DAILY, WEEKLY)
- **Database Design**: Four-table architecture for reliability and multi-server support:
    - `reminders` - reminder configuration and scheduling  
    - `reminder_logs` - delivery tracking and error logging
    - `reminder_locks` - distributed lock mechanism for preventing duplicate sends
    - `reminder_states` - stateful reminder creation flow (V8 migration) for multi-instance environments
- **Multi-Instance Support**: State management moved from memory to database to support horizontal scaling
- **Status Management**: ACTIVE, PAUSED, COMPLETED states for flexible reminder control
- **Room-Scoped**: Each reminder belongs to a specific chat room with creator tracking
- **Interactive Creation Flow**: Three-step process (repeat type → time input → content input) with 30-minute state expiration
- **Reliability Features**:
    - Duplicate prevention via unique lock keys
    - Comprehensive error logging and retry mechanisms
    - Delivery status tracking for monitoring
    - Automatic cleanup of expired creation states
- **UI Integration**: Accessible through LINE Bot menu system with postback actions
- **AI-Powered Time Parsing**: Natural language time parsing via `AnalyzerUtil`
    - Supports expressions like "明天下午3點", "30分鐘後", "2025-09-06 17:00"  
    - Uses Groq AI service for semantic analysis and time resolution
    - Strict format validation (YYYY-MM-DD HH:MM) with timezone handling (Asia/Taipei)
    - **User Feedback System**: Real-time display of AI parsing process to users
        - Shows original input, AI interpretation, and parsing results
        - Provides detailed error messages with examples on failure
        - Confirms successful parsing with formatted time display
- **Service Architecture**:
    - `ReminderService` handles reminder CRUD operations
    - `ReminderStateManager` manages database-backed creation flow state
    - `AnalyzerUtil` provides AI-driven time parsing with user feedback integration
    - Template-based user messaging via `MessageTemplateProvider`

## Development Guidelines

### Code Structure

- **Domain-based packages**: `ai/`, `chatroom/`, `template/`, `reminder/` for domain logic; `service/`, `handler/`, `controller/` for technical concerns
- **Interface-implementation pattern**: All major services have interfaces with `Impl` suffix implementations
- **Entity Lifecycle Management**: JPA entities use `@PrePersist` and `@PreUpdate` for automatic timestamp handling, but explicit field setting is preferred for critical fields like `expiresAt`
- **Constants Management**: Use `Actions` class for postback constants, `UIConstants` for UI styling
- **Message Template Pattern**: All user-facing messages should be defined in `MessageTemplateProvider` interface and implemented in `MessageTemplateProviderImpl` for consistency
  - **Professional UI System**: All templates use the unified `createCard()` method with consistent header-description-button structure
  - **Method Overloading**: Support parameterized templates (e.g., `reminderInputError(String originalInput, String aiAnalysis)`) for enhanced context-aware messaging
  - **Design Consistency**: Follow white/light gray backgrounds with blue-green accents for professional appearance
- **Admin Services**: `AdminService` handles authentication flow, `DynamicPasswordService` generates time-based passwords
- **Reminder Services**: `ReminderService` handles CRUD operations, `ReminderStateManager` manages multi-step creation flow with database persistence
- **Utility Classes**: 
  - `AnalyzerUtil` provides AI-powered time parsing with natural language support and user feedback, integrated as Spring `@Component` with static methods
  - **Enhanced Error Reporting**: `TimeParseResult` class encapsulates parsing results with original input, AI analysis, and success status for detailed user feedback
  - **Dynamic AI Prompts**: Time parsing uses real-time context including current date/time and calculated relative dates for improved accuracy
  - **Performance Constants**: Extract commonly used `DateTimeFormatter` patterns as static final constants (e.g., `TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")`) to improve performance and maintainability
- Use Lombok for boilerplate reduction (`@RequiredArgsConstructor`, etc.)
- Event handlers should be lightweight and delegate to services
- All external API calls should have timeout and error handling
- **Async Pattern**: Use `CompletableFuture.runAsync()` for non-blocking operations, especially AI processing
- **Transaction Management**: Use `@Transactional` for database operations, avoid in async contexts
- **Simplicity over complexity**: Prefer direct switch statements over complex abstractions
- **Interface segregation**: Keep interfaces focused on specific domain responsibilities
- **Multi-Instance Architecture**: State should be stored in database, not memory, to support horizontal scaling

### Build System

- **Gradle Kotlin DSL**: Uses `build.gradle.kts` with Kotlin syntax
- **Versioning**: Git tag-based versioning (reads from `git describe --tags --abbrev=0`)
- **Java Toolchain**: Configured for Java 17 (line 22-25 in build.gradle.kts)
- **Spring Boot Plugin**: Version 3.4.3 with dependency management
- **JAR Configuration**: Standard jar disabled, bootJar enabled for executable deployment

### Constants and UI Management

- **Actions Constants**: All postback actions defined in `constants/Actions.java`
  ```java
  import static com.acenexus.tata.nexusbot.constants.Actions.*;
  // Use TOGGLE_AI, ENABLE_AI, DISABLE_AI, etc.
  ```
- **UI Constants**: Professional color system and 8px grid spacing in `template/UIConstants.java`
  ```java
  import static com.acenexus.tata.nexusbot.template.UIConstants.*;
  // Use Colors.PRIMARY (#00A8CC), Colors.GRAY_500, Sizes.SPACING_MD (16px)
  // Professional gray scale: GRAY_50, GRAY_100, GRAY_500, GRAY_900
  // 8px grid system: SPACING_SM (8px), SPACING_MD (16px), SPACING_LG (24px)
  ```
- **Professional Color Palette**: Modern blue-green primary with comprehensive gray scale system
- **8px Grid System**: Consistent spacing using 8px baseline grid for professional alignment
- **Design Consistency**: White backgrounds with subtle professional accents

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
- **ReplyToken Limitation**: Each `replyToken` can only be used once - combine messages or use single response to avoid API errors
- **Message Template Overloading**: Support for parameterized templates (e.g., `reminderInputMenu(String step, String reminderTime)`) for enhanced user experience

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

- `ChatRoom` (V1 migration) - Per-room AI settings with lazy record creation
    - Tracks AI enabled/disabled state for each LINE user/group
    - Supports both individual and group conversations
    - V5: Added `is_admin` field for room-based admin authentication
    - V6: Added `auth_pending` field for two-step authentication flow
- `ChatMessage` (V2 migration) - Multi-turn conversation tracking and AI analytics
    - Records all user and AI messages with timestamps
    - Includes AI cost tracking (tokens_used, processing_time_ms, ai_model)
    - Contains room_type redundancy for query performance optimization
    - V4: Added soft delete support for conversation management

**Data Access**:

- JPA/Hibernate with `@Entity` annotations
- Repository pattern with `ChatRoomRepository` and `ChatMessageRepository`
- Database migration managed by Flyway
- Schema validation via `ddl-auto: validate` (no auto-generation in any environment)

**Migration Scripts**:

- Located in `src/main/resources/db/migration`
- File naming: `V{version}__{description}.sql` (e.g., `V2__Create_chat_messages_table.sql`)
- Cross-database compatibility (H2 local, MySQL dev/prod)
- **No Foreign Key Constraints**: Uses application-layer consistency control for better performance
- **Design Rationale**: Detailed comments in migration files explain architectural decisions
- **Current Migrations**: V1 (chat rooms), V2 (chat messages), V3 (AI model tracking), V4 (soft delete support), V5 (admin authentication), V6 (auth pending state), V7 (reminder system), V8 (reminder states for multi-instance support)

### UI Design System Architecture

**Professional Design System**:

- **UIConstants**: Centralized professional design constants with modern color palette and 8px grid system
- **Actions**: Postback action constants for button interactions
- **Template Pattern**: `MessageTemplateProvider` creates consistent professional Flex Message layouts
- **Modernized Approach**: Clean white/light gray backgrounds with professional blue-green accents

**Professional Color System**:

```java
// Primary Colors - Modern Blue-Green Palette
Colors.PRIMARY = "#00A8CC"         // Professional blue-green
Colors.PRIMARY_LIGHT = "#E3F8FF"   // Light blue-green background
Colors.PRIMARY_DARK = "#006B7D"    // Deep blue-green

// Functional Colors
Colors.SUCCESS = "#10B981"         // Modern green
Colors.ERROR = "#EF4444"           // Error red
Colors.WARNING = "#F59E0B"         // Warning orange
Colors.INFO = "#0EA5E9"            // Information blue

// Professional Gray Scale System
Colors.GRAY_50 = "#F9FAFB"         // Lightest gray
Colors.GRAY_100 = "#F3F4F6"        // Light background
Colors.GRAY_500 = "#6B7280"        // Secondary text
Colors.GRAY_900 = "#111827"        // Primary text

// Background System
Colors.BACKGROUND = "#FFFFFF"       // Pure white
Colors.CARD_BACKGROUND = "#FFFFFF"  // Card background
Colors.SECTION_BACKGROUND = "#F9FAFB" // Section background
```

**8px Grid System**:

```java
// Spacing System (8px baseline grid)
Sizes.SPACING_XS = "4px"    // 4px
Sizes.SPACING_SM = "8px"    // 8px
Sizes.SPACING_MD = "16px"   // 16px (2x grid)
Sizes.SPACING_LG = "24px"   // 24px (3x grid)
Sizes.SPACING_XL = "32px"   // 32px (4x grid)

// Border Radius
Sizes.RADIUS_SM = "4px"     // Small radius
Sizes.RADIUS_MD = "8px"     // Standard radius
Sizes.RADIUS_LG = "12px"    // Large radius
```

**Professional Template System**:

```java
// Core template methods in MessageTemplateProviderImpl
createCard(title, description, buttons)           // Unified card template
createNotification(title, message, statusColor)   // Status notification with indicator
createHeaderSection(title, description)           // Standardized header
createButtonSection(buttons)                       // Consistent button layout
createProfessionalCard(altText, components)        // Professional card container

// Button creation methods
createPrimaryButton(label, action)    // Primary action (blue-green)
createButton(label, action)           // Secondary action (gray)
createSuccessButton(label, action)    // Success action (green)
```

**Design Principles**:

- **Consistency**: All templates use unified `createCard()` method
- **Professional**: Modern SaaS product design language
- **Clean**: White backgrounds with subtle blue-green accents
- **Readable**: Proper text hierarchy and color contrast
- **User Experience**: Clear operation guidance and status feedback

## Common Issues and Solutions

### Build Issues

- **Java Version Mismatch**: Ensure Gradle uses Java 17+, not Java 8
- **Compilation Errors**: After major changes, run `./gradlew clean build` to clear cache
- **Dependency Conflicts**: Check `build.gradle.kts` for version alignment

### Database Issues

- **Migration Failures**: Check Flyway baseline settings in configuration
- **H2 Console Access**: Ensure correct JDBC URL format in local development
- **Entity Mapping Errors**: Verify JPA annotations and database column names match

### AI Integration Issues

- **Groq API Failures**: Check API key configuration and network connectivity
- **Timeout Issues**: AI processing has 15-second timeout, check response patterns
- **Empty Responses**: Fallback mechanism provides default responses when AI fails
- **Performance Optimization**: Use direct parsing for standard time formats before falling back to AI semantic analysis

### LINE Bot Integration

- **Webhook Validation**: Signature validation can be disabled for development
- **Message Type Handling**: All message types have specific handlers in `MessageEventHandler`
- **Async Response Issues**: Use `CompletableFuture` for non-blocking AI processing
- **ReplyToken Usage**: Avoid using the same `replyToken` multiple times - combine responses or use single message
- **Switch Case Limitations**: For parameterized postback actions like `"action=delete_reminder&id=123"`, use `startsWith()` in default case rather than exact matching in switch statements

### Reminder System Issues

- **Time Parsing Performance**: Standard format (`yyyy-MM-dd HH:mm`) is parsed directly for performance; natural language expressions use AI semantic analysis
- **Enhanced Error Feedback**: `AnalyzerUtil.TimeParseResult` class provides detailed parsing feedback including original input and AI analysis results for better user experience
- **AI Prompt Optimization**: Time parsing prompts include dynamic context (current time, day of week) and structured examples for improved accuracy
- **State Management**: Multi-step creation flow uses database-backed state for multi-instance support
- **User Experience**: Display parsed time results and detailed error analysis to users for confirmation and transparency

### Performance Considerations

- **Database Queries**: `ChatMessageRepository` includes optimized queries for analytics
- **Memory Usage**: H2 in-memory database for local development
- **Async Processing**: AI requests don't block LINE webhook responses
- **Constant Optimization**: Use static final constants for frequently used formatters and patterns