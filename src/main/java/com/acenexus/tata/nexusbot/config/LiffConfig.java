package com.acenexus.tata.nexusbot.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * LINE LIFF (LINE Front-end Framework) 配置
 * LIFF 是 LINE 官方提供的前端框架，用於在 LINE 內嵌瀏覽器中運行 Web 應用。
 * 透過 LIFF SDK 可以安全地獲取用戶資訊，無需手動處理身份驗證。
 * 配置 LIFF 應用步驟：
 * 1. 前往 LINE Developers Console: https://developers.line.biz/console/
 * 2. 選擇你的 Provider 和 Channel
 * 3. 進入 "LIFF" 頁籤
 * 4. 點擊 "Add" 建立新的 LIFF app
 * 5. 設定參數：
 * - LIFF app name: NexusBot - 台股分析
 * - Size: Full
 * - Endpoint URL: https://your-domain.com/stock.html
 * - Scope: profile, openid (勾選需要的權限)
 * - Bot link feature: On (Normal) - 啟用可讓用戶從 LIFF 回到聊天室
 * 6. 複製生成的 LIFF ID (格式: 1234567890-abcdefgh)
 * 7. 設定環境變數: LINE_LIFF_STOCK_ANALYSIS_ID=你的LIFF_ID
 */
@Getter
@Configuration
@ConfigurationProperties(prefix = "line.liff")
public class LiffConfig {

    /**
     * 台股分析頁面的 LIFF ID
     * LIFF ID 格式：1234567890-abcdefgh
     * 從 LINE Developers Console 的 LIFF 頁籤取得
     */
    private String stockAnalysisId;

    public void setStockAnalysisId(String stockAnalysisId) {
        this.stockAnalysisId = stockAnalysisId;
    }

    /**
     * 取得完整的 LIFF URL
     *
     * @return LIFF URL (例如: https://liff.line.me/1234567890-abcdefgh)
     */
    public String getStockAnalysisUrl() {
        if (stockAnalysisId == null || stockAnalysisId.isEmpty()) {
            throw new IllegalStateException(
                    "LIFF Stock Analysis ID is not configured. " +
                            "Please set LINE_LIFF_STOCK_ANALYSIS_ID environment variable. " +
                            "Visit https://developers.line.biz/console/ to create a LIFF app."
            );
        }
        return "https://liff.line.me/" + stockAnalysisId;
    }

    /**
     * 檢查 LIFF 是否已配置
     *
     * @return true 如果已配置 LIFF ID
     */
    public boolean isConfigured() {
        return stockAnalysisId != null && !stockAnalysisId.isEmpty();
    }
}
