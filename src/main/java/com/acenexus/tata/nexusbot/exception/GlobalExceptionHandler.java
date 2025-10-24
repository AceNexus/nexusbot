package com.acenexus.tata.nexusbot.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 全域例外處理器
 *
 * 設計原則:
 * - 捕獲所有未處理的例外，防止應用程式崩潰
 * - 對 LINE webhook 永遠返回 HTTP 200，避免重試循環
 * - 記錄結構化錯誤資訊（errorCode, traceId, 堆疊追蹤）
 * - 內部使用 ErrorResponse 進行錯誤分類和追蹤
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 處理業務例外（400 類錯誤）
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<String> handleBusinessException(BusinessException e, WebRequest request) {
        String traceId = getOrSetTraceId(e);
        String path = getRequestPath(request);

        logger.warn("Business exception [traceId={}] [path={}] [errorCode={}]: {}",
                traceId, path, e.getErrorCodeString(), e.getMessage());

        // 對 LINE webhook 返回 HTTP 200
        return ResponseEntity.ok("OK");
    }

    /**
     * 處理系統例外（500 類錯誤）
     */
    @ExceptionHandler(SystemException.class)
    public ResponseEntity<String> handleSystemException(SystemException e, WebRequest request) {
        String traceId = getOrSetTraceId(e);
        String path = getRequestPath(request);

        logger.error("System exception [traceId={}] [path={}] [errorCode={}]: {}",
                traceId, path, e.getErrorCodeString(), e.getMessage(), e);

        // 對 LINE webhook 返回 HTTP 200
        return ResponseEntity.ok("OK");
    }

    /**
     * 處理第三方服務例外（502/504 類錯誤）
     */
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<String> handleExternalServiceException(ExternalServiceException e, WebRequest request) {
        String traceId = getOrSetTraceId(e);
        String path = getRequestPath(request);

        logger.error("External service exception [traceId={}] [path={}] [errorCode={}]: {}",
                traceId, path, e.getErrorCodeString(), e.getMessage(), e);

        // 對 LINE webhook 返回 HTTP 200
        return ResponseEntity.ok("OK");
    }

    /**
     * 處理所有未捕獲的例外（兜底處理）
     *
     * 注意: 對於 LINE webhook，必須返回 HTTP 200，否則 LINE 會重試
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e, WebRequest request) {
        // 取得或生成 traceId
        String traceId = getTraceId();

        // 取得請求路徑
        String path = getRequestPath(request);

        // 建立結構化錯誤回應（用於日誌）
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .message(ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                .detail(e.getMessage())
                .traceId(traceId)
                .path(path)
                .httpStatus(500)
                .build();

        // 記錄詳細錯誤資訊
        logger.error("Unhandled exception [traceId={}] [path={}] [errorCode={}]: {}",
                traceId, path, errorResponse.getErrorCode(), e.getMessage(), e);

        // 對 LINE webhook 返回 HTTP 200
        return ResponseEntity.ok("OK");
    }

    /**
     * 取得或設定 BaseException 的 TraceId
     */
    private String getOrSetTraceId(BaseException e) {
        String traceId = e.getTraceId();
        if (traceId == null) {
            traceId = getTraceId();
            e.withTraceId(traceId);
        }
        return traceId;
    }

    /**
     * 取得 TraceId
     * 優先從 MDC 取得，如果沒有則生成新的
     */
    private String getTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            MDC.put("traceId", traceId);
        }
        return traceId;
    }

    /**
     * 取得請求路徑
     */
    private String getRequestPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            return ((ServletWebRequest) request).getRequest().getRequestURI();
        }
        return "unknown";
    }

    /**
     * 建立錯誤 metadata
     */
    private Map<String, Object> buildMetadata(Exception e, String path) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("exceptionType", e.getClass().getName());
        metadata.put("path", path);
        metadata.put("timestamp", System.currentTimeMillis());
        return metadata;
    }
}