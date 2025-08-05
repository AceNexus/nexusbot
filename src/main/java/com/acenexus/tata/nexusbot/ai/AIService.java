package com.acenexus.tata.nexusbot.ai;

/**
 * AI 聊天服務介面
 */
public interface AIService {

    /**
     * 處理聊天對話
     *
     * @param message 使用者訊息
     * @return AI 回應，失敗時回傳 null
     */
    String chat(String message);
}