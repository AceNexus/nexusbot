package com.acenexus.tata.nexusbot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger 配置
 *
 * <p>提供完整的 API 文檔，包含：
 * <ul>
 *   <li>專案資訊與描述</li>
 *   <li>LINE Webhook Endpoints</li>
 *   <li>Email 確認 Endpoints</li>
 *   <li>健康檢查 Endpoints</li>
 * </ul>
 *
 * <p>訪問路徑：
 * <ul>
 *   <li>Swagger UI: <a href="http://localhost:5001/swagger-ui.html">http://localhost:5001/swagger-ui.html</a></li>
 *   <li>API Docs JSON: <a href="http://localhost:5001/v3/api-docs">http://localhost:5001/v3/api-docs</a></li>
 * </ul>
 *
 * @author NexusBot Team
 * @since 1.0.0
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:5001}")
    private String serverPort;

    @Value("${spring.application.name:nexusbot}")
    private String applicationName;

    /**
     * 配置 OpenAPI 實例
     *
     * @return OpenAPI 配置物件
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.nexusbot.example.com")
                                .description("Production Server (未來部署)")
                ));
    }

    /**
     * API 基本資訊
     *
     * @return API Info 物件
     */
    private Info apiInfo() {
        return new Info()
                .title("NexusBot API Documentation")
                .description("""
                        # NexusBot - 智能 LINE Bot 應用

                        ## 專案簡介
                        NexusBot 是一個基於 Spring Boot 3.4 的 LINE Bot 應用，整合 AI 對話、提醒管理、Email 通知等功能。

                        ## 技術棧
                        - **後端框架**: Spring Boot 3.4.3 + Java 17
                        - **LINE SDK**: LINE Bot SDK 6.0.0
                        - **AI 整合**: Groq API（多模型支援，見 AiModel enum）
                        - **資料庫**: MySQL 8.3 + Flyway 遷移
                        - **架構模式**: DDD + Strategy + Facade Pattern

                        ## 核心功能
                        1. **AI 智能對話** - 多模型支援、多輪對話、對話歷史管理
                        2. **提醒管理** - 單次/每日/每週提醒、AI 時間解析、三種通知管道
                        3. **Email 通知** - 多 Email 綁定、HTML 模板、確認機制
                        4. **位置服務** - 找附近廁所 (OSM API)
                        5. **管理員功能** - 兩步驟認證、系統統計

                        ## 架構設計亮點
                        - **設計模式**: Strategy, Chain of Responsibility, Facade
                        - **多實例支援**: Database-backed State + Distributed Lock
                        - **非同步處理**: CompletableFuture 避免阻塞
                        - **通知管道統一**: 易於擴充新管道 (SMS, Push)

                        ## API 類型
                        - **Webhook Endpoints**: 接收 LINE 事件 (Postback, Message, Follow, Join)
                        - **Confirmation Endpoints**: Email 確認連結處理
                        - **Health Check**: 健康檢查與監控

                        ## [GitHub Repository](https://github.com/orgs/AceNexus/repositories)
                        """)
                .version("1.0.0")
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }
}
