package com.acenexus.tata.nexusbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LIFF 驗證請求 DTO
 * 前端 LIFF 應用發送用戶資訊到後端進行驗證
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiffVerifyRequest {

    /**
     * LINE 用戶 ID
     */
    private String userId;

    /**
     * 用戶顯示名稱
     */
    private String displayName;

    /**
     * 用戶頭像 URL
     */
    private String pictureUrl;

    /**
     * LIFF Access Token（用於後端驗證）
     */
    private String accessToken;
}
