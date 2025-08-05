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

**Service Layer** (Simplified Architecture):
- `MessageService` - handles LINE API communication (SDK 6.0.0)
- `GroqService` - AI chat integration (llama-3.1-8b-instant model)
- `MessageProcessorService` - orchestrates message processing with two-tier approach (predefined commands → AI fallback)
- `FlexMenuService` - creates Flex Message interactive menus using factory pattern
- `EventHandlerService` - routes events to specific handlers based on event type

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
- All responses defined in `BotMessages` constants class

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
- Follow existing package structure: `config`, `controller`, `service`, `handler`, `constants`, `enums`
- Use Lombok for boilerplate reduction (`@RequiredArgsConstructor`, etc.)
- Event handlers should be lightweight and delegate to services
- All external API calls should have timeout and error handling
- **Simplicity over complexity**: Prefer direct switch statements over complex abstractions
- **Single responsibility**: Each service should have one clear purpose

### SOLID Principles

**S - Single Responsibility Principle (SRP)**
- Each class should have only one reason to change
- Examples: `MessageService` only handles LINE API communication, `GroqService` only handles AI integration
- Event handlers focus solely on routing, business logic stays in services

**O - Open/Closed Principle (OCP)**
- Classes should be open for extension, closed for modification
- Use interfaces for external integrations (e.g., `GroqService` can be extended for different AI providers)
- Factory pattern in `FlexMenuService` allows adding new menu types without changing existing code

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

### Simplified Menu Architecture

**Flex Message System** - Single service approach using design patterns:
- **Factory Pattern**: `FlexMenuService.createMenu(MenuType)` creates different menu types
- **Direct Handler Processing**: `PostbackEventHandler` directly processes button clicks
- **Material Design Theme**: Consistent color scheme across all menus
- **Menu Types**: Main menu, AI settings, medication management, help menu
- **Postback Actions**: Structured data format (`action=toggle_ai`, `action=back_to_menu`)

**Event Processing Flow** (Simplified):
```
PostbackEventHandler
├── Parse action from postback data
├── Process action directly OR
└── Return appropriate Flex menu via FlexMenuService
```

**Key Design Decisions**:
- Direct switch statement routing in handlers for simplicity
- Single responsibility services with clear boundaries
- No state management - stateless operation throughout
- Simplified architecture without complex abstractions