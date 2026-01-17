package com.acenexus.tata.nexusbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LIFF 驗證響應 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiffVerifyResponse {

    /**
     * 驗證是否成功
     */
    private boolean success;

    /**
     * 驗證後的用戶 ID
     */
    private String userId;

    /**
     * 訊息
     */
    private String message;

    /**
     * 快速建立成功響應
     */
    public static LiffVerifyResponse success(String userId) {
        return LiffVerifyResponse.builder()
                .success(true)
                .userId(userId)
                .message("Verification successful")
                .build();
    }

    /**
     * 快速建立失敗響應
     */
    public static LiffVerifyResponse failure(String message) {
        return LiffVerifyResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}
