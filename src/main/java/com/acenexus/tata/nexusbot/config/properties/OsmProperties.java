package com.acenexus.tata.nexusbot.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "osm")
public class OsmProperties {

    @NotBlank(message = "OSM Overpass API base URL cannot be empty")
    private String overpassBaseUrl;

    @Positive(message = "OSM timeout must be positive")
    private int timeoutMs;

    @NotBlank(message = "OSM user agent cannot be empty")
    private String userAgent;

    // UI 顯示配置
    @Positive(message = "OSM carousel max items must be positive")
    private int carouselMaxItems;

    // 業務規則配置（OSM 標籤查詢規則）
    @NotEmpty(message = "OSM toilet tags cannot be empty")
    private String[] toiletTags;

    @NotEmpty(message = "OSM facility tags cannot be empty")
    private String[] facilityTags;
}