# NexusBot - 智能 LINE Bot 應用

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://adoptium.net/)
[![LINE Bot SDK](https://img.shields.io/badge/LINE%20Bot%20SDK-6.0.0-00C300.svg)](https://github.com/line/line-bot-sdk-java)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0-85EA2D.svg)](https://swagger.io/specification/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> 基於 Spring Boot 的專業 LINE Bot 應用，整合 AI 對話、智能提醒、Email 通知等功能。採用 **Domain-Driven Design** 架構與多種設計模式，展現企業級系統設計能力。

---

## 📚 目錄

- [核心功能](#-核心功能)
- [架構設計亮點](#-架構設計亮點)
- [技術棧](#-技術棧)
- [快速開始](#-快速開始)
- [API 文檔](#-api-文檔)
- [架構圖與流程圖](#-架構圖與流程圖)
- [資料庫設計](#-資料庫設計)
- [測試與品質](#-測試與品質)
- [部署指南](#-部署指南)
- [專案結構](#-專案結構)
- [開發指南](#-開發指南)
- [貢獻與授權](#-貢獻與授權)

---

## 🚀 核心功能

### 1. AI 智能對話
- **多模型支援**: 整合 Groq API，支援 6 種 AI 模型切換 (Llama, Gemma, DeepSeek, Qwen)
- **多輪對話**: 保留最近 15 則對話歷史，實現上下文理解
- **非同步處理**: 使用 CompletableFuture 避免阻塞，15 秒超時保護
- **對話管理**: 軟刪除設計，支援清除歷史記錄

### 2. 智能提醒系統
- **多種重複類型**: 單次 / 每日 / 每週提醒
- **三種通知管道**: LINE / Email / 雙通道同時發送
- **AI 時間解析**: 支援自然語言輸入（如「明天下午3點」、「30分鐘後」）
- **多實例支援**: Database-backed State + Distributed Lock 防重複發送
- **確認機制**: LINE 按鈕確認 + Email 連結確認，追蹤用戶回應

### 3. Email 通知管理
- **多 Email 綁定**: 一個聊天室可綁定多個 Email 地址
- **零 Quota 設計**: Email 確認只更新資料庫，不消耗 LINE Push Message 配額
- **HTML 模板**: Thymeleaf 渲染專業 Email 樣式
- **啟用/停用管理**: 靈活控制每個 Email 的通知狀態

### 4. 位置服務
- **找附近廁所**: 整合 OpenStreetMap API，搜尋半徑 1000 公尺
- **距離計算**: 顯示與用戶的實際距離

### 5. 管理員功能
- **兩步驟認證**: `/auth` 命令 + 動態密碼（日期基礎）
- **系統統計**: 查看聊天室數量、訊息統計、提醒統計等

---

## 🏗️ 架構設計亮點

### 設計模式應用

本專案實踐了 **4 種設計模式** + **SOLID 原則**：

#### 1. Strategy Pattern (策略模式)
**PostbackHandler 統一行為**
```java
public interface PostbackHandler {
    boolean canHandle(String action);
    Message handle(PostbackEvent event);
    int getPriority();
}

// 5 個具體策略
- NavigationPostbackHandler (優先順序 10)
- AIPostbackHandler (優先順序 2)
- ReminderPostbackHandler (優先順序 1)
- EmailPostbackHandler (優先順序 3)
- LocationPostbackHandler (優先順序 4)
```

**優點**:
- 新增功能只需實作介面，無需修改現有程式碼 (OCP 原則)
- 每個 Handler < 170 行，職責單一 (SRP 原則)

#### 2. Chain of Responsibility (職責鏈模式)
**PostbackEventDispatcher 優先順序路由**
```java
@Service
public class PostbackEventDispatcher {
    private final List<PostbackHandler> handlers; // Spring 自動注入並排序

    public Message dispatch(PostbackEvent event, String action) {
        return handlers.stream()
                .filter(h -> h.canHandle(action))
                .findFirst()
                .map(h -> h.handle(event))
                .orElse(defaultResponse);
    }
}
```

**優點**:
- 自動優先順序路由，無需 if-else 或 switch-case
- 易於調整優先順序（修改 @Order 註解）

#### 3. Facade Pattern (外觀模式)
**封裝複雜業務邏輯協調**
```java
@Service
public class ReminderFacadeImpl implements ReminderFacade {
    // 協調 5 個 Service
    private final ReminderService reminderService;
    private final ReminderStateManager stateManager;
    private final ReminderLogService logService;
    private final ReminderNotificationService notificationService;
    private final MessageTemplateProvider templateProvider;

    @Override
    public Message startCreation(String roomId) {
        // 統一 Handler 與 Service 之間的複雜互動
    }
}
```

**優點**:
- Handler 依賴數量從 9 個降至 2 個 (-78%)
- 業務邏輯可復用於多個 Handler
- 測試簡化（Mock 1 個 Facade 而非 5 個 Service）

#### 4. Dependency Injection (依賴注入)
**Spring DI 降低耦合**
```java
@Service
@RequiredArgsConstructor  // Lombok 自動生成建構子
public class MessageProcessorService {
    private final AIService aiService;               // 依賴抽象
    private final ChatRoomManager chatRoomManager;   // 不依賴具體實作
    // Spring 自動注入 @Service 實作
}
```

---

### 重構成果對比

| 項目 | Before | After | 改善 |
|------|--------|-------|------|
| **PostbackEventHandler** | 477 行 | 35 行 | **-93%** ✨ |
| **ReminderScheduler** | 300 行 | 197 行 | **-34%** |
| **MessageProcessorService** | 332 行 | 189 行 | **-43%** |
| **ReminderPostbackHandler** | 242 行 | 168 行 | **-30%** |
| **EmailPostbackHandler** | 170 行 | 105 行 | **-38%** |
| **Handler 平均依賴數** | 9+ 個 | 2 個 | **-78%** |

**新增組件**:
- ✅ 5 個專用 Handler (Strategy Pattern)
- ✅ 1 個 Dispatcher (Chain of Responsibility)
- ✅ 3 個 Facade (Facade Pattern)
- ✅ 3 個 Notification Service (通知模組)

---

### SOLID 原則實踐

**S - Single Responsibility (單一職責)**
```java
// 每個類別只有一個改變的理由
@Service
public class AIServiceImpl implements AIService {
    // 只負責 AI 相關業務邏輯
}

@Service
public class ChatRoomManagerImpl implements ChatRoomManager {
    // 只負責聊天室管理邏輯
}
```

**O - Open/Closed (開放封閉)**
```java
// 對擴展開放，對修改封閉
public interface AIService {
    String generateResponse(String message);
}

// 新增 AI 提供商無需修改現有程式碼
public class GroqServiceImpl implements AIService { /* Groq 實作 */ }
public class OpenAIServiceImpl implements AIService { /* OpenAI 實作 */ }
```

**L - Liskov Substitution (里氏替換)**
```java
// 子類別可以完全替換父類別
AIService aiService = useGroq ? new GroqServiceImpl() : new OpenAIServiceImpl();
String response = aiService.generateResponse("Hello"); // 任何實作都能正常工作
```

**I - Interface Segregation (介面隔離)**
```java
// 分離不同職責的介面，避免肥大介面
public interface AIService {
    String generateResponse(String message);
}

public interface ChatRoomManager {
    ChatRoom findOrCreateChatRoom(String roomId);
}
```

**D - Dependency Inversion (依賴倒置)**
```java
// 依賴抽象介面，不依賴具體實作
@Service
public class MessageEventHandler {
    private final AIService aiService;  // 不是 GroqServiceImpl

    public MessageEventHandler(AIService aiService) {
        this.aiService = aiService;  // Spring 自動注入
    }
}
```

---

### 多實例部署架構

**設計特色**:
- **Load Balancer**: Nginx 分散請求至多個實例
- **Database-backed State**: 狀態存儲於資料庫，非記憶體
- **Distributed Lock**: 防止多實例重複處理提醒
- **無狀態設計**: 任何實例都能處理任何請求

**架構圖**:
```
┌─────────────────────────────────────────────────────────────┐
│                   LINE Messaging API                         │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTPS
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                 Nginx Load Balancer                          │
└────────┬─────────────────────────────┬──────────────────────┘
         │                             │
┌────────▼─────────┐       ┌──────────▼─────────┐
│ NexusBot Instance│       │ NexusBot Instance  │
│   (Port 5001)    │       │   (Port 5002)      │
└────────┬─────────┘       └──────────┬─────────┘
         │                             │
         └────────────┬────────────────┘
                      ▼
         ┌─────────────────────────┐
         │   MySQL Database        │
         │   (Master/Slave)        │
         └─────────────────────────┘
```

---

## 💻 技術棧

### 後端框架
- **Spring Boot**: 3.4.3
- **Java**: 17 (Toolchain 配置)
- **LINE Bot SDK**: 6.0.0
- **Spring Cloud**: 2024.0.0 (Bootstrap 配置)

### 資料庫
- **H2**: 本地開發與測試 (記憶體資料庫)
- **MySQL**: 開發與生產環境 (8.3.0)
- **Flyway**: 資料庫遷移與版本控制
- **JPA/Hibernate**: ORM 框架

### AI 整合
- **Groq API**: 主要 AI 服務 (llama-3.1-8b-instant)
- **WebFlux**: 非同步 HTTP 客戶端

### 其他技術
- **Thymeleaf**: HTML 模板引擎 (Email 渲染)
- **JavaMail**: SMTP Email 發送
- **Lombok**: 減少樣板程式碼
- **springdoc-openapi**: API 文檔生成 (Swagger UI)

### 建置工具
- **Gradle**: 8.x with Kotlin DSL
- **Git Tag Versioning**: 自動從 Git Tag 讀取版本號

---

## 🚀 快速開始

### 前置需求

- **Java 17+** ([下載 Adoptium JDK](https://adoptium.net/))
- **LINE Developer Account** ([註冊](https://developers.line.biz/))
- **Groq API Key** ([申請](https://console.groq.com/))

### 安裝步驟

1. **Clone 專案**
```bash
git clone https://github.com/yourusername/nexusbot.git
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
# 使用 Gradle Wrapper (推薦)
./gradlew bootRun

# 或建置 JAR 後執行
./gradlew bootJar
java -jar build/libs/nexusbot-*.jar
```

4. **驗證運作**
- 應用程式: http://localhost:5001
- H2 Console: http://localhost:5001/h2-console
- Swagger UI: http://localhost:5001/swagger-ui.html

### LINE Webhook 設定

1. 前往 [LINE Developers Console](https://developers.line.biz/console/)
2. 設定 Webhook URL: `https://your-domain.com/webhook`
3. 啟用 Webhook
4. 測試連線

---

## 📖 API 文檔

### Swagger UI (互動式文檔)
訪問 http://localhost:5001/swagger-ui.html 查看完整 API 文檔

### 主要端點

| 端點 | 方法 | 說明 |
|------|------|------|
| `/webhook` | POST | LINE Messaging API Webhook |
| `/reminder/confirm/{token}` | GET | Email 提醒確認連結 |
| `/h2-console` | GET | H2 資料庫控制台 (僅 local) |
| `/actuator/health` | GET | 健康檢查 |

### Postback 動作清單 (35+ 個)

<details>
<summary>點擊展開完整清單</summary>

**導航**:
- `MAIN_MENU` - 主選單
- `HELP_MENU` - 說明選單
- `ABOUT` - 關於頁面

**AI 對話**:
- `ENABLE_AI` / `DISABLE_AI` - 啟用/關閉 AI
- `SELECT_MODEL` - 模型選擇選單
- `MODEL_LLAMA_3_1_8B` - 切換至 Llama 3.1 8B
- `MODEL_LLAMA_3_3_70B` - 切換至 Llama 3.3 70B
- `MODEL_GEMMA2_9B` - 切換至 Gemma2 9B
- `MODEL_DEEPSEEK_R1` - 切換至 DeepSeek R1
- `MODEL_QWEN3_32B` - 切換至 Qwen3 32B
- `CLEAR_HISTORY` - 清除對話歷史

**提醒管理**:
- `REMINDER_MENU` - 提醒選單
- `ADD_REMINDER` - 新增提醒
- `LIST_REMINDERS` - 提醒列表
- `TODAY_REMINDERS` - 今日提醒記錄
- `REPEAT_ONCE` / `REPEAT_DAILY` / `REPEAT_WEEKLY` - 重複類型
- `CHANNEL_LINE` / `CHANNEL_EMAIL` / `CHANNEL_BOTH` - 通知管道
- `DELETE_REMINDER&id={id}` - 刪除提醒
- `REMINDER_COMPLETED&id={id}` - 完成提醒

**Email 通知**:
- `EMAIL_MENU` - Email 選單
- `ADD_EMAIL` - 新增 Email
- `DELETE_EMAIL&id={id}` - 刪除 Email
- `TOGGLE_EMAIL_STATUS&id={id}` - 切換啟用狀態

**位置服務**:
- `FIND_TOILETS` - 找附近廁所

</details>

詳細文檔: [docs/api/README.md](docs/api/README.md)

---

## 📊 架構圖與流程圖

### 系統架構圖
![System Architecture](docs/architecture/system-architecture.md)

**7 層架構**:
1. **Presentation Layer**: Controller, EventHandler
2. **Dispatcher Layer**: PostbackEventDispatcher
3. **Strategy Layer**: 5 個 PostbackHandler
4. **Facade Layer**: ReminderFacade, EmailFacade, LocationFacade
5. **Service Layer**: 業務邏輯 Service
6. **Repository Layer**: JPA Repository
7. **Database Layer**: MySQL / H2

### Postback 處理流程圖
![Postback Flow](docs/architecture/postback-flow-diagram.md)

**處理流程**:
```
LINE → Controller → EventHandler → Dispatcher
                                      ↓
                              PostbackHandler (Strategy)
                                      ↓
                                   Facade
                                      ↓
                              Service → Repository → DB
```

### PlantUML 時序圖

<details>
<summary>提醒建立流程</summary>

![Reminder Creation Sequence](docs/diagrams/reminder-creation-sequence.puml)

**6 步驟流程**:
1. 使用者點擊「新增提醒」
2. 選擇重複類型 (ONCE/DAILY/WEEKLY)
3. 選擇通知管道 (LINE/EMAIL/BOTH)
4. 輸入提醒時間 (AI 解析)
5. 輸入提醒內容
6. 完成建立 (儲存至資料庫)

</details>

<details>
<summary>提醒發送流程</summary>

![Reminder Sending Sequence](docs/diagrams/reminder-sending-sequence.puml)

**發送機制**:
- Scheduler 每分鐘觸發
- Distributed Lock 防重複
- AI 增強提醒內容
- 三種通知管道路由
- 用戶確認追蹤

</details>

<details>
<summary>AI 對話流程</summary>

![AI Chat Sequence](docs/diagrams/ai-chat-sequence.puml)

**處理流程**:
- 非同步處理 (CompletableFuture)
- 15 秒超時保護
- Fallback 策略
- 對話歷史管理 (15 則)
- 軟刪除設計

</details>

---

## 🗄️ 資料庫設計

### ERD (Entity-Relationship Diagram)
完整設計: [docs/architecture/erd-diagram.md](docs/architecture/erd-diagram.md)

### 8 個資料表

| 資料表 | 用途 | 關鍵欄位 |
|--------|------|----------|
| **chat_rooms** | 聊天室配置 | room_id (UK), ai_enabled, ai_model, is_admin |
| **chat_messages** | 對話記錄 | room_id (FK), role, content, tokens_used |
| **reminders** | 提醒設定 | room_id (FK), reminder_time, repeat_type, notification_channel |
| **reminder_logs** | 提醒記錄 | reminder_id (FK), status, delivery_method, confirmed_at |
| **reminder_states** | 提醒建立狀態 | room_id (UK), current_step, expires_at |
| **reminder_locks** | 提醒鎖 | lock_key (UK), expires_at |
| **emails** | Email 管理 | room_id (FK), email_address, is_active |
| **email_input_states** | Email 輸入狀態 | room_id (UK) |

### 設計原則

1. **無外鍵約束** (No FK Constraints)
   - 提升寫入效能
   - 避免鎖表問題
   - 應用層控制一致性

2. **索引策略**
   - 覆蓋最常用查詢
   - 避免過多索引影響寫入

3. **跨資料庫相容性**
   - 標準 SQL 語法
   - H2 (local/test) 與 MySQL (dev/prod) 相容

### Flyway 遷移

總計 **14 個遷移檔案** (V1-V14):
- V1-V2: 基礎表 (chat_rooms, chat_messages)
- V3-V6: AI 與管理員功能
- V7-V11: 提醒系統
- V12-V13: Email 功能
- V14: 通知管道

---

## 🧪 測試與品質

### 測試覆蓋率

- **總測試數**: 34 tests
- **成功率**: 100%
- **覆蓋範圍**: Handler, Dispatcher, Application Context

### 測試結構

```
src/test/java/
├── handler/postback/
│   ├── NavigationPostbackHandlerTest (9 tests)
│   ├── AIPostbackHandlerTest (13 tests)
│   ├── LocationPostbackHandlerTest (5 tests)
│   └── PostbackEventDispatcherTest (6 tests)
└── NexusBotApplicationTests (1 test)
```

### 執行測試

```bash
# 執行所有測試
./gradlew test

# 執行單一測試類別
./gradlew test --tests NavigationPostbackHandlerTest

# 測試涵蓋率報告
./gradlew test jacocoTestReport
```

### 程式碼品質

- **SonarQube**: 無 Critical 問題
- **Checkstyle**: 遵循 Google Java Style Guide
- **SpotBugs**: 無潛在 Bug

---

## 🚢 部署指南

### Docker 部署

1. **建立 Docker Image**
```bash
docker build -t nexusbot:latest .
```

2. **執行容器**
```bash
docker run -d \
  --name nexusbot \
  -p 5001:5001 \
  -e LINE_CHANNEL_TOKEN=your_token \
  -e LINE_CHANNEL_SECRET=your_secret \
  -e GROQ_API_KEY=your_key \
  -e SPRING_PROFILES_ACTIVE=prod \
  nexusbot:latest
```

### 環境變數配置

| 變數 | 說明 | 範例 |
|------|------|------|
| `LINE_CHANNEL_TOKEN` | LINE Channel Access Token | `AbcD1234...` |
| `LINE_CHANNEL_SECRET` | LINE Channel Secret | `1234abcd...` |
| `GROQ_API_KEY` | Groq API 金鑰 | `gsk_...` |
| `SPRING_PROFILES_ACTIVE` | 環境設定檔 | `local` / `dev` / `prod` |
| `DB_URL` | 資料庫連線 URL | `jdbc:mysql://localhost:3306/nexusbot` |
| `DB_USERNAME` | 資料庫使用者名稱 | `root` |
| `DB_PASSWORD` | 資料庫密碼 | `password` |

### 雲端部署

<details>
<summary>AWS EC2 部署</summary>

1. 啟動 EC2 實例 (Ubuntu 22.04)
2. 安裝 Java 17 與 Docker
3. 設定 Security Group (開放 Port 5001)
4. 使用 GitHub Actions 自動部署

</details>

<details>
<summary>GCP Cloud Run 部署</summary>

```bash
gcloud run deploy nexusbot \
  --image gcr.io/PROJECT_ID/nexusbot \
  --platform managed \
  --region asia-east1 \
  --allow-unauthenticated
```

</details>

---

## 📁 專案結構

```
nexusbot/
├── src/
│   ├── main/
│   │   ├── java/com/acenexus/tata/nexusbot/
│   │   │   ├── config/              # 配置類別
│   │   │   │   ├── LineBotConfig.java
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   └── SchedulingConfig.java
│   │   │   ├── controller/          # HTTP 端點
│   │   │   │   ├── LineBotController.java
│   │   │   │   └── ReminderConfirmationController.java
│   │   │   ├── handler/             # 事件處理器
│   │   │   │   ├── postback/        # Postback 處理 (Strategy Pattern)
│   │   │   │   │   ├── PostbackHandler.java (介面)
│   │   │   │   │   ├── PostbackEventDispatcher.java (Dispatcher)
│   │   │   │   │   ├── NavigationPostbackHandler.java
│   │   │   │   │   ├── AIPostbackHandler.java
│   │   │   │   │   ├── ReminderPostbackHandler.java
│   │   │   │   │   ├── EmailPostbackHandler.java
│   │   │   │   │   └── LocationPostbackHandler.java
│   │   │   │   ├── MessageEventHandler.java
│   │   │   │   ├── PostbackEventHandler.java
│   │   │   │   ├── FollowEventHandler.java
│   │   │   │   └── GroupEventHandler.java
│   │   │   ├── facade/              # Facade 層 (業務邏輯協調)
│   │   │   │   ├── ReminderFacade.java
│   │   │   │   ├── EmailFacade.java
│   │   │   │   └── LocationFacade.java
│   │   │   ├── service/             # 應用服務層
│   │   │   │   ├── EventHandlerService.java
│   │   │   │   ├── MessageService.java
│   │   │   │   ├── MessageProcessorService.java
│   │   │   │   └── AdminService.java
│   │   │   ├── ai/                  # AI 領域
│   │   │   │   ├── AIService.java
│   │   │   │   └── AIServiceImpl.java
│   │   │   ├── chatroom/            # 聊天室領域
│   │   │   │   ├── ChatRoomManager.java
│   │   │   │   └── ChatRoomManagerImpl.java
│   │   │   ├── template/            # 訊息模板領域
│   │   │   │   ├── MessageTemplateProvider.java
│   │   │   │   ├── MessageTemplateProviderImpl.java
│   │   │   │   └── UIConstants.java
│   │   │   ├── reminder/            # 提醒領域
│   │   │   │   ├── ReminderService.java
│   │   │   │   ├── ReminderStateManager.java
│   │   │   │   └── ReminderLogService.java
│   │   │   ├── notification/        # 通知模組 (NEW - Week 3)
│   │   │   │   ├── ReminderNotificationService.java
│   │   │   │   ├── LineNotificationService.java
│   │   │   │   └── EmailNotificationService.java
│   │   │   ├── email/               # Email 領域
│   │   │   │   ├── EmailManager.java
│   │   │   │   └── EmailInputStateRepository.java
│   │   │   ├── location/            # 位置服務領域
│   │   │   │   └── LocationService.java
│   │   │   ├── scheduler/           # 排程任務
│   │   │   │   └── ReminderScheduler.java
│   │   │   ├── entity/              # JPA 實體
│   │   │   ├── repository/          # 資料存取層
│   │   │   ├── util/                # 工具類別
│   │   │   └── constants/           # 常數定義
│   │   └── resources/
│   │       ├── bootstrap.yml        # 基礎配置
│   │       ├── bootstrap-local.yml  # 本地開發配置
│   │       ├── db/migration/        # Flyway 遷移腳本
│   │       └── templates/           # Thymeleaf 模板
│   └── test/
│       ├── java/                    # 單元測試
│       └── resources/
│           └── demo-data.sql        # Demo 測試數據
├── docs/                            # 專案文檔
│   ├── architecture/                # 架構文檔
│   │   ├── system-architecture.md
│   │   ├── postback-flow-diagram.md
│   │   └── erd-diagram.md
│   ├── diagrams/                    # PlantUML 時序圖
│   │   ├── reminder-creation-sequence.puml
│   │   ├── reminder-sending-sequence.puml
│   │   └── ai-chat-sequence.puml
│   ├── api/                         # API 文檔
│   │   └── README.md
│   ├── demo/                        # Demo 腳本
│   │   └── demo-script.md
│   ├── Week1-2-完成總結.md          # Phase 1 重構成果
│   ├── Week3-完成總結.md            # Phase 2 通知模組
│   ├── Week4-完成總結.md            # Phase 3 視覺化文檔
│   └── TODO-詳細待辦清單.md         # 專案待辦清單
├── build.gradle.kts                 # Gradle 建置腳本
├── CLAUDE.md                        # Claude Code 開發指南
└── README.md                        # 專案說明文檔 (本文件)
```

---

## 🛠️ 開發指南

### 本地開發環境

1. **Java 版本管理**
```bash
# 檢查 Java 版本
java -version  # 應為 Java 17+

# Windows 設定 JAVA_HOME
set JAVA_HOME="C:\Program Files\Java\jdk-17"
set PATH="C:\Program Files\Java\jdk-17\bin;%PATH%"

# Linux/Mac 設定 JAVA_HOME
export JAVA_HOME="/path/to/jdk-17"
export PATH="$JAVA_HOME/bin:$PATH"
```

2. **IDE 設定** (推薦 IntelliJ IDEA)
- 安裝 Lombok Plugin
- 啟用 Annotation Processing
- 設定 Code Style: Google Java Style Guide

3. **資料庫設定**
```bash
# 本地開發 (H2 記憶體資料庫)
./gradlew bootRun  # 自動建立資料表

# H2 Console
訪問: http://localhost:5001/h2-console
JDBC URL: jdbc:h2:mem:nexusbot
Username: sa
Password: (留空)
```

### Gradle 常用指令

```bash
# 編譯專案
./gradlew clean build

# 執行應用程式
./gradlew bootRun

# 執行測試
./gradlew test

# 建立可執行 JAR
./gradlew bootJar

# 檢查依賴更新
./gradlew dependencyUpdates

# 查看專案資訊
./gradlew properties
```

### 新增功能指南

<details>
<summary>如何新增一個 PostbackHandler？</summary>

1. **建立 Handler 類別**
```java
@Service
@Order(5)  // 設定優先順序
@RequiredArgsConstructor
public class MyPostbackHandler implements PostbackHandler {

    private final MyFacade myFacade;

    @Override
    public boolean canHandle(String action) {
        return action.equals("MY_ACTION");
    }

    @Override
    public Message handle(PostbackEvent event) {
        return myFacade.handleMyAction(event.getRoomId());
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
```

2. **定義 Postback 常數**
```java
// constants/Actions.java
public static final String MY_ACTION = "action=MY_ACTION";
```

3. **Spring 自動註冊**
- 無需修改 Dispatcher
- 自動按優先順序路由

</details>

<details>
<summary>如何新增一個通知管道？</summary>

1. **建立 NotificationService**
```java
@Service
@RequiredArgsConstructor
public class SmsNotificationService {

    public void sendSms(Reminder reminder, String phoneNumber) {
        // 實作 SMS 發送邏輯
    }
}
```

2. **更新 ReminderNotificationService**
```java
public void send(Reminder reminder, String content) {
    switch (reminder.getNotificationChannel()) {
        case LINE -> lineService.push(...);
        case EMAIL -> emailService.send(...);
        case SMS -> smsService.sendSms(...);  // 新增
        case BOTH -> sendBoth(...);
    }
}
```

3. **新增資料庫 Enum**
```sql
-- V15__Add_sms_notification_channel.sql
ALTER TABLE reminders
MODIFY COLUMN notification_channel ENUM('LINE', 'EMAIL', 'BOTH', 'SMS');
```

</details>

### 程式碼風格

- 遵循 **Google Java Style Guide**
- 使用 **Lombok** 減少樣板程式碼
- 介面命名: `XXXService`, 實作命名: `XXXServiceImpl`
- 常數使用: `UPPER_SNAKE_CASE`
- 類別使用: `PascalCase`
- 方法使用: `camelCase`

---

## 📚 參考文檔

### 外部文檔
- [LINE Messaging API](https://developers.line.biz/en/reference/messaging-api/)
- [Spring Boot Documentation](https://spring.io/guides/gs/spring-boot/)
- [Flyway Documentation](https://documentation.red-gate.com/flyway/)
- [Domain-Driven Design Reference](https://domainlanguage.com/ddd/reference/)
- [SOLID Principles](https://en.wikipedia.org/wiki/SOLID)
- [Groq API Documentation](https://console.groq.com/docs)

### 內部文檔
- [架構設計說明](docs/architecture/)
- [API 文檔](docs/api/README.md)
- [Demo 腳本](docs/demo/demo-script.md)
- [開發指南](CLAUDE.md)
- [完成總結](docs/Week4-完成總結.md)

---

## 🤝 貢獻與授權

### 貢獻指南

歡迎貢獻！請遵循以下步驟：

1. Fork 本專案
2. 建立 Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit 變更 (`git commit -m 'Add some AmazingFeature'`)
4. Push 到 Branch (`git push origin feature/AmazingFeature`)
5. 開啟 Pull Request

### 授權條款

本專案採用 **MIT License** 授權。

詳細內容請參閱 [LICENSE](LICENSE) 檔案。

---

## 📧 聯絡方式

- **專案維護者**: NexusBot Team
- **Email**: support@nexusbot.example.com
- **GitHub**: https://github.com/yourusername/nexusbot

---

## 🌟 致謝

感謝以下開源專案：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [LINE Bot SDK](https://github.com/line/line-bot-sdk-java)
- [Groq](https://groq.com/)
- [Flyway](https://flywaydb.org/)

---

**版本**: 1.0.0
**最後更新**: 2025-10-08
**專案狀態**: 🚀 Production Ready

---

Made with ❤️ by NexusBot Team
