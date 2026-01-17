package com.acenexus.tata.nexusbot.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 快取配置
 * 使用 Caffeine 作為快取實作
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 配置 Caffeine 快取管理器
     * 快取策略：
     * - twseStockList: TWSE 股票清單，1天過期
     * - twseInstitutional: TWSE 法人進出數據，1天過期
     * - stockFinancialData: 財務數據快取，1天過期
     *
     * @return CacheManager
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 預設快取配置：1天過期
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.DAYS)
                .maximumSize(10000));

        // 註冊快取名稱 (含 TWSE 快取)
        cacheManager.setCacheNames(List.of(
                "stockFinancialData",
                "twseStockList",
                "twseInstitutional"
        ));

        return cacheManager;
    }
}
