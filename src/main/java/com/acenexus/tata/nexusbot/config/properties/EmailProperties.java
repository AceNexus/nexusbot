package com.acenexus.tata.nexusbot.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 電子郵件配置屬性
 */
@Data
@ConfigurationProperties(prefix = "email")
public class EmailProperties {

    /**
     * SMTP 主機位址
     * 例如: smtp.gmail.com
     */
    private String host;

    /**
     * SMTP 端口
     * Gmail 使用 587 (TLS) 或 465 (SSL)
     */
    private Integer port;

    /**
     * SMTP 使用者名稱（通常是電子郵件地址）
     */
    private String username;

    /**
     * SMTP 密碼或應用程式專用密碼
     * 建議使用環境變數設定，不要寫在配置檔中
     */
    private String password;

    /**
     * 寄件者電子郵件地址
     */
    private String from;

    /**
     * 寄件者名稱
     */
    private String fromName;

    /**
     * 是否啟用 SMTP 認證
     * 預設: true
     */
    private Boolean auth = true;

    /**
     * 是否啟用 STARTTLS
     * 預設: true
     */
    private Boolean starttls = true;

    /**
     * 是否啟用除錯模式
     * 預設: false
     */
    private Boolean debug = false;

    /**
     * 提醒確認連結的基礎 URL
     * 例如: https://your-domain.com
     * 完整確認連結格式: {confirmationBaseUrl}/reminder/confirm/{token}
     */
    private String confirmationBaseUrl;
}
