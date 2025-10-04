package com.acenexus.tata.nexusbot.email;

/**
 * 電子郵件服務介面
 * 提供郵件發送功能
 */
public interface EmailService {

    /**
     * 發送提醒郵件
     *
     * @param to      收件者電子郵件地址
     * @param subject 郵件主旨
     * @param content 郵件內容（純文字或HTML）
     * @param isHtml  是否為 HTML 格式
     * @return 發送是否成功
     */
    boolean sendEmail(String to, String subject, String content, boolean isHtml);

    /**
     * 發送提醒通知郵件（快捷方法）
     *
     * @param to           收件者電子郵件地址
     * @param reminderTime 提醒時間
     * @param reminderText 提醒內容
     * @return 發送是否成功
     */
    boolean sendReminderEmail(String to, String reminderTime, String reminderText);
}
