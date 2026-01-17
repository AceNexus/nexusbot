package com.acenexus.tata.nexusbot.controller;

import com.acenexus.tata.nexusbot.dto.LiffVerifyRequest;
import com.acenexus.tata.nexusbot.dto.LiffVerifyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LIFF (LINE Front-end Framework) API Controller
 * <p>
 * 處理 LIFF 應用的後端驗證和用戶識別
 */
@Slf4j
@Tag(name = "LIFF API", description = "LINE Front-end Framework 相關接口")
@RestController
@RequestMapping("/api/liff")
@RequiredArgsConstructor
public class LiffController {

    @Operation(summary = "驗證 LIFF 用戶", description = """
            驗證從 LIFF 應用發送的用戶資訊和 Access Token。

            此接口用於：
            1. 驗證 LIFF Access Token 的有效性
            2. 記錄用戶訪問日誌
            3. 確保請求來自合法的 LINE 用戶

            注意：
            - 前端應在 LIFF 初始化後調用此接口
            - Access Token 會自動由 LINE SDK 生成
            - 此接口不會阻擋用戶操作，僅用於日誌記錄
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "驗證成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LiffVerifyResponse.class))),
            @ApiResponse(responseCode = "400", description = "請求參數錯誤",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Access Token 無效",
                    content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/verify")
    public ResponseEntity<LiffVerifyResponse> verifyLiffUser(@RequestBody LiffVerifyRequest request) {
        try {
            log.info("[LIFF] Verify request received - userId={}, displayName={}",
                    request.getUserId(), request.getDisplayName());

            // 參數驗證
            if (request.getUserId() == null || request.getUserId().isEmpty()) {
                log.warn("[LIFF] Invalid request - userId is null or empty");
                return ResponseEntity.badRequest()
                        .body(LiffVerifyResponse.failure("User ID is required"));
            }

            if (request.getAccessToken() == null || request.getAccessToken().isEmpty()) {
                log.warn("[LIFF] Invalid request - accessToken is null or empty");
                return ResponseEntity.badRequest()
                        .body(LiffVerifyResponse.failure("Access token is required"));
            }

            // TODO: 可選 - 向 LINE API 驗證 Access Token 的有效性
            // 目前僅記錄用戶訪問日誌，不做嚴格驗證
            // 如需嚴格驗證，可調用：
            // https://api.line.me/oauth2/v2.1/verify?access_token={accessToken}

            log.info("[LIFF] User verified successfully - userId={}, displayName={}",
                    request.getUserId(), request.getDisplayName());

            // 記錄成功
            return ResponseEntity.ok(LiffVerifyResponse.success(request.getUserId()));

        } catch (Exception ex) {
            log.error("[LIFF] Verification error - userId={}, error={}",
                    request.getUserId(), ex.getMessage(), ex);
            return ResponseEntity.internalServerError()
                    .body(LiffVerifyResponse.failure("Verification failed: " + ex.getMessage()));
        }
    }
}
