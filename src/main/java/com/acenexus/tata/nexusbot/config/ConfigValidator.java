package com.acenexus.tata.nexusbot.config;

import com.acenexus.tata.nexusbot.config.properties.AdminProperties;
import com.acenexus.tata.nexusbot.config.properties.EmailProperties;
import com.acenexus.tata.nexusbot.config.properties.GroqProperties;
import com.acenexus.tata.nexusbot.config.properties.LineBotProperties;
import com.acenexus.tata.nexusbot.config.properties.OsmProperties;
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
        AdminProperties.class,
        OsmProperties.class,
        EmailProperties.class
})
public class ConfigValidator {
    private static final Logger logger = LoggerFactory.getLogger(ConfigValidator.class);
    private final LineBotProperties lineBotProperties;
    private final GroqProperties groqProperties;

    public ConfigValidator(LineBotProperties lineBotProperties, GroqProperties groqProperties) {
        this.lineBotProperties = lineBotProperties;
        this.groqProperties = groqProperties;
    }

    @PostConstruct
    public void validateConfiguration() {
        logger.info("Starting configuration validation...");

        validateLineBotConfig();
        validateGroqConfig();

        logger.info("Configuration validation completed, all required configurations are properly set");
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

    private void validateGroqConfig() {
        if (!StringUtils.hasText(groqProperties.getApiKey())) {
            throw new IllegalStateException("Groq API key is not configured or empty");
        }

        logger.info("Groq configuration validation passed");
    }
}