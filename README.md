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

## 技術架構

### 領域驅動設計 (DDD)

本專案採用 DDD 架構，按業務領域劃分模組，讓程式碼結構對應實際業務邏輯：

```
src/main/java/com/acenexus/tata/nexusbot/
├── ai/          # AI 領域 - 聊天機器人核心邏輯
├── chatroom/    # 聊天室領域 - 房間管理與設定  
├── template/    # 訊息模板領域 - LINE UI 元件
└── service/     # 應用服務層 - 協調各領域互動
```

**DDD 核心概念應用**：

- **領域分離**：AI、聊天室、模板各自獨立，降低耦合
- **實體設計**：ChatRoom 實體封裝聊天室狀態與行為
- **領域服務**：AIService 處理 AI 相關的業務邏輯
- **應用服務**：LineMessageService 協調各領域協作

### SOLID 原則實踐

本專案嚴格遵循 SOLID 五大原則，確保程式碼可維護性：

**S - 單一職責原則**

```java

@Service
public class AIServiceImpl implements AIService {
    // 只負責 AI 相關的業務邏輯
}

@Service
public class ChatRoomService {
    // 只負責聊天室管理邏輯
}
```

**O - 開放封閉原則**

```java
public interface AIService {
    String chat(String message);
}
// 可以新增不同 AI 提供商實作，無需修改現有程式碼
```

**L - 里氏替換原則**

```java
// 所有 AIService 實作都能完全替換使用
AIService groqService = new GroqServiceImpl();
AIService openaiService = new OpenAIServiceImpl(); // 未來擴展
```

**I - 介面隔離原則**

```java
public interface AIService {
    String chat(String message); // 只包含 AI 相關方法
}

public interface ChatRoomService {
    ChatRoom findOrCreate(String roomId); // 只包含聊天室相關方法
}
```

**D - 依賴倒置原則**

```java

@Service
public class LineMessageService {
    private final AIService aiService; // 依賴抽象介面

    public LineMessageService(AIService aiService) {
        this.aiService = aiService; // Spring 自動注入具體實作
    }
}
```

## 技術棧

**後端**: Spring Boot 3.4.3, Java 17+, LINE Bot SDK 6.0.0  
**資料庫**: H2 (本地) / MySQL (生產), Flyway 遷移  
**測試**: JUnit 5  
**AI**: Groq API

## 資料庫設計

**ChatRoom 實體** - 聊天室 AI 設定管理

```sql
CREATE TABLE chat_rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id VARCHAR(100) NOT NULL UNIQUE,
    room_type VARCHAR(10) NOT NULL,
    ai_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

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

## 專案特色

### 架構設計優勢

**領域驅動設計 (DDD,domain-driven design)**

- 業務邏輯清晰：程式碼結構直接對應業務概念
- 團隊溝通：開發人員與業務人員使用相同語言
- 可維護性：每個領域獨立演進，降低變更影響範圍

**SOLID 原則收益**

- **容易測試**：介面分離，可獨立進行單元測試
- **易於擴展**：新增 AI 提供商或訊息類型無需修改核心邏輯
- **降低耦合**：各模組職責明確，依賴關係清晰
- **提升重用**：抽象介面設計，組件可在不同場景重複使用

### 技術亮點

- **多層架構**：Controller → Service → Repository，職責分離清晰
- **配置外部化**：環境變數管理，支援多環境部署
- **優雅降級**：AI 服務異常時系統仍可正常運作
- **型別安全**：強型別設計，編譯期錯誤檢查
- **資料庫版本控制**：Flyway 管理 schema 演進歷程
- **完整測試覆蓋**：單元測試 + 整合測試雙重保障

**參考文檔
**: [LINE API](https://developers.line.biz/en/docs/messaging-api/) | [Spring Boot](https://spring.io/guides/gs/spring-boot/) | [Flyway](https://documentation.red-gate.com/flyway/) | [DDD Reference](https://domainlanguage.com/ddd/reference/) | [SOLID Principles](https://en.wikipedia.org/wiki/SOLID)