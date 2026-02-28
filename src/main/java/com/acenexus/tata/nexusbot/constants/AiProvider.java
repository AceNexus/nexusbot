package com.acenexus.tata.nexusbot.constants;

/**
 * AI 廠商識別符。
 * 每個 {@link AiModel} 常數綁定一個 provider，選模型即自動決定廠商。
 * 各廠商的 URL 與 API Key 定義在 application.yml，支援環境變數覆蓋。
 */
public enum AiProvider {

    /**
     * Groq Cloud — OpenAI 相容介面
     */
    GROQ,

    /**
     * 本地 Gemini Proxy（AIClient-2-API 或其他相容工具）
     */
    GEMINI_PROXY
}
