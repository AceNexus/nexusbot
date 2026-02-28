package com.acenexus.tata.nexusbot.config;

import com.acenexus.tata.nexusbot.config.properties.AdminProperties;
import com.acenexus.tata.nexusbot.config.properties.EmailProperties;
import com.acenexus.tata.nexusbot.config.properties.GeminiProxyProperties;
import com.acenexus.tata.nexusbot.config.properties.GroqProperties;
import com.acenexus.tata.nexusbot.config.properties.LineBotProperties;
import com.acenexus.tata.nexusbot.config.properties.OsmProperties;
import com.acenexus.tata.nexusbot.config.properties.TimezoneProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@EnableConfigurationProperties({
        LineBotProperties.class,
        GroqProperties.class,
        GeminiProxyProperties.class,
        AdminProperties.class,
        OsmProperties.class,
        EmailProperties.class,
        TimezoneProperties.class
})
public class ConfigValidator {
    private static final Logger logger = LoggerFactory.getLogger(ConfigValidator.class);
    private final LineBotProperties lineBotProperties;
    private final GroqProperties groqProperties;
    private final GeminiProxyProperties geminiProxyProperties;

    public ConfigValidator(LineBotProperties lineBotProperties,
                           GroqProperties groqProperties,
                           GeminiProxyProperties geminiProxyProperties) {
        this.lineBotProperties = lineBotProperties;
        this.groqProperties = groqProperties;
        this.geminiProxyProperties = geminiProxyProperties;
    }

    @PostConstruct
    public void validateConfiguration() {
        logger.info("Starting configuration validation...");

        validateLineBotConfig();
        validateAiConfig();

        logger.info("Configuration validation completed");
    }

    private void validateLineBotConfig() {
        if (!StringUtils.hasText(lineBotProperties.getChannelToken())) {
            throw new IllegalStateException("LINE Bot channel token is not configured or empty");
        }

        if (!StringUtils.hasText(lineBotProperties.getChannelSecret())) {
            throw new IllegalStateException("LINE Bot channel secret is not configured or empty");
        }

        logger.info("LINE Bot configuration validation passed");
    }

    private void validateAiConfig() {
        if (!StringUtils.hasText(groqProperties.getApiKey())) {
            throw new IllegalStateException("Groq API key (GROQ_API_KEY) is not configured");
        }
        logger.info("AI provider GROQ enabled, url: {}", groqProperties.getUrl());
        logger.info("AI provider GEMINI_PROXY enabled, url: {}", geminiProxyProperties.getUrl());
    }
}