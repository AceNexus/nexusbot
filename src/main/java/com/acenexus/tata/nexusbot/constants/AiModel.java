package com.acenexus.tata.nexusbot.constants;

/**
 * Groq 支援的 AI 模型清單
 * 新增模型時只需在此加一個 enum 常數，temperature/maxTokens/displayName 一併定義。
 */
public enum AiModel {

    LLAMA_3_1_8B("llama-3.1-8b-instant", "Llama 3.1 8B", 0.8, 800),
    LLAMA_3_3_70B("llama-3.3-70b-versatile", "Llama 3.3 70B", 0.6, 1200),
    LLAMA3_70B("llama3-70b-8192", "Llama3 70B", 0.65, 1500),
    GEMMA2_9B("gemma2-9b-it", "Gemma2 9B", 0.9, 1000),
    DEEPSEEK_R1("deepseek-r1-distill-llama-70b", "DeepSeek R1", 0.4, 1500),
    QWEN3_32B("qwen/qwen3-32b", "Qwen3 32B", 0.7, 1200);

    public final String id;
    public final String displayName;
    public final double temperature;
    public final int maxTokens;

    AiModel(String id, String displayName, double temperature, int maxTokens) {
        this.id = id;
        this.displayName = displayName;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
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
