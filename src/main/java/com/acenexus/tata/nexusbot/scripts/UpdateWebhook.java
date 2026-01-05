package com.acenexus.tata.nexusbot.scripts;

import com.acenexus.tata.nexusbot.config.properties.LineBotProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class UpdateWebhook {

    private static final Logger logger = LoggerFactory.getLogger(UpdateWebhook.class);
    private static final String NGROK_API_URL = "http://127.0.0.1:4040/api/tunnels";
    private static final String LINE_WEBHOOK_API = "https://api.line.me/v2/bot/channel/webhook/endpoint";
    private static final String CALLBACK_PATH = "/webhook";

    private String lastUrl = null;
    private final RestTemplate restTemplate = new RestTemplate();
    private final LineBotProperties lineBotProperties;

    /**
     * 每 5 秒執行一次檢查
     */
    @Scheduled(fixedDelay = 5000)
    public void checkAndUpdateWebhook() {
        String currentUrl = getNgrokUrl();

        if (currentUrl != null && !currentUrl.equals(lastUrl)) {
            logger.info("偵測到新的 ngrok URL: {}", currentUrl);
            if (updateLineWebhook(currentUrl)) {
                lastUrl = currentUrl;
            }
        }
    }

    private String getNgrokUrl() {
        try {
            JsonNode response = restTemplate.getForObject(NGROK_API_URL, JsonNode.class);
            JsonNode tunnels = response.get("tunnels");

            for (JsonNode tunnel : tunnels) {
                String publicUrl = tunnel.get("public_url").asText();
                if (publicUrl.startsWith("https://")) {
                    return publicUrl;
                }
            }
        } catch (Exception e) {
            logger.error("無法取得 ngrok URL: {}", e.getMessage());
        }
        return null;
    }

    private boolean updateLineWebhook(String ngrokUrl) {
        String endpoint = ngrokUrl + CALLBACK_PATH;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + lineBotProperties.getChannelToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("{\"endpoint\":\"%s\"}", endpoint);
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    LINE_WEBHOOK_API,
                    HttpMethod.PUT,
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Webhook 更新成功: {}", endpoint);
                return true;
            } else {
                logger.error("Webhook 更新失敗: {}", response.getBody());
                return false;
            }
        } catch (Exception e) {
            logger.error("LINE API 請求失敗: {}", e.getMessage());
            return false;
        }
    }
}