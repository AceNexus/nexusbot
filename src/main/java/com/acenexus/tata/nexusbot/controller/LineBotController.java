package com.acenexus.tata.nexusbot.controller;

import com.acenexus.tata.nexusbot.service.EventHandlerService;
import com.acenexus.tata.nexusbot.util.SignatureValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "LINE Webhook", description = "LINE Messaging API Webhook 接收端點")
@RestController
@RequiredArgsConstructor
public class LineBotController {
    private static final Logger logger = LoggerFactory.getLogger(LineBotController.class);
    private final ObjectMapper objectMapper;
    private final EventHandlerService eventHandlerService;
    private final SignatureValidator signatureValidator;

    @Operation(
            summary = "處理 LINE Webhook 事件",
            description = """
                    接收並處理來自 LINE Messaging API 的 Webhook 事件。

                    **支援的事件類型**:
                    - Message Event (文字、圖片、貼圖、影片、音訊、檔案、位置)
                    - Postback Event (按鈕點擊回調)
                    - Follow Event (使用者加入好友)
                    - Unfollow Event (使用者封鎖)
                    - Join Event (機器人加入群組)
                    - Leave Event (機器人離開群組)

                    **處理流程**:
                    1. 驗證 X-Line-Signature 簽章
                    2. 解析 JSON payload
                    3. 路由事件至對應的 Handler
                    4. 非同步處理並回應 200 OK

                    **安全性**:
                    - 簽章驗證確保請求來自 LINE 官方
                    - 簽章驗證失敗仍回應 200 OK (避免 LINE 重送)
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "事件接收成功",
                    content = @Content(
                            mediaType = "text/plain",
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "OK")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "請求格式錯誤",
                    content = @Content(mediaType = "text/plain")
            )
    })
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @Parameter(
                    description = "LINE Webhook Event Payload (JSON 格式)",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Message Event",
                                            summary = "文字訊息事件",
                                            value = """
                                                    {
                                                      "events": [
                                                        {
                                                          "type": "message",
                                                          "message": {
                                                            "type": "text",
                                                            "id": "123456789",
                                                            "text": "Hello, NexusBot!"
                                                          },
                                                          "source": {
                                                            "type": "user",
                                                            "userId": "U1234567890abcdef"
                                                          },
                                                          "replyToken": "abc123def456"
                                                        }
                                                      ]
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Postback Event",
                                            summary = "按鈕回調事件",
                                            value = """
                                                    {
                                                      "events": [
                                                        {
                                                          "type": "postback",
                                                          "postback": {
                                                            "data": "action=MAIN_MENU"
                                                          },
                                                          "source": {
                                                            "type": "user",
                                                            "userId": "U1234567890abcdef"
                                                          },
                                                          "replyToken": "abc123def456"
                                                        }
                                                      ]
                                                    }
                                                    """
                                    )
                                            }
                    )
            )
            @RequestBody String payload,
            @Parameter(
                    description = "LINE 簽章驗證 Header (HMAC-SHA256)",
                    example = "abcd1234efgh5678ijkl9012mnop3456qrst7890uvwx1234yz56"
            )
            @RequestHeader(value = "X-Line-Signature", required = false) String signature) throws Exception {

        logger.info("Received LINE webhook request, payload size: {} bytes", payload.length());

        if (signature != null && !signatureValidator.validate(payload, signature)) {
            logger.warn("Invalid webhook signature");
            return ResponseEntity.ok("OK");
        }

        JsonNode requestBody = objectMapper.readTree(payload);
        JsonNode events = requestBody.get("events");

        if (events != null && events.isArray() && !events.isEmpty()) {
            logger.info("Processing {} events", events.size());
            eventHandlerService.processEvents(events);
        } else {
            logger.info("Received empty events array");
        }

        return ResponseEntity.ok("OK");
    }
}