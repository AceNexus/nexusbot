package com.acenexus.tata.nexusbot.exception;

/**
 * 系統例外類別
 *
 * 用途:
 * - 系統內部錯誤（如：資料庫連線失敗）
 * - 配置錯誤（如：缺少必要配置）
 * - 資源不足（如：記憶體不足、磁碟空間不足）
 * - 非預期的系統狀態
 *
 * HTTP 狀態碼: 通常為 500 (Internal Server Error)
 *
 * 使用範例:
 * <pre>
 * // 資料庫錯誤
 * throw new SystemException(ErrorCode.DATABASE_ERROR, e);
 *
 * // 配置錯誤
 * throw new SystemException(ErrorCode.CONFIGURATION_ERROR,
 *     "Missing required config: LINE_CHANNEL_TOKEN");
 *
 * // 帶 metadata
 * throw new SystemException(ErrorCode.INTERNAL_SERVER_ERROR)
 *     .withMetadata("operation", "processReminder")
 *     .withMetadata("reminderId", reminderId);
 * </pre>
 */
public class SystemException extends BaseException {

    /**
     * 基本建構子
     */
    public SystemException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 帶詳細訊息的建構子
     */
    public SystemException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    /**
     * 帶 cause 的建構子
     */
    public SystemException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 帶詳細訊息和 cause 的建構子
     */
    public SystemException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }

    /**
     * 建立資料庫錯誤例外
     */
    public static SystemException databaseError(String operation, Throwable cause) {
        return (SystemException) new SystemException(ErrorCode.DATABASE_ERROR,
                "資料庫操作失敗: " + operation, cause)
                .withMetadata("operation", operation);
    }

    /**
     * 建立配置錯誤例外
     */
    public static SystemException configurationError(String configName) {
        return (SystemException) new SystemException(ErrorCode.CONFIGURATION_ERROR,
                "缺少必要配置: " + configName)
                .withMetadata("configName", configName);
    }

    /**
     * 建立內部伺服器錯誤例外
     */
    public static SystemException internalError(String operation, Throwable cause) {
        return (SystemException) new SystemException(ErrorCode.INTERNAL_SERVER_ERROR,
                "內部錯誤: " + operation, cause)
                .withMetadata("operation", operation);
    }

    /**
     * 建立未知錯誤例外
     */
    public static SystemException unknownError(String message, Throwable cause) {
        return (SystemException) new SystemException(ErrorCode.UNKNOWN_ERROR, message, cause);
    }
}
