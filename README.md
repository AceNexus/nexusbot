# NexusBot

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://adoptium.net/)
[![LINE Bot SDK](https://img.shields.io/badge/LINE%20Bot%20SDK-6.0.0-00C300.svg)](https://github.com/line/line-bot-sdk-java)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> LINE Bot 應用程式，整合 AI 聊天功能，採用 SOLID 原則和 DDD 架構設計

## 核心功能

- **AI 聊天**: Groq API 自然語言處理，繁體中文回應
- **多媒體支援**: 文字、圖片、貼圖、語音、檔案處理
- **互動選單**: LINE Flex Message 卡片式介面
- **聊天室管理**: 個人/群組對話，獨立 AI 功能設定
- **資料庫遷移**: Flyway 版本控制，跨環境 schema 一致性
- **SOLID 架構**: 領域驅動設計，介面分離，依賴注入

## 技術架構

### 領域驅動設計 (Domain-Driven Design, DDD)

本專案採用 DDD 架構，按業務領域劃分模組，讓程式碼結構對應實際業務邏輯：

```
src/main/java/com/acenexus/tata/nexusbot/
├── ai/          # AI 領域 - 聊天機器人核心邏輯
├── chatroom/    # 聊天室領域 - 房間管理與設定  
├── template/    # 訊息模板領域 - LINE UI 元件
└── service/     # 應用服務層 - 協調各領域互動
```

**為什麼使用 DDD？**

傳統專案常按技術層分包（controller、service、repository），但 DDD 按業務領域分包：

```
傳統分包          DDD 分包
controller/   →   ai/service/
service/      →   chatroom/service/  
model/        →   template/service/
```

**優點**：

- **業務理解更直觀**：資料夾名稱直接對應業務功能
- **團隊協作更容易**：產品經理看程式碼結構就能理解功能分工

### SOLID 原則實踐

本專案遵循 SOLID 五大原則，確保程式碼可維護性：

**S - 單一職責原則 (Single Responsibility)**

*每個類別只有一個改變的理由*

```java

@Service
public class AIServiceImpl implements AIService {
    // 只負責 AI 相關的業務邏輯
    // 如果 AI API 規格改變，只會影響這個類別
}

@Service
public class ChatRoomManagerImpl implements ChatRoomManager {
    // 只負責聊天室管理邏輯
    // 如果聊天室規則改變，只會影響這個類別
}
```

**實務意義**：當需求變更時，每次只會修改一個類別，減少 bug 風險。

**O - 開放封閉原則 (Open/Closed)**

*對擴展開放，對修改封閉*

```java
public interface AIService {
    String generateResponse(String message);
}

// 擴展新的 AI 提供商，不需要修改現有程式碼
public class GroqServiceImpl implements AIService { /* Groq 實作 */
}

public class OpenAIServiceImpl implements AIService { /* OpenAI 實作 */
}

public class ClaudeServiceImpl implements AIService { /* Claude 實作 */
}
```

**實務意義**：新增功能不會破壞既有功能，降低測試成本。

**L - 里氏替換原則 (Liskov Substitution)**

*子類別可以完全替換父類別*

```java
// 所有 AIService 實作都有相同的行為契約
AIService aiService = condition ? new GroqServiceImpl() : new OpenAIServiceImpl();
String response = aiService.generateResponse("Hello"); // 任何實作都能正常工作
```

**實務意義**：可以動態切換實作不影響系統運作。

**I - 介面隔離原則 (Interface Segregation)**

*客戶端不應該依賴它不使用的介面*

```java
// 分離不同職責的介面，避免肥大介面
public interface AIService {
    String generateResponse(String message); // 只包含 AI 功能
}

public interface ChatRoomManager {
    ChatRoom findOrCreateChatRoom(String roomId); // 只包含聊天室功能
}

// 錯誤示範：肥大介面
// public interface BotService {
//     String generateResponse(String message);     // AI 功能
//     ChatRoom findChatRoom(String roomId);       // 聊天室功能  
//     FlexMessage createMenu();                   // 選單功能
// }
```

**實務意義**：類別只需要實作它真正需要的方法，提高程式碼品質。

**D - 依賴倒置原則 (Dependency Inversion)**

*高層模組不應該依賴低層模組，兩者都應該依賴抽象*

```java

@Service
public class MessageEventHandler {
    // 依賴抽象介面，不依賴具體實作
    private final AIService aiService;             // 不是 GroqServiceImpl
    private final ChatRoomManager chatRoomManager; // 不是 ChatRoomManagerImpl

    // Spring 會自動注入具體實作
    public MessageEventHandler(AIService aiService, ChatRoomManager chatRoomManager) {
        this.aiService = aiService;
        this.chatRoomManager = chatRoomManager;
    }
}
```

**實務意義**：容易進行單元測試（注入 Mock 物件），也容易替換不同實作。

## 技術棧

**後端**: Spring Boot 3.4.3, Java 17+, LINE Bot SDK 6.0.0  
**資料庫**: H2 (本地) / MySQL (生產), Flyway 遷移  
**測試**: JUnit 5  
**AI**: Groq API

## 開發與部署

**本地開發**

```bash
./gradlew bootRun  # H2 記憶體資料庫
./gradlew test     # 執行測試
./gradlew bootJar  # 建置 JAR
```

**環境配置**: local (H2) → dev (MySQL) → prod (MySQL)

**主要端點**

- `POST /webhook` - LINE Bot Webhook
- `GET /actuator/health` - 健康檢查
- `GET /h2-console` - 本地資料庫控制台

## 快速開始

**前置需求**: Java 17+、LINE Developer Console、Groq API Key

```bash
# 下載並執行
git clone https://github.com/AceNexus/nexusbot.git
cd nexusbot

# 配置 LINE Bot 和 Groq API 金鑰
vim src/main/resources/bootstrap-local.yml

# 建置執行
./gradlew bootRun
```

**參考文檔
**: [LINE API](https://developers.line.biz/en/docs/messaging-api/) | [Spring Boot](https://spring.io/guides/gs/spring-boot/) | [Flyway](https://documentation.red-gate.com/flyway/) | [DDD Reference](https://domainlanguage.com/ddd/reference/) | [SOLID Principles](https://en.wikipedia.org/wiki/SOLID)