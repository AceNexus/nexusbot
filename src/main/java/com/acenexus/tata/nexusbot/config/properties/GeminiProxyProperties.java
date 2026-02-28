package com.acenexus.tata.nexusbot.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "gemini-proxy")
public class GeminiProxyProperties {
    private String url;
    private String apiKey;
}
