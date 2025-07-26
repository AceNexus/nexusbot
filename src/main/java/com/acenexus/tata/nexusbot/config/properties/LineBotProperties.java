package com.acenexus.tata.nexusbot.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "line.bot")
public class LineBotProperties {
    @NotBlank(message = "LINE Bot channel token cannot be empty")
    private String channelToken;
    @NotBlank(message = "LINE Bot channel secret cannot be empty")
    private String channelSecret;
}