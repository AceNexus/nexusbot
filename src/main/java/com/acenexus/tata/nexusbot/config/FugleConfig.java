package com.acenexus.tata.nexusbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Fugle API 設定
 * 免費註冊：https://developer.fugle.tw/
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "fugle")
public class FugleConfig {

    /**
     * Fugle API Key
     */
    private String apiKey;

    /**
     * WebSocket URL
     */
    private String websocketUrl = "wss://api.fugle.tw/marketdata/v1.0/stock/streaming";

    /**
     * REST API Base URL
     */
    private String restBaseUrl = "https://api.fugle.tw/marketdata/v1.0";

    /**
     * WebSocket 重連延遲（毫秒）
     */
    private long reconnectDelay = 5000;

    /**
     * WebSocket 最大訂閱數量
     */
    private int maxSubscriptions = 5;

    /**
     * 是否啟用 Fugle
     */
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isEmpty();
    }
}
