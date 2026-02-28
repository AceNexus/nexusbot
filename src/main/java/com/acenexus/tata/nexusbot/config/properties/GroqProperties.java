package com.acenexus.tata.nexusbot.config.properties;

import com.acenexus.tata.nexusbot.constants.AiModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "groq")
public class GroqProperties {
    private String apiKey;

    private String url = "https://api.groq.com/openai/v1";

    private String defaultModel = AiModel.LLAMA_3_1_8B.id;
}