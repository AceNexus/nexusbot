# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run locally (H2 file-based, port 5001)
./gradlew bootRun

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.acenexus.tata.nexusbot.util.TimezoneValidatorTest"

# Clean build
./gradlew clean build
```

Local dev endpoints:
- App: http://localhost:5001
- H2 Console: http://localhost:5001/h2-console (JDBC: `jdbc:h2:file:./data/testdb`, user: `sa`, no password)
- Swagger UI: http://localhost:5001/swagger-ui.html

## Microservices Ecosystem

This repo (`nexusbot`) is one of four services in the **AceNexus** platform. Startup order matters:

| # | Service | Port | Role |
|---|---------|------|------|
| 1 | `configservice` | 8888 | Spring Cloud Config Server — serves YAML configs from its own `configs/` git dir; RabbitMQ for live refresh |
| 2 | `eurekaservice` | 8761 | Spring Cloud Netflix Eureka — service registry |
| 3 | `gatewayservice` | 8080 | Spring Cloud Gateway — unified entry point; JWT auth + request logging |
| 4 | `nexusbot` | 5001 | This service — LINE Bot application |

In **prod**, nexusbot fetches its config from configservice and registers with Eureka. In **local**, Config Server and Eureka are both disabled (`application-local.yml`).

The gateway routes `/api/linebot/**` to nexusbot. The webhook path (`/api/linebot/webhook`) bypasses JWT validation.

Dynamic config refresh: `POST /actuator/busrefresh` on configservice → RabbitMQ → all services re-fetch config and rebind `@RefreshScope` beans without restart.

### 本地開發（Kubernetes）

核心微服務跑在 **Docker Desktop Kubernetes**（namespace: `acenexus`），設定與腳本位於 `deploy/` repo。
啟動順序與詳細操作指令請參考 `deploy/README.md`。

**流量路徑：**

```
LINE → ngrok (HTTPS) → gatewayservice :8080 → nexusbot :5001
```

**ngrok 自動化**：`deploy/ngrok-tunnel.bat` 啟動後自動取得 public URL，
更新 K8s `NEXUSBOT_BASE_URL` env var，並呼叫 LINE API 更新 webhook。

Commit message format: `[type] 中文描述` — types: `feat`, `fix`, `refactor`, `docs`, `test`, `config`

---

## Architecture

Spring Boot 3.4.3 / Java 17 LINE Bot with event-driven architecture and Chain of Responsibility pattern.

### Request Flow

```
LINE Webhook → LineBotController
  → LineBotEventCoordinator
    → EventConverterRegistry  (raw JsonNode → LineBotEvent)
    → LineBotEventDispatcher  (Chain of Responsibility, sorted by priority)
      → first matching LineBotEventHandler.canHandle() wins
        → handler.handle() → Message → MessageService.sendMessage()
```

### Event Handler Priority Conventions

| Range | Type |
|-------|------|
| 1–10 | State-based handlers (user is mid-flow: reminder, email, timezone input) |
| 11–30 | Postback handlers |
| 31–50 | Explicit command handlers (menu, admin) |
| 51–99 | Low-priority handlers |
| 100 | `AIMessageEventHandler` — fallback catch-all |

**Adding a new handler**: implement `LineBotEventHandler` (`canHandle`, `handle`, `getPriority`), annotate with `@Component`. Spring auto-discovers and registers it. `canHandle()` must be a pure function with no side effects.

### Key Packages

| Package | Role |
|---------|------|
| `event/handler/` | Event handlers (Chain of Responsibility) |
| `event/converter/` | EventConverterRegistry — maps raw LINE JSON to `LineBotEvent` |
| `facade/` | Facade layer called by handlers — see Facades section below |
| `admin/` | Admin auth flow (`/auth` command), `DynamicPasswordService` (seed-based password), `SystemStatsService` (`/stats`); brute-force lockout is in-memory (resets on restart), configurable via `AdminProperties` |
| `chatroom/` | Per-room state: AI on/off, model selection, admin status, toilet-search wait, timezone |
| `template/` | Flex Message builders per feature: `ReminderTemplateBuilder`, `LocationTemplateBuilder`, `NavigationTemplateBuilder`, etc. |
| `notification/` | `LineNotificationService`, `EmailNotificationService`, `ReminderNotificationService` |
| `scheduler/` | `ReminderScheduler` (1 s), `StateCleanupScheduler` (10 min), `LockCleanupScheduler` (10 min) |
| `lock/` | DB-based distributed lock via `reminder_locks` table (UNIQUE KEY prevents duplicates) |
| `controller/` | Webhook (`LineBotController`), REST (reminder confirmation) |
| `config/` | Spring config classes |
| `fliter/` | `TraceIdFilter` (MDC TraceId per request) + `MdcTaskDecorator` (propagates to async) |

### Configuration & Profiles

Active profile is controlled by `SPRING_PROFILES_ACTIVE` env var (default: `local-eureka`, port 5002).

- `application-local.yml` — H2 file DB, disables Eureka/Config Server, port 5001
- `application-local-eureka.yml` — same as local but enables Eureka, port 5002
- `application-prod.yml` — MySQL, full microservices integration

Required env vars (no real defaults in local):
- `LINE_CHANNEL_TOKEN`, `LINE_CHANNEL_SECRET`
- `GROQ_API_KEY`
- `GEMINI_PROXY_URL`, `GEMINI_PROXY_API_KEY` (Gemini via OpenAI-compatible proxy)
- `EMAIL_USERNAME`, `EMAIL_PASSWORD`, `EMAIL_FROM`
- `ADMIN_PASSWORD_SEED` (seed for dynamic admin password; use `{cipher}` encryption in prod via Config Server)

### Database

- **Local**: H2 file (`./data/testdb`)
- **Prod**: MySQL 8.x
- **Migrations**: Flyway (`src/main/resources/db/migration/V*.sql`), `ddl-auto: validate`
- **No foreign keys** — referential integrity enforced at application layer
- Reminder times stored as epoch millis (`reminder_time_instant BIGINT`) for timezone safety

### AI Provider Architecture

Two providers are registered at startup via `AiProvider` enum: `GROQ` and `GEMINI_PROXY` (OpenAI-compatible proxy). `AIServiceImpl` builds one `WebClient` per provider and routes calls by reading `AiModel.provider`.

**Adding a new AI model**: add one constant to `AiModel` enum with `id`, `displayName`, `temperature`, `maxTokens`, and the target `AiProvider`. Add a matching `Actions` constant and a Flex Message button in `AISettingsTemplateBuilder`. No other changes needed.

### Distributed Locking

`reminder_locks` table with a UNIQUE KEY on `lock_key`. The `DistributedLock` bean uses `DataIntegrityViolationException` on duplicate insert as the lock mechanism (no Redis dependency).

### Facades

| Facade | Role |
|--------|------|
| `AIFacade` | Chat history and model selection; delegates to `AIService` which routes to Groq or Gemini Proxy based on the `AiModel` enum |
| `EmailFacade` | Email input flow state and validation |
| `LocationFacade` | OSM Overpass API integration for facility search |
| `ReminderFacade` | Reminder CRUD, repeat-type selection, timezone changes, stateful flow via `ReminderStateManager` |
| `TimezoneFacade` | Timezone input flow, IANA ID resolution, Chinese/English alias support (e.g. `台北` → `Asia/Taipei`) |

### Stateful Multi-Step Flows

`ReminderStateManager` tracks multi-step interactions via a `ReminderState` entity. Step order: `WAITING_FOR_REPEAT_TYPE` → `WAITING_FOR_NOTIFICATION_CHANNEL` → `WAITING_FOR_TIME` → `WAITING_FOR_CONTENT`. States expire after 30 minutes. State-based handlers (priority 1–10) check state first via facade before `canHandle()` returns true.

### Scheduler Architecture

`ReminderScheduler` (every 1 s) finds due reminders and delegates each to `ReminderProcessor` (@Transactional). `ReminderProcessor` acquires a distributed lock, sends the notification (async, optionally AI-enhanced), then delegates repeat logic to `ReminderRepeatHandler`: `ONCE` → `COMPLETED`; `DAILY`/`WEEKLY` → reschedule. If the app was down and missed repeating reminders, a self-healing loop advances the next trigger time until it's in the future.

`StateCleanupScheduler` (every 10 min) bulk-deletes expired rows from all three state tables (`reminder_states`, `email_input_states`, `timezone_input_states`). This keeps `canHandle()` pure — no DB writes happen inside state-based handlers.

### Logging

Every request gets an 8-char TraceId via `TraceIdFilter` → MDC. `MdcTaskDecorator` propagates MDC context to `CompletableFuture` async tasks. Log pattern includes `[traceId]` field.

### CI/CD

GitHub Actions (`.github/workflows/ci.yml`) triggers on push / PR to `main`:
- **test job**（PR + main）：`./gradlew build`（compile + test）
- **release job**（main only）：`./gradlew bootJar` → `docker build` → Trivy scan → push `ghcr.io/acenexus/nexusbot:<sha>` to GHCR → update `AceNexus/deploy` k8s/nexusbot/deployment.yaml image tag
- ArgoCD 偵測 deploy repo 變動後自動 `kubectl apply`，滾動更新至新版本
