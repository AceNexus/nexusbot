package com.acenexus.tata.nexusbot.fliter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * TraceId 追蹤 Filter
 * 功能：
 * - 讀取 Micrometer Tracing（OTel Bridge）自動寫入 MDC 的 traceId
 * - 將 traceId 透過 X-Trace-Id Response Header 回傳，方便 client 端關聯
 * 注意：
 * - MDC 的 traceId / spanId 由 OTel 的 WebMvcObservationFilter（HIGHEST_PRECEDENCE + 1）負責寫入與清理
 * - 此 Filter 不再自行生成 traceId，也不操作 MDC，避免與 OTel 衝突
 * - Order 設為 HIGHEST_PRECEDENCE + 2，確保在 OTel filter 之後執行，MDC 已有值
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class TraceIdFilter implements Filter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // OTel WebMvcObservationFilter（優先順序更高）已將 traceId 寫入 MDC
        // 直接讀取並放入 Response Header，供 client 端（LINE Bot debug、Gateway 追蹤）使用
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        if (traceId != null) {
            httpResponse.setHeader(TRACE_ID_HEADER, traceId);
        }

        chain.doFilter(request, response);
        // MDC 清理由 OTel 的 WebMvcObservationFilter 負責，此處不介入
    }
}
