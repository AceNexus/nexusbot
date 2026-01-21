package com.acenexus.tata.nexusbot.scheduler;

import com.acenexus.tata.nexusbot.service.StockChipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 股票數據同步排程
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockSyncScheduler {

    private final StockChipService stockChipService;

    /**
     * 每日下午 17:00 同步當日法人籌碼數據
     * 台灣證交所與櫃買中心通常在 15:00 - 16:30 之間完成結算
     */
    @Scheduled(cron = "0 0 17 * * MON-FRI")
    public void syncDailyInstitutionalStats() {
        LocalDate today = LocalDate.now();
        log.info("Starting daily institutional stats sync for date: {}", today);

        try {
            stockChipService.fetchAndSaveDailyStats(today);
            log.info("Daily institutional stats sync completed for date: {}", today);
        } catch (Exception e) {
            log.error("Failed to sync daily institutional stats for date: {}", today, e);
        }
    }

    /**
     * 每週日凌晨 03:00 執行一次「回溯補漏」
     * 檢查過去 7 天是否有漏掉的交易日數據並自動補齊
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void backfillMissingStats() {
        LocalDate today = LocalDate.now();
        log.info("Starting weekly backfill process...");

        for (int i = 1; i <= 7; i++) {
            LocalDate targetDate = today.minusDays(i);
            // 只處理平日
            if (targetDate.getDayOfWeek().getValue() < 6) {
                stockChipService.fetchAndSaveDailyStats(targetDate);
            }
        }

        log.info("Weekly backfill process completed.");
    }
}
