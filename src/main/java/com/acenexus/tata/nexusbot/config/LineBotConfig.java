package com.acenexus.tata.nexusbot.config;

import com.acenexus.tata.nexusbot.config.properties.LineBotProperties;
import com.linecorp.bot.client.LineMessagingClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LineBotConfig {

    private final LineBotProperties lineBotProperties;

    public LineBotConfig(LineBotProperties lineBotProperties) {
        this.lineBotProperties = lineBotProperties;
    }

    @Bean
    public LineMessagingClient lineMessagingClient() {
        return LineMessagingClient.builder(lineBotProperties.getChannelToken()).build();
    }
}