# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

NexusBot is a LINE Bot application built with Spring Boot 3.4.3 and Java 17. It integrates with LINE Messaging API (SDK 6.0.0) and provides AI-powered chat responses through Groq API. The architecture follows Domain-Driven Design principles with interface-implementation pattern throughout.

### Core Features (Main Menu)

1. **AI 智能對話** - AI chat with model selection and conversation history management
2. **提醒管理** - Smart reminder system with notification channels (LINE/Email/Both)
3. **Email 通知** - Email notification configuration and management
4. **找附近廁所** - Location-based toilet search service
5. **說明與支援** - Help and support documentation

### Backend Features (No Menu Entry)

6. **聊天室管理** - Chat room configuration and settings management

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
- **Java Environment**: Requires Java 17+ (configured for Java 17)
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

### Event Processing Flow

1. `LineBotController` receives LINE webhook requests (port 5001)
2. `EventHandlerService` routes events to specific handlers
3. Event-specific handlers process different event types:
   - `MessageEventHandler` - handles all message types (text, image, sticker, video, audio, file, location)
   - `PostbackEventHandler` - **委派給 `PostbackEventDispatcher`** (35 行簡潔設計)
   - `FollowEventHandler` - handles follow/unfollow events
   - `GroupEventHandler` - handles group join/leave events

### Postback Event Processing (Strategy + Chain of Responsibility Pattern)

**Architecture**: PostbackEventHandler (35 行) → PostbackEventDispatcher → Specific Handlers

**PostbackEventDispatcher** routes postback events to specific handlers by priority:

1. **@Order(1) ReminderPostbackHandler** - Reminder management (highest priority, 168 lines)
   - Actions: REMINDER_MENU, ADD_REMINDER, LIST_REMINDERS, TODAY_REMINDERS, DELETE_REMINDER, REMINDER_COMPLETED, CANCEL_REMINDER_INPUT
   - Repeat types: REPEAT_ONCE, REPEAT_DAILY, REPEAT_WEEKLY
   - Notification channels: CHANNEL_LINE, CHANNEL_EMAIL, CHANNEL_BOTH
   - Dependencies: `ReminderFacade`, `ReminderStateManager`, `MessageTemplateProvider`

2. **@Order(2) AIPostbackHandler** - AI chat management (138 lines)
   - Actions: TOGGLE_AI, ENABLE_AI, DISABLE_AI, SELECT_MODEL, CLEAR_HISTORY, CONFIRM_CLEAR_HISTORY
   - Models: MODEL_LLAMA_3_1_8B, MODEL_LLAMA_3_3_70B, MODEL_LLAMA3_70B, MODEL_GEMMA2_9B, MODEL_DEEPSEEK_R1, MODEL_QWEN3_32B
   - Dependencies: `ChatRoomManager`, `MessageTemplateProvider`

3. **@Order(3) EmailPostbackHandler** - Email notification management (105 lines)
   - Actions: EMAIL_MENU, ADD_EMAIL, DELETE_EMAIL, TOGGLE_EMAIL_STATUS, CANCEL_EMAIL_INPUT
   - Dependencies: `EmailFacade`

4. **@Order(4) LocationPostbackHandler** - Location services (59 lines)
   - Actions: FIND_TOILETS
   - Dependencies: `ChatRoomManager`, `MessageTemplateProvider`

5. **@Order(10) NavigationPostbackHandler** - Navigation menu (58 lines, lowest priority)
   - Actions: MAIN_MENU, HELP_MENU, ABOUT
   - Dependencies: `MessageTemplateProvider`

**Design Benefits**:
- Each handler < 170 lines (SRP compliance)
- Dependencies reduced from 9+ to 1-3 per handler
- Easy to add new handlers without modifying existing code (OCP)
- Priority-based routing ensures correct handler selection

### Facade Layer (Business Logic Coordination)

**Introduced in Week 2** to encapsulate complex multi-service coordination:

1. **ReminderFacade** (`facade/ReminderFacade.java`, `ReminderFacadeImpl.java` - 228 lines)
   - Coordinates: ReminderService, ReminderStateManager, ReminderLogService, ReminderLogRepository, MessageTemplateProvider
   - Methods: showMenu(), startCreation(), listActive(), showTodayLogs(), deleteReminder(), confirmReminder(), sendNotification(), handleInteraction()
   - Benefits: Reduced ReminderPostbackHandler from 242 → 168 lines (-30%)

2. **EmailFacade** (`facade/EmailFacade.java`, `EmailFacadeImpl.java` - 161 lines)
   - Coordinates: EmailManager, EmailInputStateRepository, MessageTemplateProvider
   - Methods: showMenu(), startAddingEmail(), cancelAddingEmail(), deleteEmail(), toggleEmailStatus(), handleEmailInput(), isWaitingForEmailInput(), clearEmailInputState()
   - Benefits: Reduced EmailPostbackHandler from 170 → 105 lines (-38%), dependencies from 3 → 1

3. **LocationFacade** (`facade/LocationFacade.java`, `LocationFacadeImpl.java` - 97 lines)
   - Coordinates: ChatRoomManager, LocationService, MessageService, MessageTemplateProvider
   - Methods: handleLocationMessage() - async toilet search with fallback handling
   - Benefits: Encapsulated 36 lines of async location processing logic

**Facade Pattern Benefits**:
- Business logic reuse across multiple callers
- Simplified testing (mock 1 facade instead of 5 services)
- Clear separation between presentation layer (Handler) and business logic (Facade)

### Message Processing Pipeline

1. `MessageEventHandler` receives message events and extracts `userId` from LINE payload
2. Routes to `MessageProcessorService` based on message type (189 lines, refactored from 332 lines)
3. For text messages:
   - Stores user message to `chat_messages` table immediately
   - First processes admin authentication commands via `AdminService`
   - Then checks for predefined commands (menu, 選單)
   - Processes reminder interaction via `ReminderFacade.handleInteraction()` (replaces 76 lines of logic)
   - Processes email input via `EmailFacade.handleEmailInput()` (replaces 40 lines of logic)
   - Falls back to AI processing via `AIService`
   - Async processing with fallback responses
   - Stores AI responses with analytics (processing time, model used)
4. For location messages:
   - Delegates to `LocationFacade.handleLocationMessage()` for toilet search handling

### Domain-Driven Package Structure

- `handler/postback/` - **Postback event handlers** (NEW - Week 1)
  - `PostbackHandler` (interface) - Strategy pattern for unified handler behavior
  - `PostbackEventDispatcher` - Chain of Responsibility implementation
  - 5 specific handlers (Navigation, AI, Reminder, Email, Location)
- `facade/` - **Business logic coordination layer** (NEW - Week 2)
  - `ReminderFacade`, `EmailFacade`, `LocationFacade`
  - Encapsulates complex multi-service workflows
- `ai/` - AI service interface (`AIService`) and Groq implementation (`AIServiceImpl`)
- `chatroom/` - Chat room management (`ChatRoomManager` interface, `ChatRoomManagerImpl`)
- `template/` - Message template generation (`MessageTemplateProvider` interface, `MessageTemplateProviderImpl`)
- `reminder/` - Reminder domain services (`ReminderService`, `ReminderStateManager`, `ReminderLogService`, `EmailNotificationService`)
- `email/` - Email management (`EmailManager`, `EmailInputStateRepository`)
- `location/` - Location services (toilet search via `LocationService`)
- `service/` - Core application services (`MessageService`, `MessageProcessorService`, `EventHandlerService`, `AdminService`)
- `util/` - Utility classes (`AnalyzerUtil` for AI time parsing, `SignatureValidator` for LINE webhook validation)
- `config/properties/` - Configuration properties classes
- `constants/` - Global constants management (`Actions` for postback actions)
- `handler/` - Event processing handlers
- `entity/` - JPA entities
- `repository/` - Data access repositories
- `controller/` - HTTP endpoints
- `exception/` - Global exception handling
- `scheduler/` - Scheduled tasks (reminder scheduler)
- `lock/` - Distributed lock mechanism

### Service Layer Architecture

- All major services follow interface-implementation pattern with `Impl` suffix
- `MessageProcessorService` orchestrates message processing (admin auth → predefined commands → AI fallback)
- `AdminService` handles two-step authentication flow with state tracking
- Dependency injection uses interfaces for loose coupling and testability

### Notification Module Architecture (Week 3 - 2025-10-07)

**Design Goal**: Unified notification system with extensible channel support

**Module Structure**:
- `notification/ReminderNotificationService` (interface) - Unified notification coordination
- `notification/ReminderNotificationServiceImpl` (118 lines) - Route notifications to appropriate channels
- `notification/LineNotificationService` (89 lines) - LINE push message delivery with logging
- `notification/EmailNotificationService` (116 lines) - Email delivery with multi-recipient support

**Refactoring Benefits**:
- `ReminderScheduler` dependencies reduced from 9 → 4 (-56%)
- `ReminderScheduler` code reduced from 300 → 197 lines (-34%)
- Notification logic centralized for easy extension (SMS, Push, etc.)
- Clear separation: Scheduler handles "when", NotificationService handles "how"

## Configuration Management

### Profile-based Configuration

- `bootstrap.yml` - base configuration
- `bootstrap-local.yml` - local development (H2, no Eureka)
- `bootstrap-dev.yml` - development environment
- `bootstrap-prod.yml` - production environment

### Key Configuration Classes

- `LineBotConfig` - LINE Bot SDK 6.0.0 configuration with `LineMessagingClient`
- `ConfigValidator` - validates configuration on startup
- Properties classes in `config.properties` package

### Message Types Supported

- Text messages (with AI responses)
- Images, stickers, videos, audio files
- File uploads, location sharing
- All responses handled by `MessageTemplateProvider` implementations

## Key Features

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

### Reminder System

- **Smart Reminders**: Scheduled reminder system with repeat options (ONCE, DAILY, WEEKLY)
- **Database Design**: Four-table architecture for reliability and multi-server support:
  - `reminders` - reminder configuration and scheduling
  - `reminder_logs` - delivery tracking and error logging
  - `reminder_locks` - distributed lock mechanism for preventing duplicate sends
  - `reminder_states` - stateful reminder creation flow for multi-instance environments
- **Multi-Instance Support**: State management moved from memory to database to support horizontal scaling
- **Status Management**: ACTIVE, PAUSED, COMPLETED states for flexible reminder control
- **Room-Scoped**: Each reminder belongs to a specific chat room with creator tracking
- **Interactive Creation Flow**: Multi-step process (repeat type → notification channel → time input → content input) with 30-minute state expiration
- **Notification Channels**: LINE, Email, or BOTH (dual notification)
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
  - `ReminderScheduler` handles scheduled reminder execution
  - `ReminderLogService` tracks delivery history and confirmation status
  - `EmailNotificationService` handles email reminder delivery
  - `AnalyzerUtil` provides AI-driven time parsing with user feedback integration
  - Template-based user messaging via `MessageTemplateProvider`

### Email Notification System

- **Zero-Quota Design**: Email confirmations update database only, no LINE push message to save quota
- **Multi-Email Support**: Users can bind multiple email addresses per chat room
- **Confirmation Flow**: Email contains confirmation link → User clicks → Database updated → Status visible in LINE Bot
- **Email Templates**: Thymeleaf-based HTML templates (simplified design)
  - `reminder-email.html` - Reminder notification email
  - `reminder-confirmation.html` - Confirmation result page
- **Time Format**: Chinese format (yyyy年MM月dd日 HH:mm:ss)
- **Controller**: `ReminderConfirmationController` handles web-based confirmation
- **Service**: `EmailNotificationService` manages SMTP delivery with UUID tokens

### Today's Reminder Logs

- **Purpose**: View reminders sent today (solves "ONCE" reminder visibility issue after completion)
- **Data Source**: Queries `reminder_logs` table instead of `reminders` table
- **Deduplication**: Merges multiple logs (LINE + Email) into single display entry
- **Status Display**: Simplified to "已確認" or "待確認" (removes emoji and notification type)
- **Service**: `ReminderLogService.getTodaysSentReminders(roomId)`
- **DTO**: `TodayReminderLog` record with `isConfirmed` flag
- **UI**: Accessible via "今日提醒記錄" button in reminder menu

### Location Services

- **Toilet Search**: OSM (OpenStreetMap) API integration
- **Location Input State**: Database-backed state management for multi-step location input
- **Search Flow**: User shares location → System searches nearby toilets → Displays results with distance

## Development Guidelines

### Code Structure

- **Domain-based packages**: `ai/`, `chatroom/`, `template/`, `reminder/`, `email/` for domain logic; `service/`, `handler/`, `controller/` for technical concerns
- **Interface-implementation pattern**: All major services have interfaces with `Impl` suffix implementations
- **Entity Lifecycle Management**: JPA entities use `@PrePersist` and `@PreUpdate` for automatic timestamp handling, but explicit field setting is preferred for critical fields like `expiresAt`
- **Constants Management**: Use `Actions` class for postback constants, `UIConstants` for UI styling
- **Message Template Pattern**: All user-facing messages should be defined in `MessageTemplateProvider` interface and implemented in `MessageTemplateProviderImpl` for consistency
  - **Professional UI System**: All templates use the unified `createCard()` method with consistent header-description-button structure
  - **Method Overloading**: Support parameterized templates for enhanced context-aware messaging
  - **Design Consistency**: Follow white/light gray backgrounds with blue-green accents for professional appearance
- **Service Layer**: Use `@Transactional` for database operations, avoid in async contexts
- **Async Pattern**: Use `CompletableFuture.runAsync()` for non-blocking operations, especially AI processing
- **Multi-Instance Architecture**: State should be stored in database, not memory, to support horizontal scaling
- Use Lombok for boilerplate reduction (`@RequiredArgsConstructor`, etc.)
- Event handlers should be lightweight and delegate to services
- All external API calls should have timeout and error handling

### Build System

- **Gradle Kotlin DSL**: Uses `build.gradle.kts` with Kotlin syntax
- **Versioning**: Git tag-based versioning (reads from `git describe --tags --abbrev=0`)
- **Java Toolchain**: Configured for Java 17
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
  ```
- **Professional Color Palette**: Modern blue-green primary with comprehensive gray scale system
- **8px Grid System**: Consistent spacing using 8px baseline grid for professional alignment

### SOLID Principles

**S - Single Responsibility Principle (SRP)**
- Each class should have only one reason to change
- Examples: `MessageService` only handles LINE API communication, `AIServiceImpl` only handles AI integration

**O - Open/Closed Principle (OCP)**
- Classes should be open for extension, closed for modification
- Use interfaces for external integrations (e.g., `AIService` can have different implementations)

**L - Liskov Substitution Principle (LSP)**
- Derived classes must be substitutable for their base classes
- All service implementations should honor their interface contracts

**I - Interface Segregation Principle (ISP)**
- Clients should not depend on interfaces they don't use
- Keep service interfaces focused and specific to their domain

**D - Dependency Inversion Principle (DIP)**
- High-level modules should not depend on low-level modules - both should depend on abstractions
- Use Spring's dependency injection with `@RequiredArgsConstructor`
- Depend on interfaces, not concrete implementations

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
- **Message Template Overloading**: Support for parameterized templates for enhanced user experience

### Testing

- Tests located in `src/test/java`
- Use `application-test.yml` for test configuration
- JUnit 5 with Spring Boot Test support
- **Test Coverage**: 34 tests (100% success rate)
  - Handler Tests: 33 tests covering all PostbackHandlers and Dispatcher
  - Application Context Test: 1 test

**Test Structure**:
- `handler/postback/NavigationPostbackHandlerTest` - 9 tests
- `handler/postback/AIPostbackHandlerTest` - 13 tests
- `handler/postback/LocationPostbackHandlerTest` - 5 tests
- `handler/postback/PostbackEventDispatcherTest` - 6 tests

### Deployment

- Automated deployment via GitHub Actions on version tags (`v*.*.*`)
- Builds with Java 21, deploys to EC2 via Docker
- JAR versioning based on Git tags
- Uses `./gradlew bootJar` for executable JAR creation

## Database Design

### Entity Structure

- **ChatRoom** - Per-room AI settings with lazy record creation
  - Tracks AI enabled/disabled state, admin status, authentication pending state
  - Supports both individual and group conversations
- **ChatMessage** - Multi-turn conversation tracking and AI analytics
  - Records all user and AI messages with timestamps
  - Includes AI cost tracking (tokens_used, processing_time_ms, ai_model)
  - Supports soft delete for conversation management
- **Reminder** - Reminder configuration with scheduling information
  - Contains repeat type, notification channel, status
  - Tracks creation time and creator
- **ReminderLog** - Delivery tracking and confirmation status
  - Records delivery method (LINE/Email), confirmation token, confirmed time
  - Tracks user response status for LINE notifications
- **ReminderState** - Multi-step creation flow state (for multi-instance support)
  - Tracks current step, repeat type, notification channel, time, expiration
- **Email** - User email bindings per room
  - Supports multiple emails per room, active/inactive status
- **EmailInputState** - Temporary state for email input flow

### Data Access

- JPA/Hibernate with `@Entity` annotations
- Repository pattern with Spring Data JPA
- Database migration managed by Flyway
- Schema validation via `ddl-auto: validate` (no auto-generation in any environment)

### Migration Scripts

- Located in `src/main/resources/db/migration`
- File naming: `V{version}__{description}.sql` (e.g., `V2__Create_chat_messages_table.sql`)
- Cross-database compatibility (H2 local, MySQL dev/prod)
- **No Foreign Key Constraints**: Uses application-layer consistency control for better performance
- **Design Rationale**: Detailed comments in migration files explain architectural decisions
- **Current Migrations**: V1-V14 covering chat rooms, messages, reminders, emails, and notification channels

### UI Design System Architecture

**Professional Design System**:
- **UIConstants**: Centralized professional design constants with modern color palette and 8px grid system
- **Actions**: Postback action constants for button interactions
- **Template Pattern**: `MessageTemplateProvider` creates consistent professional Flex Message layouts
- **Modernized Approach**: Clean white/light gray backgrounds with professional blue-green accents

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

- **Database Queries**: Repositories include optimized queries for analytics
- **Memory Usage**: H2 in-memory database for local development
- **Async Processing**: AI requests don't block LINE webhook responses
- **Constant Optimization**: Use static final constants for frequently used formatters and patterns
