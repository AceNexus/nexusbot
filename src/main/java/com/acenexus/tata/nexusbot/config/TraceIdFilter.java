package com.acenexus.tata.nexusbot.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * TraceId 追蹤 Filter
 *
 * 功能:
 * - 為每個 HTTP 請求自動生成或傳遞 traceId
 * - 將 traceId 存入 MDC (Mapped Diagnostic Context)
 * - 支援分散式追蹤（從 Header 讀取 X-Trace-Id）
 * - 自動清理 MDC 避免記憶體洩漏
 *
 * 使用方式:
 * - 在日誌中使用 %X{traceId} 輸出 traceId
 * - 在程式碼中使用 MDC.get("traceId") 取得當前 traceId
 *
 * 設計原則:
 * - 最高優先順序執行（@Order(Ordered.HIGHEST_PRECEDENCE)）
 * - finally 區塊確保 MDC 清理
 * - 支援微服務架構的 traceId 傳遞
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(TraceIdFilter.class);

    /**
     * HTTP Header 名稱
     * 用於微服務間傳遞 traceId
     */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * MDC Key
     * 用於 Logback 輸出
     */
    private static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // 1. 取得或生成 traceId
            String traceId = getOrGenerateTraceId(httpRequest);

            // 2. 存入 MDC
            MDC.put(TRACE_ID_MDC_KEY, traceId);

            // 3. 將 traceId 加入 Response Header（用於微服務間傳遞）
            httpResponse.setHeader(TRACE_ID_HEADER, traceId);

            // 4. 記錄請求資訊（可選，用於除錯）
            if (logger.isDebugEnabled()) {
                logger.debug("Request started - Path: {}, Method: {}, TraceId: {}",
                        httpRequest.getRequestURI(),
                        httpRequest.getMethod(),
                        traceId);
            }

            // 5. 繼續 Filter Chain
            chain.doFilter(request, response);

        } finally {
            // 6. 清理 MDC（重要！避免記憶體洩漏）
            MDC.remove(TRACE_ID_MDC_KEY);

            if (logger.isDebugEnabled()) {
                logger.debug("Request completed - Path: {}, Status: {}",
                        httpRequest.getRequestURI(),
                        httpResponse.getStatus());
            }
        }
    }

    /**
     * 取得或生成 TraceId
     *
     * 優先順序:
     * 1. 從 Request Header 讀取（微服務傳遞）
     * 2. 生成新的 UUID
     */
    private String getOrGenerateTraceId(HttpServletRequest request) {
        // 優先從 Header 讀取（支援微服務架構）
        String traceId = request.getHeader(TRACE_ID_HEADER);

        if (traceId != null && !traceId.trim().isEmpty()) {
            return traceId.trim();
        }

        // 生成新的 traceId
        return generateTraceId();
    }

    /**
     * 生成 TraceId
     *
     * 格式: UUID（32 字元，去除連字號）
     * 範例: 550e8400e29b41d4a716446655440000
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
