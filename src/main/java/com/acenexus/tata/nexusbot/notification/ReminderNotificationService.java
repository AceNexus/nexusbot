package com.acenexus.tata.nexusbot.notification;

import com.acenexus.tata.nexusbot.entity.Reminder;

/**
 * 提醒通知服務介面
 * 負責協調 LINE 和 Email 通知的發送
 * 設計目標：
 * - 統一通知邏輯，支援多種通知管道
 * - 封裝複雜的通知路由邏輯
 * - 便於擴充新的通知管道（如 Push Notification、SMS）
 */
public interface ReminderNotificationService {

    /**
     * 發送提醒通知
     * 根據 Reminder 的 notificationChannel 自動路由到對應管道
     *
     * @param reminder        提醒資料
     * @param enhancedContent AI 增強後的提醒內容（用於 LINE 訊息）
     */
    void send(Reminder reminder, String enhancedContent);

    /**
     * 僅發送 LINE 通知
     *
     * @param reminder        提醒資料
     * @param enhancedContent AI 增強後的提醒內容
     */
    void sendLineOnly(Reminder reminder, String enhancedContent);

    /**
     * 僅發送 Email 通知
     *
     * @param reminder        提醒資料
     * @param enhancedContent AI 增強後的提醒內容
     */
    void sendEmailOnly(Reminder reminder, String enhancedContent);

    /**
     * 同時發送 LINE 和 Email 通知
     *
     * @param reminder        提醒資料
     * @param enhancedContent AI 增強後的提醒內容（用於 LINE 訊息）
     */
    void sendBoth(Reminder reminder, String enhancedContent);
}
