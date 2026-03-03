package com.acenexus.tata.nexusbot.util;

import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;

import java.util.function.Supplier;

/**
 * MDC 任務裝飾器
 * 功能：
 * - 在非同步任務執行前捕獲當前執行緒的完整 context（MDC + OTel trace context）
 * - 在非同步執行緒中恢復 context，確保 traceId 在 log 中連續可見
 * - 非同步 Span 作為當前 Span 的子 Span，trace 關係在 Grafana 中正確呈現
 * 使用場景：
 * - CompletableFuture.runAsync(MdcTaskDecorator.wrap(...))
 * - CompletableFuture.supplyAsync(MdcTaskDecorator.wrapSupplier(...))
 * 設計原則：
 * - 使用 Micrometer ContextSnapshotFactory 同時傳播 MDC 與 OTel ThreadLocal context
 * - ContextSnapshot.Scope 在 try-with-resources 關閉時自動還原前一個 context，無需手動清理
 */
public class MdcTaskDecorator {

    private static final ContextSnapshotFactory SNAPSHOT_FACTORY = ContextSnapshotFactory.builder().build();

    /**
     * 包裝 Runnable，自動傳遞 MDC + OTel trace context 到非同步執行緒
     * 使用範例：
     * CompletableFuture.runAsync(MdcTaskDecorator.wrap(() -> {
     * log.info("traceId 在此處仍可見，且 Span 為父 Span 的子 Span");
     * }));
     */
    public static Runnable wrap(Runnable task) {
        ContextSnapshot snapshot = SNAPSHOT_FACTORY.captureAll();
        return () -> {
            try (ContextSnapshot.Scope ignored = snapshot.setThreadLocals()) {
                task.run();
            }
        };
    }

    /**
     * 包裝 Supplier，自動傳遞 MDC + OTel trace context 到非同步執行緒
     * 使用範例：
     * CompletableFuture.supplyAsync(MdcTaskDecorator.wrapSupplier(() -> processData()));
     */
    public static <T> Supplier<T> wrapSupplier(Supplier<T> task) {
        ContextSnapshot snapshot = SNAPSHOT_FACTORY.captureAll();
        return () -> {
            try (ContextSnapshot.Scope ignored = snapshot.setThreadLocals()) {
                return task.get();
            }
        };
    }
}
