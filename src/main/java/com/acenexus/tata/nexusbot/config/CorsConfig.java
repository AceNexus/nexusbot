package com.acenexus.tata.nexusbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS 跨域設定
 * 允許從不同來源（如 IntelliJ 內建伺服器）訪問 API
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 允許的來源
        // localhost:63342 - IntelliJ 內建伺服器
        // localhost:5001 - Spring Boot 本身
        config.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));

        // 允許的 HTTP 方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 允許的 Headers
        config.setAllowedHeaders(List.of("*"));

        // 允許攜帶憑證（如 Cookies）
        config.setAllowCredentials(true);

        // 預檢請求（OPTIONS）的快取時間（秒）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 對所有路徑應用 CORS 設定
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
