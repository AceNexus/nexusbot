package com.acenexus.tata.nexusbot.constants;

/**
 * AI 模型清單（跨廠商）
 * 新增模型時只需在此加一個 enum 常數，temperature/maxTokens/displayName/provider 一併定義。
 * provider 欄位決定呼叫哪個廠商的 WebClient，選模型即自動切換廠商。
 */
public enum AiModel {

    // Groq 模型
    LLAMA_3_1_8B("llama-3.1-8b-instant", "Llama 3.1 8B", 0.8, 800, AiProvider.GROQ),
    LLAMA_3_3_70B("llama-3.3-70b-versatile", "Llama 3.3 70B", 0.6, 1200, AiProvider.GROQ),

    // Gemini 模型（透過 gemini-proxy 使用）
    GEMINI_25_FLASH("gemini-2.5-flash", "Gemini 2.5 Flash", 0.8, 2048, AiProvider.GEMINI_PROXY);

    public final String id;
    public final String displayName;
    public final double temperature;
    public final int maxTokens;
    public final AiProvider provider;

    AiModel(String id, String displayName, double temperature, int maxTokens, AiProvider provider) {
        this.id = id;
        this.displayName = displayName;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.provider = provider;
    }

    /**
     * 依 model ID 查找，找不到時回傳預設模型 {@link #LLAMA_3_1_8B}。
     */
    public static AiModel fromId(String id) {
        for (AiModel m : values()) {
            if (m.id.equals(id)) return m;
        }
        return LLAMA_3_1_8B;
    }
}
