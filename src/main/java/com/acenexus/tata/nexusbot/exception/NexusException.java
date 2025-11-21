package com.acenexus.tata.nexusbot.exception;

import lombok.Getter;
import org.slf4j.MDC;

/**
 * NexusBot 統一業務異常
 * 設計原則：
 * - 單一異常類別，簡化異常體系
 * - 自動從 MDC 讀取 traceId
 * - 提供靜態工廠方法快速建立常見異常
 * 使用範例：
 * // 基本用法
 * throw new NexusException(ErrorCode.REMINDER_NOT_FOUND);
 * // 帶詳細訊息
 * throw new NexusException(ErrorCode.INVALID_TIME_FORMAT, "無法解析: " + input);
 * // 靜態工廠方法
 * throw NexusException.reminderNotFound(reminderId);
 * throw NexusException.aiTimeout();
 * </pre>
 */
@Getter
public class NexusException extends RuntimeException {

    /**
     * 錯誤碼
     */
    private final ErrorCode errorCode;

    /**
     * 追蹤 ID（自動從 MDC 讀取）
     */
    private final String traceId;

    /**
     * 基本建構子
     */
    public NexusException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.traceId = getTraceIdFromMDC();
    }

    /**
     * 帶詳細訊息的建構子
     */
    public NexusException(ErrorCode errorCode, String detail) {
        super(detail);
        this.errorCode = errorCode;
        this.traceId = getTraceIdFromMDC();
    }

    /**
     * 帶 cause 的建構子
     */
    public NexusException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.traceId = getTraceIdFromMDC();
    }

    /**
     * 帶詳細訊息和 cause 的建構子
     */
    public NexusException(ErrorCode errorCode, String detail, Throwable cause) {
        super(detail, cause);
        this.errorCode = errorCode;
        this.traceId = getTraceIdFromMDC();
    }

    /**
     * 從 MDC 取得 traceId
     */
    private static String getTraceIdFromMDC() {
        return MDC.get("traceId");
    }
}
