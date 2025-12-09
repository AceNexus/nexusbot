package com.acenexus.tata.nexusbot.util;

import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * MDC 任務裝飾器
 * 功能：
 * - 在非同步任務執行前捕獲當前執行緒的 MDC context
 * - 在非同步執行緒中恢復 MDC context
 * - 執行完畢後清理 MDC，避免記憶體洩漏
 * 使用場景：
 * - CompletableFuture 非同步操作
 * - ExecutorService 執行緒池任務
 * - @Async 方法
 * 設計原則：
 * - 線程安全
 * - 自動清理，避免 MDC 污染
 * - 支援巢狀呼叫
 */
public class MdcTaskDecorator {

    /**
     * 包裝 Runnable 任務，自動傳遞 MDC context
     * 使用範例：
     * CompletableFuture.runAsync(MdcTaskDecorator.wrap(() -> {
     *     // 這裡可以正確取得 traceId
     *     log.info("Processing...");
     * }));
     */
    public static Runnable wrap(Runnable task) {
        // 在當前執行緒捕獲 MDC context
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            // 在非同步執行緒恢復 MDC context
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }

            try {
                // 執行實際任務
                task.run();
            } finally {
                // 清理 MDC，避免污染執行緒池
                MDC.clear();
            }
        };
    }

    /**
     * 包裝 Supplier 任務，自動傳遞 MDC context
     *
     * 使用範例：
     * <pre>
     * CompletableFuture.supplyAsync(MdcTaskDecorator.wrapSupplier(() -> {
     *     // 這裡可以正確取得 traceId
     *     return processData();
     * }));
     * </pre>
     */
    public static <T> Supplier<T> wrapSupplier(Supplier<T> task) {
        // 在當前執行緒捕獲 MDC context
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            // 在非同步執行緒恢復 MDC context
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }

            try {
                // 執行實際任務
                return task.get();
            } finally {
                // 清理 MDC，避免污染執行緒池
                MDC.clear();
            }
        };
    }
}
