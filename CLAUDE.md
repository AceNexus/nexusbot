# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

NexusBot is a LINE Bot application built with Spring Boot 3.4.3 and Java 17/21. It integrates with the LINE Messaging API to handle various message types and provides AI-powered responses through the Groq API.

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

**Service Layer**:
- `MessageService` - handles LINE API communication
- `GroqService` - AI chat integration (llama-3.1-8b-instant model)
- `MessageProcessorService` - orchestrates message processing logic
- `FlexMenuService` - creates text-based menu displays
- `InteractiveMenuService` - creates interactive menus with quick reply options

### Configuration Management

**Profile-based Configuration**:
- `bootstrap.yml` - base configuration
- `bootstrap-local.yml` - local development (H2, no Eureka)
- `bootstrap-dev.yml` - development environment
- `bootstrap-prod.yml` - production environment

**Key Configuration Classes**:
- `LineBotConfig` - LINE Bot SDK configuration
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
- Follow existing package structure: `config`, `controller`, `service`, `handler`, `constants`
- Use Lombok for boilerplate reduction (`@RequiredArgsConstructor`, etc.)
- Event handlers should be lightweight and delegate to services
- All external API calls should have timeout and error handling

### Configuration
- Add new configuration properties to appropriate `Properties` classes
- Validate critical configuration in `ConfigValidator`
- Use profile-specific files for environment differences

### Testing
- Tests located in `src/test/java`
- Use `application-test.yml` for test configuration
- JUnit 5 with Spring Boot Test support

### Deployment
- Automated deployment via GitHub Actions on version tags (`v*.*.*`)
- Builds with Java 21, deploys to EC2 via Docker
- JAR versioning based on Git tags
- Uses `./gradlew bootJar` for executable JAR creation

### Interactive Features
- LINE Bot SDK 9.8.0 with interactive message templates
- Quick Reply buttons for rapid user interaction
- Buttons Template, Carousel Template, and Confirm Template support
- Comprehensive interactive button guide available in `INTERACTIVE_BUTTONS_GUIDE.md`
- PostbackEventHandler processes button interactions with structured data