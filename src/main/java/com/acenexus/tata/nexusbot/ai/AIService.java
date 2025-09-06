package com.acenexus.tata.nexusbot.ai;

/**
 * AI 聊天服務介面
 */
public interface AIService {

    /**
     * 處理聊天對話（帶上下文和指定模型）
     *
     * @param roomId  聊天室 ID，用於查詢對話歷史
     * @param message 使用者訊息
     * @param model   指定的AI模型
     * @return AI 回應結果（包含模型、tokens等資訊）
     */
    ChatResponse chatWithContext(String roomId, String message, String model);

    /**
     * AI 回應結果
     */
    record ChatResponse(String content, String model, Integer tokensUsed, Long processingTime, boolean success) {
    }
}