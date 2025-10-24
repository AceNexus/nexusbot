# 錯誤處理機制使用範例

本文件提供 NexusBot 統一錯誤處理機制的實際使用範例。

---

## 📚 目錄

1. [自訂例外使用範例](#自訂例外使用範例)
2. [ErrorResponse 使用範例](#errorresponse-使用範例)
3. [TraceId 追蹤範例](#traceid-追蹤範例)
4. [重構現有程式碼範例](#重構現有程式碼範例)
5. [最佳實踐](#最佳實踐)

---

## 自訂例外使用範例

### 1. BusinessException（業務例外）

#### 範例 1: 提醒不存在

```java
@Service
public class ReminderServiceImpl implements ReminderService {

    @Override
    public Reminder getReminder(Long reminderId, String roomId) {
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

#### 範例 2: 時間格式錯誤

```java
@Service
public class ReminderFacadeImpl implements ReminderFacade {

    private Message handleTimeInput(String roomId, String input) {
        LocalDateTime reminderTime;

        try {
            reminderTime = parseTime(input);
        } catch (Exception e) {
            // 使用自訂例外
            throw BusinessException.invalidTimeFormat(input)
                    .withMetadata("roomId", roomId);
        }

        if (reminderTime.isBefore(LocalDateTime.now())) {
            throw BusinessException.pastTimeNotAllowed(
                    reminderTime.format(TIME_FORMATTER))
                    .withMetadata("roomId", roomId)
                    .withMetadata("inputTime", reminderTime);
        }

        // ... 繼續處理
    }
}
```

#### 範例 3: 參數驗證

```java
@Service
public class EmailFacadeImpl implements EmailFacade {

    public Message addEmail(String roomId, String email) {
        // 驗證 Email 格式
        if (!isValidEmail(email)) {
            throw BusinessException.invalidEmailFormat(email)
                    .withMetadata("roomId", roomId);
        }

        // 檢查是否已存在
        if (emailRepository.existsByRoomIdAndEmail(roomId, email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS)
                    .withMetadata("roomId", roomId)
                    .withMetadata("email", email);
        }

        // ... 繼續處理
    }
}
```

---

### 2. SystemException（系統例外）

#### 範例 1: 資料庫錯誤

```java
@Service
public class ChatMessageService {

    public void saveMessage(ChatMessage message) {
        try {
            chatMessageRepository.save(message);
        } catch (DataAccessException e) {
            throw SystemException.databaseError("saveMessage", e)
                    .withMetadata("messageId", message.getId())
                    .withMetadata("roomId", message.getRoomId());
        }
    }
}
```

#### 範例 2: 配置錯誤

```java
@Component
public class ConfigValidator {

    @PostConstruct
    public void validateConfiguration() {
        if (!StringUtils.hasText(lineBotProperties.getChannelToken())) {
            throw SystemException.configurationError("LINE_CHANNEL_TOKEN");
        }

        if (!StringUtils.hasText(groqProperties.getApiKey())) {
            throw SystemException.configurationError("GROQ_API_KEY");
        }
    }
}
```

#### 範例 3: 內部錯誤

```java
@Service
public class ReminderScheduler {

    @Scheduled(cron = "0 * * * * *")
    public void processReminders() {
        try {
            List<Reminder> dueReminders = findDueReminders();
            for (Reminder reminder : dueReminders) {
                processReminder(reminder);
            }
        } catch (Exception e) {
            throw SystemException.internalError("processReminders", e)
                    .withMetadata("timestamp", LocalDateTime.now());
        }
    }
}
```

---

### 3. ExternalServiceException（第三方服務例外）

#### 範例 1: AI 服務超時

```java
@Service
public class AIServiceImpl implements AIService {

    @Override
    public ChatResponse chatWithContext(String roomId, String message, String model) {
        long startTime = System.currentTimeMillis();

        try {
            var response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            return parseResponse(response);

        } catch (TimeoutException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            throw ExternalServiceException.aiServiceTimeout(model, processingTime)
                    .withMetadata("roomId", roomId)
                    .withMetadata("message", message);

        } catch (Exception e) {
            throw ExternalServiceException.aiApiError(model, e)
                    .withMetadata("roomId", roomId);
        }
    }
}
```

#### 範例 2: LINE API 錯誤

```java
@Service
public class MessageService {

    public void sendPushMessage(String roomId, Message message) {
        try {
            lineMessagingClient.pushMessage(
                    new PushMessage(roomId, message)
            ).get();

        } catch (InterruptedException | ExecutionException e) {
            throw ExternalServiceException.lineMessageSendFailed(roomId, e);
        }
    }
}
```

#### 範例 3: Email 發送失敗

```java
@Service
public class EmailNotificationService {

    public void sendEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // ... 設定 message

            mailSender.send(message);

        } catch (MailException e) {
            throw ExternalServiceException.emailSendFailed(to, e);
        }
    }
}
```

---

## ErrorResponse 使用範例

### 1. 簡單的錯誤回應

```java
@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/reminder/{id}")
    public ResponseEntity<ReminderDto> getReminder(@PathVariable Long id) {
        try {
            Reminder reminder = reminderService.getReminder(id);
            return ResponseEntity.ok(toDto(reminder));

        } catch (BusinessException e) {
            // 轉換為 ErrorResponse
            ErrorResponse errorResponse = e.toErrorResponse();
            return ResponseEntity
                    .status(e.getErrorCode().getHttpStatus())
                    .body(null); // 或返回 errorResponse
        }
    }
}
```

### 2. 帶 Metadata 的錯誤回應

```java
// 建立錯誤回應
ErrorResponse errorResponse = ErrorResponse.of(
        ErrorCode.REMINDER_NOT_FOUND,
        MDC.get("traceId"),
        Map.of(
                "reminderId", reminderId,
                "roomId", roomId,
                "timestamp", LocalDateTime.now()
        )
);

logger.error("Reminder not found: {}", errorResponse);
```

### 3. 完整的錯誤回應

```java
ErrorResponse errorResponse = ErrorResponse.builder()
        .errorCode(ErrorCode.AI_SERVICE_TIMEOUT.getCode())
        .message(ErrorCode.AI_SERVICE_TIMEOUT.getMessage())
        .detail("AI 服務在 15 秒後超時")
        .traceId(MDC.get("traceId"))
        .metadata(Map.of(
                "model", "llama-3.1-8b-instant",
                "roomId", roomId,
                "timeoutMs", 15000
        ))
        .httpStatus(504)
        .path("/webhook")
        .build();
```

---

## TraceId 追蹤範例

### 1. 在日誌中使用 TraceId

```java
@Service
public class ReminderService {

    private static final Logger logger = LoggerFactory.getLogger(ReminderService.class);

    public void createReminder(Reminder reminder) {
        // TraceId 會自動從 MDC 取得並輸出到日誌
        logger.info("Creating reminder for room: {}", reminder.getRoomId());

        try {
            reminderRepository.save(reminder);
            logger.info("Reminder created successfully: {}", reminder.getId());

        } catch (Exception e) {
            // 日誌中會包含 traceId，方便追蹤
            logger.error("Failed to create reminder: {}", e.getMessage(), e);
            throw e;
        }
    }
}
```

**日誌輸出範例**:
```
2025-10-24 14:30:15.123 [550e8400e29b41d4a716446655440000] [http-nio-5001-exec-1] INFO  c.a.t.n.r.ReminderService - Creating reminder for room: U1234567
2025-10-24 14:30:15.456 [550e8400e29b41d4a716446655440000] [http-nio-5001-exec-1] INFO  c.a.t.n.r.ReminderService - Reminder created successfully: 123
```

### 2. 在程式碼中取得 TraceId

```java
@Service
public class MyService {

    public void someMethod() {
        // 取得當前請求的 traceId
        String traceId = MDC.get("traceId");

        // 在例外中使用
        throw new BusinessException(ErrorCode.REMINDER_NOT_FOUND)
                .withTraceId(traceId)
                .withMetadata("someKey", "someValue");
    }
}
```

### 3. 非同步處理中的 TraceId

```java
@Service
public class AsyncService {

    public void asyncOperation(String roomId) {
        // 在 async 之前取得 traceId
        String traceId = MDC.get("traceId");

        CompletableFuture.runAsync(() -> {
            // 在 async 區塊中設定 traceId
            MDC.put("traceId", traceId);

            try {
                // 執行非同步操作
                logger.info("Async operation for room: {}", roomId);
                // ... 業務邏輯

            } finally {
                // 清理 MDC
                MDC.remove("traceId");
            }
        });
    }
}
```

---

## 重構現有程式碼範例

### Before（重構前）

```java
@Service
public class ReminderFacadeImpl implements ReminderFacade {

    public Message deleteReminder(Long reminderId, String roomId) {
        try {
            boolean success = reminderService.deleteReminder(reminderId, roomId);
            if (success) {
                logger.info("Deleted reminder {} for room: {}", reminderId, roomId);
                return messageTemplateProvider.success("提醒已刪除");
            } else {
                logger.warn("Failed to delete reminder {} for room: {}", reminderId, roomId);
                return messageTemplateProvider.error("刪除失敗");
            }
        } catch (Exception e) {
            logger.error("Delete reminder error: {}", e.getMessage(), e);
            return messageTemplateProvider.error("刪除提醒時發生錯誤");
        }
    }
}
```

### After（重構後）

```java
@Service
public class ReminderFacadeImpl implements ReminderFacade {

    public Message deleteReminder(Long reminderId, String roomId) {
        try {
            // Service 層拋出 BusinessException
            reminderService.deleteReminder(reminderId, roomId);

            logger.info("Deleted reminder {} for room: {}", reminderId, roomId);
            return messageTemplateProvider.success("提醒已刪除");

        } catch (BusinessException e) {
            // 業務例外：返回使用者友善訊息
            logger.warn("Business error [errorCode={}]: {}",
                    e.getErrorCodeString(), e.getMessage());
            return messageTemplateProvider.error(e.getErrorCode().getMessage());

        } catch (Exception e) {
            // 系統例外：記錄完整堆疊
            logger.error("System error: {}", e.getMessage(), e);
            return messageTemplateProvider.error("刪除提醒時發生錯誤");
        }
    }
}
```

---

## 最佳實踐

### 1. 選擇正確的例外類別

```java
// ✅ 正確：使用者輸入錯誤
throw new BusinessException(ErrorCode.INVALID_TIME_FORMAT);

// ✅ 正確：系統內部錯誤
throw new SystemException(ErrorCode.DATABASE_ERROR, cause);

// ✅ 正確：第三方服務錯誤
throw new ExternalServiceException(ErrorCode.AI_SERVICE_TIMEOUT);

// ❌ 錯誤：使用泛型 Exception
throw new Exception("Something went wrong");
```

### 2. 添加有用的 Metadata

```java
// ✅ 正確：包含關鍵資訊
throw BusinessException.reminderNotFound(reminderId, roomId)
        .withMetadata("userId", userId)
        .withMetadata("timestamp", LocalDateTime.now());

// ⚠️ 一般：缺少上下文
throw new BusinessException(ErrorCode.REMINDER_NOT_FOUND);
```

### 3. 記錄適當的日誌級別

```java
// ✅ 正確：業務例外用 WARN
catch (BusinessException e) {
    logger.warn("Business error [errorCode={}]: {}", e.getErrorCodeString(), e.getMessage());
}

// ✅ 正確：系統例外用 ERROR + 堆疊
catch (SystemException e) {
    logger.error("System error [errorCode={}]: {}", e.getErrorCodeString(), e.getMessage(), e);
}

// ✅ 正確：第三方例外用 ERROR
catch (ExternalServiceException e) {
    logger.error("External service error [errorCode={}]: {}", e.getErrorCodeString(), e.getMessage(), e);
}
```

### 4. Facade 層統一處理例外

```java
@Service
public class MyFacadeImpl implements MyFacade {

    public Message handleOperation(String roomId, String input) {
        try {
            // 業務邏輯
            return doSomething(roomId, input);

        } catch (BusinessException e) {
            // 返回使用者友善訊息
            logger.warn("Business error: {}", e.getMessage());
            return messageTemplateProvider.error(e.getErrorCode().getMessage());

        } catch (ExternalServiceException e) {
            // 第三方服務失敗，提供 fallback
            logger.error("External service error: {}", e.getMessage(), e);
            return messageTemplateProvider.error("服務暫時無法使用，請稍後再試");

        } catch (Exception e) {
            // 未預期的錯誤
            logger.error("Unexpected error: {}", e.getMessage(), e);
            return messageTemplateProvider.error("系統錯誤，請稍後再試");
        }
    }
}
```

### 5. Service 層拋出明確例外

```java
@Service
public class MyServiceImpl implements MyService {

    public Reminder getReminder(Long reminderId, String roomId) {
        // 不要返回 null，直接拋出例外
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> BusinessException.reminderNotFound(reminderId, roomId));

        // 驗證權限
        if (!reminder.getRoomId().equals(roomId)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED)
                    .withMetadata("reminderId", reminderId)
                    .withMetadata("roomId", roomId);
        }

        return reminder;
    }
}
```

---

## 完整範例：整合所有機制

```java
@Service
@RequiredArgsConstructor
public class ReminderFacadeImpl implements ReminderFacade {

    private static final Logger logger = LoggerFactory.getLogger(ReminderFacadeImpl.class);

    private final ReminderService reminderService;
    private final MessageTemplateProvider messageTemplateProvider;

    @Override
    public Message deleteReminder(Long reminderId, String roomId) {
        // 1. TraceId 會自動從 MDC 取得
        logger.info("Deleting reminder [reminderId={}, roomId={}]", reminderId, roomId);

        try {
            // 2. Service 層拋出明確的 BusinessException
            reminderService.deleteReminder(reminderId, roomId);

            // 3. 成功日誌
            logger.info("Reminder deleted successfully [reminderId={}]", reminderId);
            return messageTemplateProvider.success("提醒已刪除");

        } catch (BusinessException e) {
            // 4. 業務例外：使用者友善訊息
            logger.warn("Failed to delete reminder [errorCode={}, reminderId={}, roomId={}]: {}",
                    e.getErrorCodeString(), reminderId, roomId, e.getMessage());

            // 5. 返回錯誤訊息給使用者
            return messageTemplateProvider.error(e.getErrorCode().getMessage());

        } catch (ExternalServiceException e) {
            // 6. 第三方服務例外
            logger.error("External service error while deleting reminder [errorCode={}, reminderId={}]: {}",
                    e.getErrorCodeString(), reminderId, e.getMessage(), e);

            return messageTemplateProvider.error("服務暫時無法使用，請稍後再試");

        } catch (Exception e) {
            // 7. 未預期的錯誤
            logger.error("Unexpected error while deleting reminder [reminderId={}, roomId={}]: {}",
                    reminderId, roomId, e.getMessage(), e);

            // 8. GlobalExceptionHandler 會捕獲並記錄 traceId
            throw SystemException.internalError("deleteReminder", e)
                    .withMetadata("reminderId", reminderId)
                    .withMetadata("roomId", roomId);
        }
    }
}
```

**對應的日誌輸出**:
```
2025-10-24 14:30:15.123 [550e8400e29b41d4a716446655440000] [http-nio-5001-exec-1] INFO  c.a.t.n.f.ReminderFacadeImpl - Deleting reminder [reminderId=123, roomId=U1234567]
2025-10-24 14:30:15.456 [550e8400e29b41d4a716446655440000] [http-nio-5001-exec-1] WARN  c.a.t.n.f.ReminderFacadeImpl - Failed to delete reminder [errorCode=REM_001, reminderId=123, roomId=U1234567]: 找不到該提醒
```

---

## 總結

統一錯誤處理機制的核心優勢：

1. **可追蹤性**: TraceId 貫穿整個請求生命週期
2. **一致性**: 所有錯誤使用相同的格式和分類
3. **可維護性**: 錯誤碼集中管理，易於修改
4. **可觀察性**: 結構化日誌，方便監控和除錯
5. **使用者體驗**: 業務例外返回友善訊息，不暴露技術細節

遵循這些最佳實踐，可以大幅提升系統的可靠性和可維護性。
