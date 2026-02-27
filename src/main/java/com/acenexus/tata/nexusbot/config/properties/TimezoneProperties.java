package com.acenexus.tata.nexusbot.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "timezone")
public class TimezoneProperties {
    public static final String FALLBACK_DEFAULT = "Asia/Taipei";

    private String defaultTimezone = FALLBACK_DEFAULT;
}
