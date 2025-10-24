package com.acenexus.tata.nexusbot.exception;

/**
 * 業務例外類別
 *
 * 用途:
 * - 使用者操作錯誤（如：提醒不存在、時間格式錯誤）
 * - 業務規則驗證失敗（如：提醒時間必須是未來）
 * - 資料驗證失敗（如：必填欄位缺失）
 *
 * HTTP 狀態碼: 通常為 400 (Bad Request)
 *
 * 使用範例:
 * <pre>
 * // 簡單用法
 * throw new BusinessException(ErrorCode.REMINDER_NOT_FOUND);
 *
 * // 帶詳細訊息
 * throw new BusinessException(ErrorCode.INVALID_TIME_FORMAT,
 *     "無法解析時間: " + input);
 *
 * // 帶 metadata
 * throw new BusinessException(ErrorCode.REMINDER_NOT_FOUND)
 *     .withMetadata("reminderId", reminderId)
 *     .withMetadata("roomId", roomId);
 *
 * // 鏈式呼叫
 * throw new BusinessException(ErrorCode.PAST_TIME_NOT_ALLOWED)
 *     .withTraceId(traceId)
 *     .withMetadata("inputTime", reminderTime)
 *     .withMetadata("currentTime", LocalDateTime.now());
 * </pre>
 */
public class BusinessException extends BaseException {

    /**
     * 基本建構子
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 帶詳細訊息的建構子
     */
    public BusinessException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    /**
     * 帶 cause 的建構子
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 帶詳細訊息和 cause 的建構子
     */
    public BusinessException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }

    /**
     * 建立提醒不存在例外
     */
    public static BusinessException reminderNotFound(Long reminderId, String roomId) {
        return (BusinessException) new BusinessException(ErrorCode.REMINDER_NOT_FOUND)
                .withMetadata("reminderId", reminderId)
                .withMetadata("roomId", roomId);
    }

    /**
     * 建立時間格式錯誤例外
     */
    public static BusinessException invalidTimeFormat(String input) {
        return (BusinessException) new BusinessException(ErrorCode.INVALID_TIME_FORMAT,
                "無法解析時間: " + input)
                .withMetadata("input", input);
    }

    /**
     * 建立過去時間不允許例外
     */
    public static BusinessException pastTimeNotAllowed(String inputTime) {
        return (BusinessException) new BusinessException(ErrorCode.PAST_TIME_NOT_ALLOWED,
                "提醒時間必須是未來: " + inputTime)
                .withMetadata("inputTime", inputTime);
    }

    /**
     * 建立提醒內容為空例外
     */
    public static BusinessException emptyReminderContent(String roomId) {
        return (BusinessException) new BusinessException(ErrorCode.EMPTY_REMINDER_CONTENT)
                .withMetadata("roomId", roomId);
    }

    /**
     * 建立 Email 格式錯誤例外
     */
    public static BusinessException invalidEmailFormat(String email) {
        return (BusinessException) new BusinessException(ErrorCode.INVALID_EMAIL_FORMAT,
                "Email 格式錯誤: " + email)
                .withMetadata("email", email);
    }

    /**
     * 建立參數驗證失敗例外
     */
    public static BusinessException validationError(String fieldName, String message) {
        return (BusinessException) new BusinessException(ErrorCode.VALIDATION_ERROR, message)
                .withMetadata("field", fieldName);
    }
}
