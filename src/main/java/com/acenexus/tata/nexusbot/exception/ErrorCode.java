package com.acenexus.tata.nexusbot.exception;

import lombok.Getter;

/**
 * 統一錯誤碼
 * 設計原則：
 * - 簡潔設計，只包含必要資訊（code, message）
 * - 錯誤碼命名規範：{模組}_{編號}
 * - SYS_xxx: 系統錯誤
 * - REM_xxx: 提醒相關
 * - AI_xxx: AI 服務
 * - LINE_xxx: LINE API
 * - EMAIL_xxx: Email 服務
 * - VAL_xxx: 驗證錯誤
 * - AUTH_xxx: 認證錯誤
 * - LOC_xxx: 位置服務
 * - LOCK_xxx: 分散式鎖
 * - ROOM_xxx: 聊天室
 */
@Getter
public enum ErrorCode {

    // ========== 系統錯誤 ==========
    INTERNAL_SERVER_ERROR("SYS_001", "系統暫時無法處理您的請求，請稍後再試"),
    DATABASE_ERROR("SYS_002", "資料庫操作失敗"),
    CONFIGURATION_ERROR("SYS_003", "系統配置錯誤"),
    UNKNOWN_ERROR("SYS_999", "發生未預期的錯誤"),

    // ========== 提醒相關錯誤 ==========
    REMINDER_NOT_FOUND("REM_001", "找不到該提醒"),
    INVALID_TIME_FORMAT("REM_002", "時間格式錯誤，請使用「yyyy-MM-dd HH:mm」格式或自然語言"),
    PAST_TIME_NOT_ALLOWED("REM_003", "提醒時間必須是未來時間"),
    EMPTY_REMINDER_CONTENT("REM_004", "提醒內容不能為空"),
    REMINDER_CREATION_FAILED("REM_005", "提醒創建失敗"),
    REMINDER_DELETION_FAILED("REM_006", "提醒刪除失敗"),
    INVALID_REMINDER_STATE("REM_007", "提醒狀態無效"),
    REMINDER_STATE_EXPIRED("REM_008", "提醒流程已過期，請重新開始"),

    // ========== AI 服務錯誤 ==========
    AI_SERVICE_TIMEOUT("AI_001", "AI 服務回應超時"),
    AI_API_ERROR("AI_002", "AI 服務暫時無法使用"),
    AI_RESPONSE_PARSE_ERROR("AI_003", "AI 回應格式錯誤"),
    AI_NOT_CONFIGURED("AI_004", "AI 服務未配置"),

    // ========== LINE API 錯誤 ==========
    LINE_API_ERROR("LINE_001", "LINE 服務暫時無法使用"),
    LINE_MESSAGE_SEND_FAILED("LINE_002", "訊息發送失敗"),
    LINE_INVALID_TOKEN("LINE_003", "LINE 驗證失敗"),

    // ========== Email 服務錯誤 ==========
    EMAIL_SEND_FAILED("EMAIL_001", "Email 發送失敗"),
    INVALID_EMAIL_FORMAT("EMAIL_002", "Email 格式錯誤"),
    EMAIL_NOT_FOUND("EMAIL_003", "找不到該 Email"),
    EMAIL_ALREADY_EXISTS("EMAIL_004", "Email 已綁定"),
    SMTP_SERVICE_ERROR("EMAIL_005", "郵件服務暫時無法使用"),

    // ========== 驗證錯誤 ==========
    VALIDATION_ERROR("VAL_001", "參數驗證失敗"),
    REQUIRED_FIELD_MISSING("VAL_002", "必填欄位缺失"),
    INVALID_PARAMETER_FORMAT("VAL_003", "參數格式錯誤"),

    // ========== 認證錯誤 ==========
    AUTHENTICATION_FAILED("AUTH_001", "認證失敗"),
    PERMISSION_DENIED("AUTH_002", "權限不足"),
    TOKEN_EXPIRED("AUTH_003", "Token 已過期"),

    // ========== 位置服務錯誤 ==========
    LOCATION_SERVICE_ERROR("LOC_001", "位置服務暫時無法使用"),
    NO_NEARBY_FACILITIES("LOC_002", "附近找不到相關設施"),

    // ========== 分散式鎖錯誤 ==========
    LOCK_ACQUISITION_FAILED("LOCK_001", "操作正在進行中，請稍後再試"),
    LOCK_RELEASE_FAILED("LOCK_002", "鎖釋放失敗"),

    // ========== 聊天室錯誤 ==========
    CHATROOM_NOT_FOUND("ROOM_001", "找不到該聊天室"),
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
}
