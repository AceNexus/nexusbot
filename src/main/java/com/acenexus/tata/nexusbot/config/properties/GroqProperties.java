package com.acenexus.tata.nexusbot.config.properties;

import com.acenexus.tata.nexusbot.constants.AiModel;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "groq")
public class GroqProperties {
    @NotBlank(message = "Groq API key cannot be empty")
    private String apiKey;

    private String defaultModel = AiModel.LLAMA_3_1_8B.id;
}