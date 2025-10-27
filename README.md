# NexusBot

基於 Spring Boot 的 LINE Bot 應用，整合 AI 對話、智能提醒、Email 通知等功能。

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://adoptium.net/)
[![LINE Bot SDK](https://img.shields.io/badge/LINE%20Bot%20SDK-6.0.0-00C300.svg)](https://github.com/line/line-bot-sdk-java)

## 目錄

- [技術棧](#技術棧)
- [快速開始](#快速開始)
- [架構設計](#架構設計)
- [核心功能](#核心功能)
- [資料庫設計](#資料庫設計)
- [開發指南](#開發指南)
- [測試](#測試)

---

## 技術棧

### 後端框架
- **Spring Boot**: 3.4.3
- **Java**: 17 (Toolchain 配置)
- **LINE Bot SDK**: 6.0.0
- **Spring Cloud Bootstrap**: 2024.0.0

### 資料庫
- **H2**: 本地開發與測試（記憶體資料庫）
- **MySQL**: 8.3.0（開發/生產環境）
- **Flyway**: 資料庫遷移管理
- **JPA/Hibernate**: ORM 框架

### 第三方整合
- **Groq API**: AI 對話服務
- **OpenStreetMap**: 位置服務
- **JavaMail**: Email 通知
- **Thymeleaf**: HTML 模板引擎

### 建置工具
- **Gradle**: 8.x with Kotlin DSL
- **Git Tag Versioning**: 自動版本控制

---

## 快速開始

### 前置需求

- Java 17+
- LINE Developer Account
- Groq API Key

### 安裝步驟

1. **Clone 專案**
```bash
git clone <repository-url>
cd nexusbot
```

2. **配置環境變數**

建立 `src/main/resources/bootstrap-local.yml`:
```yaml
line:
  bot:
    channel-token: YOUR_LINE_CHANNEL_TOKEN
    channel-secret: YOUR_LINE_CHANNEL_SECRET

groq:
  api-key: YOUR_GROQ_API_KEY
```

3. **啟動應用程式**
```bash
# 使用 Gradle Wrapper
./gradlew bootRun

# 或建置 JAR 後執行
./gradlew bootJar
java -jar build/libs/nexusbot-*.jar
```

4. **驗證運作**
- 應用程式: http://localhost:5001
- H2 Console: http://localhost:5001/h2-console
- Swagger UI: http://localhost:5001/swagger-ui.html

### Gradle 常用指令

```bash
# 編譯
./gradlew clean build

# 執行測試
./gradlew test

# 執行應用程式
./gradlew bootRun

# 建立可執行 JAR
./gradlew bootJar
```

---

## 架構設計

### 設計模式

本專案採用 Strategy Pattern + Chain of Responsibility + Facade Pattern：

#### 1. Strategy Pattern（策略模式）

用於 PostbackHandler 和 CommandHandler：

```java
public interface PostbackHandler {
    boolean canHandle(String action);
    Message handle(String action, String roomId, ...);
    int getPriority();
}

public interface CommandHandler {
    boolean canHandle(CommandContext context);
    CommandResult handle(CommandContext context);
    int getPriority();
}
```

#### 2. Chain of Responsibility（職責鏈模式）

Dispatcher 按優先級路由請求：

```java
@Component
public class PostbackEventDispatcher {
    private final List<PostbackHandler> handlers;

    public void dispatch(JsonNode event) {
        // 按優先級排序並找到第一個可處理的 Handler
        handlers.stream()
            .sorted(Comparator.comparingInt(PostbackHandler::getPriority))
            .filter(h -> h.canHandle(action))
            .findFirst()
            .ifPresent(h -> h.handle(...));
    }
}
```

#### 3. Facade Pattern

封裝複雜業務邏輯：

```java
@Service
public class ReminderFacadeImpl implements ReminderFacade {
    private final ReminderService reminderService;
    private final ReminderStateManager stateManager;
    private final ReminderLogService logService;
    // ... 協調多個 Service
}
```

### 請求處理流程

```
LINE Webhook
    ↓
Controller
    ↓
EventHandler (Message/Postback/Follow/Group)
    ↓
Dispatcher (PostbackEventDispatcher / CommandDispatcher)
    ↓
Handler (Strategy Pattern - 按優先級選擇)
    ↓
Facade (協調業務邏輯)
    ↓
Service Layer
    ↓
Repository Layer
    ↓
Database
```

### 目錄結構

```
src/main/java/com/acenexus/tata/nexusbot/
├── ai/                  # AI 對話服務
├── chatroom/            # 聊天室管理
├── command/             # 命令處理
│   ├── handlers/        # 各類命令處理器
│   ├── CommandDispatcher.java
│   └── CommandHandler.java
├── config/              # Spring 配置
├── controller/          # HTTP 端點
├── email/               # Email 管理
├── entity/              # JPA 實體
├── exception/           # 異常處理
├── facade/              # Facade 層
├── handler/             # LINE 事件處理
│   └── postback/        # Postback 處理器
├── location/            # 位置服務
├── notification/        # 通知服務
├── reminder/            # 提醒功能
├── repository/          # 資料存取
├── scheduler/           # 排程任務
├── service/             # 業務邏輯
├── template/            # 訊息模板
└── util/                # 工具類別
```

---

## 核心功能

### 1. AI 對話

- 整合 Groq API，支援多種 AI 模型
- 保留最近 15 則對話歷史
- 非同步處理（CompletableFuture + 15 秒超時）
- 軟刪除設計

### 2. 智能提醒

- 支援單次/每日/每週重複
- 三種通知管道：LINE / Email / 雙通道
- AI 自然語言時間解析
- 分散式鎖防止重複發送
- 確認機制追蹤用戶回應

### 3. Email 通知

- 多 Email 地址綁定
- Thymeleaf HTML 模板
- 啟用/停用控制
- Email 確認連結

### 4. 位置服務

- 整合 OpenStreetMap API
- 搜尋附近設施（如廁所）
- 距離計算

### 5. 管理員功能

- 兩步驟認證（`/auth` + 動態密碼）
- 系統統計資訊

---

## 資料庫設計

### 主要資料表

| 資料表 | 用途 | 關鍵欄位 |
|--------|------|----------|
| `chat_rooms` | 聊天室配置 | room_id (UK), ai_enabled, ai_model, is_admin |
| `chat_messages` | 對話記錄 | room_id, role, content, tokens_used |
| `reminders` | 提醒設定 | room_id, reminder_time, repeat_type, notification_channel |
| `reminder_logs` | 提醒記錄 | reminder_id, status, delivery_method, confirmed_at |
| `reminder_states` | 提醒建立狀態 | room_id (UK), current_step, expires_at |
| `reminder_locks` | 分散式鎖 | lock_key (UK), expires_at |
| `emails` | Email 管理 | room_id, email_address, is_enabled |
| `email_input_states` | Email 輸入狀態 | room_id (UK), expires_at |

### 設計原則

- **無外鍵約束**: 提升寫入效能，一致性由應用層控制
- **索引策略**: 覆蓋常用查詢，避免過多索引
- **跨資料庫相容**: H2（local/test）與 MySQL（dev/prod）
- **Flyway 遷移**: 版本控制的遷移檔案

---

## 開發指南

### 環境配置

**Environment Profiles:**
- `local`: H2 記憶體資料庫，開發設定
- `dev`: MySQL 資料庫，開發設定
- `prod`: MySQL 資料庫，生產設定

**Environment Variables:**
```bash
LINE_CHANNEL_TOKEN=<your_token>
LINE_CHANNEL_SECRET=<your_secret>
GROQ_API_KEY=<your_key>
SPRING_PROFILES_ACTIVE=local  # or dev/prod

# MySQL (dev/prod only)
DB_URL=jdbc:mysql://localhost:3306/nexusbot
DB_USERNAME=root
DB_PASSWORD=<password>
```

### 新增 PostbackHandler

1. 建立 Handler 類別：

```java
@Service
@Order(5)
@RequiredArgsConstructor
public class MyPostbackHandler implements PostbackHandler {
    private final MyFacade myFacade;

    @Override
    public boolean canHandle(String action) {
        return action.startsWith("MY_PREFIX_");
    }

    @Override
    public Message handle(String action, String roomId, String roomType, String replyToken, JsonNode event) {
        return myFacade.doSomething(roomId);
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
```

2. 定義常數（`constants/Actions.java`）：

```java
public static final String MY_ACTION = "action=MY_ACTION";
```

3. Spring 自動註冊，無需修改 Dispatcher

### 新增 CommandHandler

1. 建立 Handler 類別：

```java
@Component
@RequiredArgsConstructor
public class MyCommandHandler implements CommandHandler {
    private final MyService myService;

    @Override
    public boolean canHandle(CommandContext context) {
        // 必須是純函數，無副作用
        return context.getNormalizedText().equals("mycommand");
    }

    @Override
    public CommandResult handle(CommandContext context) {
        // 執行業務邏輯
        Message message = myService.process(context.getRoomId());
        return CommandResult.withMessage(message);
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
```

**注意事項：**
- `canHandle` 必須是純函數（無副作用）
- 使用 `context.getNormalizedText()` 進行文字匹配
- 不要在 `canHandle` 中執行業務邏輯

詳細說明請參考 `CommandHandler.java` 的 JavaDoc。

### 新增資料庫遷移

在 `src/main/resources/db/migration/` 建立新檔案：

```sql
-- V15__Add_new_feature.sql
CREATE TABLE new_table (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ...
);
```

命名規範：`V{number}__{description}.sql`

### 錯誤處理

統一錯誤處理機制：

```java
// 拋出業務異常
throw new BusinessException(ErrorCode.INVALID_INPUT, "錯誤訊息");

// GlobalExceptionHandler 自動處理
// TraceIdFilter 自動添加 TraceId
```

### 訊息模板

訊息模板集中在 `MessageTemplateProvider`：

```java
@Service
public class MessageTemplateProviderImpl implements MessageTemplateProvider {
    @Override
    public Message mainMenu() {
        // Flex Message / Button Template / Text Message
    }
}
```

UI 常數定義在 `UIConstants.java`。

---

## 測試

### 執行測試

```bash
# 執行所有測試
./gradlew test

# 執行特定測試類別
./gradlew test --tests NavigationPostbackHandlerTest

# 測試覆蓋率報告
./gradlew test jacocoTestReport
```

### 測試結構

```
src/test/java/
├── handler/postback/
│   ├── NavigationPostbackHandlerTest
│   ├── AIPostbackHandlerTest
│   ├── LocationPostbackHandlerTest
│   └── PostbackEventDispatcherTest
├── command/
│   ├── CommandDispatcherTest
│   └── MenuCommandHandlerTest
└── NexusBotApplicationTests
```

### 測試覆蓋

- 總測試數：34 tests
- 成功率：100%
- 覆蓋範圍：Handler, Dispatcher, Application Context

---

## API 文檔

### Swagger UI

訪問 http://localhost:5001/swagger-ui.html 查看完整 API 文檔

### 主要端點

| 端點 | 方法 | 說明 |
|------|------|------|
| `/webhook` | POST | LINE Messaging API Webhook |
| `/reminder/confirm/{token}` | GET | Email 提醒確認連結 |
| `/h2-console` | GET | H2 資料庫控制台（僅 local） |
| `/actuator/health` | GET | 健康檢查 |

---

## 部署

### Docker 部署

```bash
# 建立 Image
docker build -t nexusbot:latest .

# 執行容器
docker run -d \
  --name nexusbot \
  -p 5001:5001 \
  -e LINE_CHANNEL_TOKEN=your_token \
  -e LINE_CHANNEL_SECRET=your_secret \
  -e GROQ_API_KEY=your_key \
  -e SPRING_PROFILES_ACTIVE=prod \
  nexusbot:latest
```

### 多實例部署

本應用支援水平擴展：

- **Database-backed State**: 狀態存儲於資料庫
- **Distributed Lock**: 防止重複處理提醒
- **Stateless Design**: 任何實例可處理任何請求

架構：
```
LINE API → Nginx Load Balancer → NexusBot Instances → MySQL
```

---

## 參考文檔

- [CLAUDE.md](CLAUDE.md) - Claude Code 開發指南
- [LINE Messaging API](https://developers.line.biz/en/reference/messaging-api/)
- [Spring Boot Documentation](https://spring.io/guides/gs/spring-boot/)
- [Flyway Documentation](https://documentation.red-gate.com/flyway/)

---