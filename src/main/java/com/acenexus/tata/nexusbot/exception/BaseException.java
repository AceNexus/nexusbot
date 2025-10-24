package com.acenexus.tata.nexusbot.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 自訂例外基礎類別
 *
 * 設計原則:
 * - 所有自訂例外都應繼承此類別
 * - 包含錯誤碼、追蹤 ID、metadata 等資訊
 * - 支援鏈式呼叫和 cause 追蹤
 */
@Getter
public abstract class BaseException extends RuntimeException {

    /**
     * 錯誤碼
     */
    private final ErrorCode errorCode;

    /**
     * 追蹤 ID
     */
    private String traceId;

    /**
     * 額外的錯誤資訊
     */
    private Map<String, Object> metadata;

    /**
     * 基本建構子
     */
    protected BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.metadata = new HashMap<>();
    }

    /**
     * 帶詳細訊息的建構子
     */
    protected BaseException(ErrorCode errorCode, String detail) {
        super(detail != null ? detail : errorCode.getMessage());
        this.errorCode = errorCode;
        this.metadata = new HashMap<>();
    }

    /**
     * 帶 cause 的建構子
     */
    protected BaseException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.metadata = new HashMap<>();
    }

    /**
     * 帶詳細訊息和 cause 的建構子
     */
    protected BaseException(ErrorCode errorCode, String detail, Throwable cause) {
        super(detail != null ? detail : errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.metadata = new HashMap<>();
    }

    /**
     * 設定追蹤 ID
     */
    public BaseException withTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    /**
     * 添加 metadata
     */
    public BaseException withMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    /**
     * 批量設定 metadata
     */
    public BaseException withMetadata(Map<String, Object> metadata) {
        if (metadata != null) {
            this.metadata.putAll(metadata);
        }
        return this;
    }

    /**
     * 轉換為 ErrorResponse
     */
    public ErrorResponse toErrorResponse() {
        return ErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .message(errorCode.getMessage())
                .detail(getMessage())
                .traceId(traceId)
                .metadata(metadata)
                .httpStatus(errorCode.getHttpStatus())
                .build();
    }

    /**
     * 取得錯誤碼字串
     */
    public String getErrorCodeString() {
        return errorCode.getCode();
    }

    @Override
    public String toString() {
        return String.format("%s [errorCode=%s, traceId=%s, message=%s]",
                getClass().getSimpleName(),
                errorCode.getCode(),
                traceId,
                getMessage());
    }
}
