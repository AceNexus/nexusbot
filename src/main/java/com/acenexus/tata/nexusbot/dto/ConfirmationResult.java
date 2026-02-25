package com.acenexus.tata.nexusbot.dto;

/**
 * 提醒確認結果 DTO
 * 用於封裝 Email 確認連結點擊後的結果資訊
 */
public record ConfirmationResult(
        boolean success,
        String message,
        String detail,
        boolean alreadyConfirmed
) {
}
