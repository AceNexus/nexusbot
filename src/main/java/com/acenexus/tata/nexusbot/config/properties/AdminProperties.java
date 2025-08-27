package com.acenexus.tata.nexusbot.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "admin")
public class AdminProperties {

    /**
     * 密碼基礎
     * 用於生成動態密碼的基礎字串
     */
    private String passwordSeed;
}