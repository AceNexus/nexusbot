package com.acenexus.tata.nexusbot.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * 系統時區設定
 * 設定 JVM 預設時區為 UTC，確保：
 * 1. 所有伺服器部署使用統一時區
 * 2. 日誌與除錯時間一致
 * 3. 避免 LocalDateTime.now() 產生混亂
 * 注意：使用者時區儲存於 ChatRoom.timezone，與系統時區分離
 */
@Slf4j
@Configuration
public class TimezoneConfig {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        log.info("System timezone set to UTC (offset: {})", TimeZone.getDefault().getRawOffset());
        log.info("Current system time: {}", java.time.Instant.now());
    }
}
