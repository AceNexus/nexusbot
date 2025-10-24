package com.acenexus.tata.nexusbot.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 統一錯誤回應格式
 * 用於內部服務間的錯誤通訊，不直接暴露給 LINE 使用者
 *
 * 設計原則:
 * - 包含錯誤碼、訊息、追蹤 ID 等資訊
 * - 支援微服務架構的錯誤追蹤
 * - 可擴展的 metadata 欄位
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * 錯誤碼（如 "SYS_001", "REM_001"）
     * 用於錯誤分類和統計
     */
    private String errorCode;

    /**
     * 使用者友善的錯誤訊息
     * 中文說明，適合顯示給使用者
     */
    private String message;

    /**
     * 詳細錯誤訊息（可選）
     * 技術細節，用於除錯
     */
    private String detail;

    /**
     * 請求追蹤 ID
     * 用於跨服務追蹤和日誌關聯
     */
    private String traceId;

    /**
     * 錯誤發生時間
     */
    private LocalDateTime timestamp;

    /**
     * 額外的錯誤資訊（可選）
     * 如: {"reminderId": 123, "roomId": "U123456"}
     */
    private Map<String, Object> metadata;

    /**
     * HTTP 狀態碼（可選）
     * 用於 REST API 回應
     */
    private Integer httpStatus;

    /**
     * 請求路徑（可選）
     */
    private String path;

    /**
     * 建立簡單的錯誤回應
     */
    public static ErrorResponse of(ErrorCode errorCode, String traceId) {
        return ErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .message(errorCode.getMessage())
                .traceId(traceId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 建立帶詳細訊息的錯誤回應
     */
    public static ErrorResponse of(ErrorCode errorCode, String traceId, String detail) {
        return ErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .message(errorCode.getMessage())
                .detail(detail)
                .traceId(traceId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 建立帶 metadata 的錯誤回應
     */
    public static ErrorResponse of(ErrorCode errorCode, String traceId, Map<String, Object> metadata) {
        return ErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .message(errorCode.getMessage())
                .traceId(traceId)
                .metadata(metadata)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 建立完整的錯誤回應
     */
    public static ErrorResponse of(ErrorCode errorCode, String traceId, String detail,
                                   Map<String, Object> metadata, Integer httpStatus, String path) {
        return ErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .message(errorCode.getMessage())
                .detail(detail)
                .traceId(traceId)
                .metadata(metadata)
                .httpStatus(httpStatus)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
