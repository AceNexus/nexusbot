package com.acenexus.tata.nexusbot.exception;

/**
 * 第三方服務例外類別
 *
 * 用途:
 * - 外部 API 呼叫失敗（如：Groq AI API、LINE API）
 * - 服務超時（如：AI 服務 15 秒超時）
 * - 第三方服務不可用
 * - 回應解析失敗
 *
 * HTTP 狀態碼: 通常為 502 (Bad Gateway) 或 504 (Gateway Timeout)
 *
 * 使用範例:
 * <pre>
 * // AI 服務超時
 * throw new ExternalServiceException(ErrorCode.AI_SERVICE_TIMEOUT)
 *     .withMetadata("model", selectedModel)
 *     .withMetadata("timeoutSeconds", 15);
 *
 * // LINE API 錯誤
 * throw new ExternalServiceException(ErrorCode.LINE_API_ERROR, cause)
 *     .withMetadata("operation", "sendPushMessage")
 *     .withMetadata("roomId", roomId);
 *
 * // Email 發送失敗
 * throw ExternalServiceException.emailSendFailed(email, cause);
 * </pre>
 */
public class ExternalServiceException extends BaseException {

    /**
     * 基本建構子
     */
    public ExternalServiceException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 帶詳細訊息的建構子
     */
    public ExternalServiceException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    /**
     * 帶 cause 的建構子
     */
    public ExternalServiceException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 帶詳細訊息和 cause 的建構子
     */
    public ExternalServiceException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }

    /**
     * 建立 AI 服務超時例外
     */
    public static ExternalServiceException aiServiceTimeout(String model, long timeoutMs) {
        return (ExternalServiceException) new ExternalServiceException(ErrorCode.AI_SERVICE_TIMEOUT,
                "AI 服務超時: " + model + " (" + timeoutMs + "ms)")
                .withMetadata("model", model)
                .withMetadata("timeoutMs", timeoutMs);
    }

    /**
     * 建立 AI API 錯誤例外
     */
    public static ExternalServiceException aiApiError(String model, Throwable cause) {
        return (ExternalServiceException) new ExternalServiceException(ErrorCode.AI_API_ERROR,
                "AI API 呼叫失敗: " + model, cause)
                .withMetadata("model", model);
    }

    /**
     * 建立 LINE API 錯誤例外
     */
    public static ExternalServiceException lineApiError(String operation, Throwable cause) {
        return (ExternalServiceException) new ExternalServiceException(ErrorCode.LINE_API_ERROR,
                "LINE API 錯誤: " + operation, cause)
                .withMetadata("operation", operation);
    }

    /**
     * 建立 LINE 訊息發送失敗例外
     */
    public static ExternalServiceException lineMessageSendFailed(String roomId, Throwable cause) {
        return (ExternalServiceException) new ExternalServiceException(ErrorCode.LINE_MESSAGE_SEND_FAILED,
                "LINE 訊息發送失敗", cause)
                .withMetadata("roomId", roomId);
    }

    /**
     * 建立 Email 發送失敗例外
     */
    public static ExternalServiceException emailSendFailed(String email, Throwable cause) {
        return (ExternalServiceException) new ExternalServiceException(ErrorCode.EMAIL_SEND_FAILED,
                "Email 發送失敗: " + email, cause)
                .withMetadata("email", email);
    }

    /**
     * 建立 SMTP 服務錯誤例外
     */
    public static ExternalServiceException smtpServiceError(String detail, Throwable cause) {
        return (ExternalServiceException) new ExternalServiceException(ErrorCode.SMTP_SERVICE_ERROR,
                detail, cause);
    }

    /**
     * 建立位置服務錯誤例外
     */
    public static ExternalServiceException locationServiceError(double latitude, double longitude, Throwable cause) {
        return (ExternalServiceException) new ExternalServiceException(ErrorCode.LOCATION_SERVICE_ERROR,
                "位置服務錯誤", cause)
                .withMetadata("latitude", latitude)
                .withMetadata("longitude", longitude);
    }
}
