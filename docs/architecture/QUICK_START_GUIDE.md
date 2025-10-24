# 統一錯誤處理機制 - 快速開始指南

**版本**: 1.0.0
**更新日期**: 2025-10-24

---

## 🚀 5 分鐘快速上手

### 步驟 1: 理解錯誤分類

NexusBot 使用 3 種例外類別：

| 例外類別 | 用途 | HTTP 狀態 | 日誌級別 |
|---------|------|-----------|----------|
| **BusinessException** | 使用者操作錯誤 | 400 | WARN |
| **SystemException** | 系統內部錯誤 | 500 | ERROR |
| **ExternalServiceException** | 第三方服務錯誤 | 502/504 | ERROR |

### 步驟 2: 使用靜態工廠方法

最簡單的方式是使用內建的靜態工廠方法：

```java
// 提醒不存在（業務錯誤）
throw BusinessException.reminderNotFound(reminderId, roomId);

// 資料庫錯誤（系統錯誤）
throw SystemException.databaseError("saveMessage", cause);

// AI 服務超時（第三方服務錯誤）
throw ExternalServiceException.aiServiceTimeout(model, 15000);
```

### 步驟 3: 在 Facade 層處理例外

```java
@Service
public class MyFacadeImpl implements MyFacade {

    public Message handleOperation(String roomId, String input) {
        try {
            // 呼叫 Service 層
            return myService.doSomething(roomId, input);

        } catch (BusinessException e) {
            // 使用者操作錯誤：返回友善訊息
            logger.warn("Business error: {}", e.getMessage());
            return messageTemplateProvider.error(e.getErrorCode().getMessage());

        } catch (ExternalServiceException e) {
            // 第三方服務錯誤：提供 fallback
            logger.error("External service error: {}", e.getMessage(), e);
            return messageTemplateProvider.error("服務暫時無法使用，請稍後再試");

        } catch (Exception e) {
            // 未預期的錯誤：轉換為 SystemException
            logger.error("Unexpected error: {}", e.getMessage(), e);
            throw SystemException.internalError("handleOperation", e)
                    .withMetadata("roomId", roomId);
        }
    }
}
```

### 步驟 4: 查看日誌中的 TraceId

啟動應用程式後，日誌會自動包含 traceId：

```
2025-10-24 14:30:15.123 [550e8400e29b41d4a716446655440000] [http-nio-5001-exec-1] INFO  Service - Creating reminder
```

使用 traceId 追蹤完整請求流程：

```bash
# 搜尋特定 traceId 的所有日誌
grep "550e8400e29b41d4a716446655440000" logs/nexusbot.log
```

---

## 📖 詳細說明

### 錯誤碼一覽

完整錯誤碼清單請參考 `exception/ErrorCode.java`，以下是常用錯誤碼：

#### 提醒相關 (REM_xxx)
- `REM_001`: 提醒不存在
- `REM_002`: 時間格式錯誤
- `REM_003`: 提醒時間必須是未來
- `REM_004`: 提醒內容為空
- `REM_005`: 提醒創建失敗
- `REM_006`: 提醒刪除失敗

#### AI 服務 (AI_xxx)
- `AI_001`: AI 服務超時
- `AI_002`: AI API 呼叫失敗
- `AI_003`: AI 回應解析失敗
- `AI_004`: AI 服務未配置

#### LINE API (LINE_xxx)
- `LINE_001`: LINE API 呼叫失敗
- `LINE_002`: 訊息發送失敗
- `LINE_003`: LINE Token 無效

#### Email (EMAIL_xxx)
- `EMAIL_001`: Email 發送失敗
- `EMAIL_002`: Email 格式錯誤
- `EMAIL_003`: Email 不存在
- `EMAIL_004`: Email 已存在

---

## 💡 常見使用場景

### 場景 1: Service 層驗證

```java
@Service
public class ReminderServiceImpl implements ReminderService {

    @Override
    public Reminder getReminder(Long reminderId, String roomId) {
        // 直接拋出例外，讓 Facade 層處理
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> BusinessException.reminderNotFound(reminderId, roomId));

        // 驗證權限
        if (!reminder.getRoomId().equals(roomId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED)
                    .withMetadata("reminderId", reminderId)
                    .withMetadata("expectedRoomId", roomId)
                    .withMetadata("actualRoomId", reminder.getRoomId());
        }

        return reminder;
    }
}
```

### 場景 2: 第三方 API 呼叫

```java
@Service
public class AIServiceImpl implements AIService {

    @Override
    public ChatResponse chat(String message, String model) {
        long startTime = System.currentTimeMillis();

        try {
            var response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(buildRequest(message, model))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            return parseResponse(response);

        } catch (TimeoutException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            throw ExternalServiceException.aiServiceTimeout(model, processingTime)
                    .withMetadata("message", message);

        } catch (Exception e) {
            throw ExternalServiceException.aiApiError(model, e)
                    .withMetadata("message", message);
        }
    }
}
```

### 場景 3: 資料庫操作

```java
@Service
public class MessageService {

    public void saveMessage(ChatMessage message) {
        try {
            chatMessageRepository.save(message);
            logger.info("Message saved: {}", message.getId());

        } catch (DataAccessException e) {
            throw SystemException.databaseError("saveMessage", e)
                    .withMetadata("messageId", message.getId())
                    .withMetadata("roomId", message.getRoomId());
        }
    }
}
```

### 場景 4: 參數驗證

```java
@Service
public class EmailFacadeImpl implements EmailFacade {

    public Message addEmail(String roomId, String email) {
        // Email 格式驗證
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw BusinessException.invalidEmailFormat(email)
                    .withMetadata("roomId", roomId);
        }

        // 重複檢查
        if (emailRepository.existsByRoomIdAndEmail(roomId, email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS)
                    .withMetadata("roomId", roomId)
                    .withMetadata("email", email);
        }

        // 繼續處理...
    }
}
```

---

## 🔍 除錯技巧

### 1. 使用 TraceId 追蹤請求

```bash
# 從日誌中提取 traceId
tail -f logs/nexusbot.log | grep "Creating reminder"

# 使用 traceId 查找完整請求流程
grep "550e8400e29b41d4a716446655440000" logs/nexusbot.log
```

### 2. 查看錯誤統計

```bash
# 統計最常見的錯誤碼
grep "ERROR" logs/nexusbot-error.log | grep -oP "errorCode=\K\w+" | sort | uniq -c | sort -rn
```

### 3. 分析錯誤趨勢

```bash
# 按小時統計錯誤數量
grep "ERROR" logs/nexusbot-error.log | cut -d' ' -f2 | cut -d':' -f1 | uniq -c
```

---

## 📚 延伸閱讀

### 完整文檔

1. **error-handling-analysis.md**
   - 現況分析
   - 架構設計
   - 改進建議

2. **error-handling-usage-examples.md**
   - 20+ 個實際範例
   - Before/After 重構對比
   - 最佳實踐

3. **error-handling-implementation-summary.md**
   - 實作總結
   - 統計數據
   - 驗收標準

4. **CLAUDE.md**
   - 架構概覽
   - 開發指南
   - 整合說明

### 原始碼參考

- `exception/ErrorCode.java` - 完整錯誤碼清單
- `exception/BusinessException.java` - 業務例外範例
- `exception/SystemException.java` - 系統例外範例
- `exception/ExternalServiceException.java` - 第三方服務例外範例

---

## ❓ FAQ

### Q1: 什麼時候使用 BusinessException？

**A**: 當使用者的輸入或操作不符合業務規則時，例如：
- 提醒不存在
- 時間格式錯誤
- Email 格式錯誤
- 權限不足

### Q2: 什麼時候使用 SystemException？

**A**: 當系統內部發生錯誤時，例如：
- 資料庫連線失敗
- 配置缺失
- 記憶體不足
- 檔案 I/O 錯誤

### Q3: 什麼時候使用 ExternalServiceException？

**A**: 當第三方服務呼叫失敗時，例如：
- AI API 超時
- LINE API 錯誤
- Email 發送失敗
- 位置服務不可用

### Q4: 如何添加新的錯誤碼？

**A**: 在 `ErrorCode.java` 中添加新的枚舉值：

```java
/**
 * 新功能錯誤
 */
NEW_FEATURE_ERROR("NEW_001", "新功能錯誤訊息"),
```

### Q5: TraceId 如何在非同步處理中使用？

**A**: 手動傳遞 traceId：

```java
String traceId = MDC.get("traceId");

CompletableFuture.runAsync(() -> {
    MDC.put("traceId", traceId);
    try {
        // 非同步操作
    } finally {
        MDC.remove("traceId");
    }
});
```

### Q6: 如何在測試中驗證例外？

**A**: 使用 JUnit 5 的 assertThrows：

```java
@Test
void testReminderNotFound() {
    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> reminderService.getReminder(999L, "U123")
    );

    assertEquals(ErrorCode.REMINDER_NOT_FOUND, exception.getErrorCode());
    assertEquals(999L, exception.getMetadata().get("reminderId"));
}
```

---

## 🎯 下一步

1. **立即開始**: 在新功能中使用統一錯誤處理
2. **逐步重構**: 將現有 Exception 替換為自訂例外
3. **監控整合**: 設定錯誤碼統計和告警
4. **團隊培訓**: 分享最佳實踐給團隊成員

---

## 💬 需要幫助？

- 查看完整文檔：`docs/architecture/error-handling-*.md`
- 參考範例程式碼：`exception/*.java`
- 閱讀架構說明：`CLAUDE.md`

---

**版本**: 1.0.0
**作者**: Claude Code
**最後更新**: 2025-10-24
