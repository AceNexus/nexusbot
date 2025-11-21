package com.acenexus.tata.nexusbot.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 全域例外處理器（簡化版）
 * 設計原則:
 * - 捕獲所有未處理的例外，防止應用程式崩潰
 * - 對 LINE webhook 永遠返回 HTTP 200，避免重試循環
 * - 記錄結構化錯誤資訊（errorCode, traceId, path）
 * - 簡化日誌格式，提高可讀性
 * 處理的異常類型:
 * 1. 自定義異常（NexusException）
 * 2. Spring 驗證異常（MethodArgumentNotValidException, ConstraintViolationException）
 * 3. Spring 參數異常（MissingServletRequestParameterException, MethodArgumentTypeMismatchException）
 * 4. JSON 解析異常（HttpMessageNotReadableException）
 * 5. 不支援事件異常（UnsupportedEventException）
 * 6. 其他未捕獲異常（Exception）
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 處理 NexusException（所有業務異常）
     */
    @ExceptionHandler(NexusException.class)
    public ResponseEntity<String> handleNexusException(NexusException e, WebRequest request) {
        String traceId = e.getTraceId() != null ? e.getTraceId() : getTraceId();
        String path = getRequestPath(request);

        // 根據錯誤碼決定日誌級別
        if (isHighSeverityError(e.getErrorCode())) {
            logger.error("[{}] [{}] [{}] {}",
                    e.getErrorCode().getCode(),
                    traceId,
                    path,
                    e.getMessage(),
                    e);
        } else {
            logger.warn("[{}] [{}] [{}] {}",
                    e.getErrorCode().getCode(),
                    traceId,
                    path,
                    e.getMessage());
        }

        // 對 LINE webhook 返回 HTTP 200
        return ResponseEntity.ok("OK");
    }

    /**
     * 處理 Spring 驗證異常（@Valid 註解觸發）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(MethodArgumentNotValidException e, WebRequest request) {
        String traceId = getTraceId();
        String path = getRequestPath(request);

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        logger.warn("[VALIDATION_ERROR] [{}] [{}] Fields: {}", traceId, path, errors);
        return ResponseEntity.ok("OK");
    }

    /**
     * 處理約束違反異常（@Validated 註解觸發）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleConstraintViolationException(ConstraintViolationException e, WebRequest request) {
        String traceId = getTraceId();
        String path = getRequestPath(request);

        Set<String> violations = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        logger.warn("[VALIDATION_ERROR] [{}] [{}] Violations: {}", traceId, path, violations);
        return ResponseEntity.ok("OK");
    }

    /**
     * 處理缺少必填參數異常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<String> handleMissingParameter(MissingServletRequestParameterException e, WebRequest request) {
        String traceId = getTraceId();
        String path = getRequestPath(request);

        logger.warn("[VALIDATION_ERROR] [{}] [{}] Missing parameter: {} (type: {})",
                traceId, path, e.getParameterName(), e.getParameterType());
        return ResponseEntity.ok("OK");
    }

    /**
     * 處理參數類型不匹配異常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleTypeMismatch(MethodArgumentTypeMismatchException e, WebRequest request) {
        String traceId = getTraceId();
        String path = getRequestPath(request);

        logger.warn("[VALIDATION_ERROR] [{}] [{}] Type mismatch: {} (expected: {}, got: {})",
                traceId, path, e.getName(),
                e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown",
                e.getValue());
        return ResponseEntity.ok("OK");
    }

    /**
     * 處理 JSON 解析異常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleHttpMessageNotReadable(HttpMessageNotReadableException e, WebRequest request) {
        String traceId = getTraceId();
        String path = getRequestPath(request);

        String detail = "Invalid JSON format";
        if (e.getCause() instanceof InvalidFormatException) {
            InvalidFormatException ife = (InvalidFormatException) e.getCause();
            detail = String.format("Invalid value for field '%s': %s",
                    ife.getPath().isEmpty() ? "unknown" : ife.getPath().get(0).getFieldName(),
                    ife.getValue());
        }

        logger.warn("[VALIDATION_ERROR] [{}] [{}] JSON parse error: {}", traceId, path, detail);
        return ResponseEntity.ok("OK");
    }

    /**
     * 處理不支援的事件異常
     */
    @ExceptionHandler(UnsupportedEventException.class)
    public ResponseEntity<String> handleUnsupportedEventException(UnsupportedEventException e, WebRequest request) {
        String traceId = getTraceId();
        String path = getRequestPath(request);

        logger.warn("[UNSUPPORTED_EVENT] [{}] [{}] {}", traceId, path, e.getMessage());
        return ResponseEntity.ok("OK");
    }

    /**
     * 處理所有未捕獲的例外（兜底處理）
     * <p>
     * 注意: 對於 LINE webhook，必須返回 HTTP 200，否則 LINE 會重試
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e, WebRequest request) {
        String traceId = getTraceId();
        String path = getRequestPath(request);

        logger.error("[UNHANDLED_ERROR] [{}] [{}] [{}] {}",
                e.getClass().getSimpleName(),
                traceId,
                path,
                e.getMessage(),
                e);

        // 對 LINE webhook 返回 HTTP 200
        return ResponseEntity.ok("OK");
    }

    /**
     * 判斷是否為高嚴重度錯誤（需要記錄完整堆疊）
     */
    private boolean isHighSeverityError(ErrorCode errorCode) {
        String code = errorCode.getCode();
        // 系統錯誤、LINE API 錯誤、認證錯誤視為高嚴重度
        return code.startsWith("SYS_")
                || code.startsWith("LINE_")
                || code.startsWith("AUTH_")
                || code.equals("AI_NOT_CONFIGURED");
    }

    /**
     * 取得 TraceId
     * 優先從 MDC 取得，如果沒有則生成新的
     */
    private String getTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString().replace("-", "");
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
}
