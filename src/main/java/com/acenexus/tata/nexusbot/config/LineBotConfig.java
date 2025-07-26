package com.acenexus.tata.nexusbot.config;

import com.acenexus.tata.nexusbot.config.properties.LineBotProperties;
import com.linecorp.bot.messaging.client.MessagingApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LineBotConfig {

    private final LineBotProperties lineBotProperties;

    public LineBotConfig(LineBotProperties lineBotProperties) {
        this.lineBotProperties = lineBotProperties;
    }

    @Bean
    public MessagingApiClient messagingApiClient() {
        return MessagingApiClient.builder(lineBotProperties.getChannelToken()).build();
    }
}