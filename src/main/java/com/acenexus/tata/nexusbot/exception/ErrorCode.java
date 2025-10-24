package com.acenexus.tata.nexusbot.exception;

import lombok.Getter;

/**
 * 統一錯誤碼管理
 *
 * 錯誤碼規範:
 * - SYS_xxx: 系統錯誤 (500 類)
 * - REM_xxx: 提醒相關錯誤 (400 類)
 * - AI_xxx: AI 服務錯誤 (502/504 類)
 * - LINE_xxx: LINE API 錯誤 (502 類)
 * - EMAIL_xxx: Email 服務錯誤 (502 類)
 * - VAL_xxx: 驗證錯誤 (400 類)
 * - AUTH_xxx: 認證錯誤 (401/403 類)
 */
@Getter
public enum ErrorCode {

    // ========== 系統錯誤 (5xx) ==========

    /**
     * 系統內部錯誤
     */
    INTERNAL_SERVER_ERROR("SYS_001", "系統暫時無法處理您的請求，請稍後再試"),

    /**
     * 資料庫錯誤
     */
    DATABASE_ERROR("SYS_002", "資料庫操作失敗"),

    /**
     * 配置錯誤
     */
    CONFIGURATION_ERROR("SYS_003", "系統配置錯誤"),

    /**
     * 未知錯誤
     */
    UNKNOWN_ERROR("SYS_999", "發生未預期的錯誤"),

    // ========== 提醒相關錯誤 (4xx) ==========

    /**
     * 提醒不存在
     */
    REMINDER_NOT_FOUND("REM_001", "找不到該提醒"),

    /**
     * 時間格式錯誤
     */
    INVALID_TIME_FORMAT("REM_002", "時間格式錯誤，請使用「yyyy-MM-dd HH:mm」格式或自然語言"),

    /**
     * 提醒時間必須是未來
     */
    PAST_TIME_NOT_ALLOWED("REM_003", "提醒時間必須是未來時間"),

    /**
     * 提醒內容為空
     */
    EMPTY_REMINDER_CONTENT("REM_004", "提醒內容不能為空"),

    /**
     * 提醒創建失敗
     */
    REMINDER_CREATION_FAILED("REM_005", "提醒創建失敗"),

    /**
     * 提醒刪除失敗
     */
    REMINDER_DELETION_FAILED("REM_006", "提醒刪除失敗"),

    /**
     * 提醒狀態無效
     */
    INVALID_REMINDER_STATE("REM_007", "提醒狀態無效"),

    /**
     * 提醒已過期
     */
    REMINDER_STATE_EXPIRED("REM_008", "提醒流程已過期，請重新開始"),

    // ========== AI 服務錯誤 (5xx) ==========

    /**
     * AI 服務超時
     */
    AI_SERVICE_TIMEOUT("AI_001", "AI 服務回應超時"),

    /**
     * AI API 呼叫失敗
     */
    AI_API_ERROR("AI_002", "AI 服務暫時無法使用"),

    /**
     * AI 回應解析失敗
     */
    AI_RESPONSE_PARSE_ERROR("AI_003", "AI 回應格式錯誤"),

    /**
     * AI 服務未配置
     */
    AI_NOT_CONFIGURED("AI_004", "AI 服務未配置"),

    // ========== LINE API 錯誤 (5xx) ==========

    /**
     * LINE API 呼叫失敗
     */
    LINE_API_ERROR("LINE_001", "LINE 服務暫時無法使用"),

    /**
     * LINE 訊息發送失敗
     */
    LINE_MESSAGE_SEND_FAILED("LINE_002", "訊息發送失敗"),

    /**
     * LINE Token 無效
     */
    LINE_INVALID_TOKEN("LINE_003", "LINE 驗證失敗"),

    // ========== Email 服務錯誤 (5xx) ==========

    /**
     * Email 發送失敗
     */
    EMAIL_SEND_FAILED("EMAIL_001", "Email 發送失敗"),

    /**
     * Email 格式錯誤
     */
    INVALID_EMAIL_FORMAT("EMAIL_002", "Email 格式錯誤"),

    /**
     * Email 不存在
     */
    EMAIL_NOT_FOUND("EMAIL_003", "找不到該 Email"),

    /**
     * Email 已存在
     */
    EMAIL_ALREADY_EXISTS("EMAIL_004", "Email 已綁定"),

    /**
     * SMTP 服務錯誤
     */
    SMTP_SERVICE_ERROR("EMAIL_005", "郵件服務暫時無法使用"),

    // ========== 驗證錯誤 (4xx) ==========

    /**
     * 參數驗證失敗
     */
    VALIDATION_ERROR("VAL_001", "參數驗證失敗"),

    /**
     * 必填欄位缺失
     */
    REQUIRED_FIELD_MISSING("VAL_002", "必填欄位缺失"),

    /**
     * 參數格式錯誤
     */
    INVALID_PARAMETER_FORMAT("VAL_003", "參數格式錯誤"),

    // ========== 認證錯誤 (4xx) ==========

    /**
     * 認證失敗
     */
    AUTHENTICATION_FAILED("AUTH_001", "認證失敗"),

    /**
     * 權限不足
     */
    PERMISSION_DENIED("AUTH_002", "權限不足"),

    /**
     * Token 過期
     */
    TOKEN_EXPIRED("AUTH_003", "Token 已過期"),

    // ========== 位置服務錯誤 (5xx) ==========

    /**
     * 位置服務 API 錯誤
     */
    LOCATION_SERVICE_ERROR("LOC_001", "位置服務暫時無法使用"),

    /**
     * 找不到附近設施
     */
    NO_NEARBY_FACILITIES("LOC_002", "附近找不到相關設施"),

    // ========== 分散式鎖錯誤 (4xx) ==========

    /**
     * 獲取鎖失敗
     */
    LOCK_ACQUISITION_FAILED("LOCK_001", "操作正在進行中，請稍後再試"),

    /**
     * 釋放鎖失敗
     */
    LOCK_RELEASE_FAILED("LOCK_002", "鎖釋放失敗"),

    // ========== 聊天室錯誤 (4xx) ==========

    /**
     * 聊天室不存在
     */
    CHATROOM_NOT_FOUND("ROOM_001", "找不到該聊天室"),

    /**
     * 聊天室配置錯誤
     */
    CHATROOM_CONFIG_ERROR("ROOM_002", "聊天室配置錯誤");

    /**
     * 錯誤碼（如 "SYS_001"）
     */
    private final String code;

    /**
     * 使用者友善的錯誤訊息
     */
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根據錯誤碼查找 ErrorCode
     */
    public static ErrorCode fromCode(String code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.code.equals(code)) {
                return errorCode;
            }
        }
        return UNKNOWN_ERROR;
    }

    /**
     * 判斷是否為系統錯誤 (5xx)
     */
    public boolean isSystemError() {
        return code.startsWith("SYS_") || code.startsWith("AI_") ||
               code.startsWith("LINE_") || code.startsWith("EMAIL_") ||
               code.startsWith("LOC_") || code.startsWith("SMTP_");
    }

    /**
     * 判斷是否為業務錯誤 (4xx)
     */
    public boolean isBusinessError() {
        return code.startsWith("REM_") || code.startsWith("VAL_") ||
               code.startsWith("AUTH_") || code.startsWith("LOCK_") ||
               code.startsWith("ROOM_");
    }

    /**
     * 取得建議的 HTTP 狀態碼
     */
    public int getHttpStatus() {
        if (isSystemError()) {
            return 500; // Internal Server Error
        } else if (code.startsWith("AUTH_")) {
            return 401; // Unauthorized
        } else if (code.startsWith("VAL_") || code.startsWith("REM_")) {
            return 400; // Bad Request
        } else {
            return 500; // Default to 500
        }
    }
}
