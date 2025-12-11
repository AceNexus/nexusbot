package com.acenexus.tata.nexusbot.template;

import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TextMessage;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static com.acenexus.tata.nexusbot.constants.Actions.ABOUT;
import static com.acenexus.tata.nexusbot.constants.Actions.EMAIL_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.FIND_TOILETS;
import static com.acenexus.tata.nexusbot.constants.Actions.HELP_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.MAIN_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.REMINDER_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.TIMEZONE_SETTINGS;
import static com.acenexus.tata.nexusbot.constants.Actions.TOGGLE_AI;
import static com.acenexus.tata.nexusbot.template.UIConstants.Colors;

/**
 * 導航與通用範本建構器
 * 包含歡迎訊息、主選單、說明頁面、系統狀態等通用範本
 */
@Component
public class NavigationTemplateBuilder extends FlexMessageTemplateBuilder {

    /**
     * 歡迎訊息
     */
    public Message welcome() {
        return createCard(
                "歡迎使用 NexusBot",
                "您的智能助手已準備就緒。我們提供 AI 對話服務、智慧提醒管理，以及完整的功能支援。",
                Arrays.asList(
                        createPrimaryButton("開始使用", MAIN_MENU),
                        createNeutralButton("功能說明", HELP_MENU)
                )
        );
    }

    /**
     * 關於訊息
     */
    public Message about() {
        return createCard(
                "NexusBot v2.0",
                "專業 AI 智能助手平台\n\n核心功能包括智能對話、提醒管理、多模型支援等服務。採用現代化架構設計，提供穩定可靠的使用體驗。",
                Arrays.asList(
                        createNavigateButton("返回主選單", MAIN_MENU)
                )
        );
    }

    /**
     * 主選單
     */
    public Message mainMenu() {
        return createCard(
                "功能選單",
                "請選擇您需要使用的功能服務",
                Arrays.asList(
                        createPrimaryButton("AI 智能對話", TOGGLE_AI),
                        createNeutralButton("提醒管理", REMINDER_MENU),
                        createNeutralButton("Email 通知", EMAIL_MENU),
                        createNeutralButton("時區設定", TIMEZONE_SETTINGS),
                        createNeutralButton("找附近廁所", FIND_TOILETS),
                        createNeutralButton("說明與支援", HELP_MENU)
                )
        );
    }

    /**
     * 說明選單
     */
    public Message helpMenu() {
        return createCard(
                "說明與支援",
                "NexusBot 提供完整的使用指南和技術支援服務。您可以查看功能說明、系統狀態，或聯繫我們的支援團隊。",
                Arrays.asList(
                        createNeutralButton("查看系統資訊", ABOUT),
                        createNavigateButton("返回主選單", MAIN_MENU)
                )
        );
    }

    /**
     * 系統統計資訊
     */
    public Message systemStats(long totalRooms, long aiEnabledRooms, long adminRooms,
                               long totalMessages, long userMessages, long aiMessages,
                               long todayActiveRooms, long weekActiveRooms, String avgProcessingTime) {

        double aiEnabledPercent = totalRooms > 0 ? (aiEnabledRooms * 100.0 / totalRooms) : 0.0;

        String statsText = String.format(
                "聊天室統計\n總計：%,d 間｜AI啟用：%,d 間 (%.1f%%)｜管理員：%,d 間\n\n" +
                        "訊息統計\n總計：%,d 條｜用戶：%,d 條｜AI回應：%,d 條\n\n" +
                        "活躍度\n今日：%,d 間｜本週：%,d 間\n\n" +
                        "AI 性能\n平均響應時間：%s",
                totalRooms, aiEnabledRooms, aiEnabledPercent, adminRooms,
                totalMessages, userMessages, aiMessages,
                todayActiveRooms, weekActiveRooms, avgProcessingTime
        );

        return createCard("NexusBot 系統狀態", statsText, Arrays.asList(
                createNavigateButton("返回主選單", MAIN_MENU)
        ));
    }

    /**
     * 成功訊息
     */
    public Message success(String message) {
        return createStatusCard("操作成功", message, Colors.SUCCESS);
    }

    /**
     * 錯誤訊息
     */
    public Message error(String message) {
        return createStatusCard("操作失敗", message, Colors.ERROR);
    }

    /**
     * Postback 回應訊息
     */
    public Message postbackResponse(String data) {
        return TextMessage.builder()
                .text("按鈕點擊: " + data + "\n感謝您的互動！")
                .build();
    }

    // ==================== 純文字回應方法 ====================

    /**
     * 圖片回應
     */
    public String imageResponse(String messageId) {
        return "收到您的圖片\n圖片ID: " + messageId;
    }

    /**
     * 貼圖回應
     */
    public String stickerResponse(String packageId, String stickerId) {
        return String.format("很可愛的貼圖\n貼圖包ID: %s\n貼圖ID: %s", packageId, stickerId);
    }

    /**
     * 影片回應
     */
    public String videoResponse(String messageId) {
        return "收到您的影片\n影片ID: " + messageId;
    }

    /**
     * 音檔回應
     */
    public String audioResponse(String messageId) {
        return "收到您的音檔\n音檔ID: " + messageId;
    }

    /**
     * 檔案回應
     */
    public String fileResponse(String fileName, long fileSize) {
        return String.format("收到您的檔案\n檔名: %s\n大小: %d bytes", fileName, fileSize);
    }

    /**
     * 位置回應
     */
    public String locationResponse(String title, String address, double latitude, double longitude) {
        StringBuilder response = new StringBuilder("收到您的位置資訊");
        if (title != null && !title.trim().isEmpty()) {
            response.append("\n地點名稱: ").append(title);
        }
        if (address != null && !address.trim().isEmpty()) {
            response.append("\n地址: ").append(address);
        }
        response.append(String.format("\n座標: %.6f, %.6f", latitude, longitude));
        return response.toString();
    }

    /**
     * 未知訊息類型
     */
    public String unknownMessage() {
        return "收到您的訊息，但目前無法識別此類型。";
    }

    /**
     * 預設文字回應
     */
    public String defaultTextResponse(String messageText) {
        return "我們已收到您的訊息：「" + messageText + "」\n輸入 menu 查看支援的指令。";
    }

    /**
     * 群組加入訊息
     */
    public String groupJoinMessage(String sourceType) {
        return "Hello everyone! I'm NexusBot!\nHappy to join this " +
                ("group".equals(sourceType) ? "group" : "room") + "!";
    }

    /**
     * 成員加入訊息
     */
    public String memberJoinedMessage(int memberCount) {
        return "Welcome new members!\n" + memberCount + " new friends joined the group!";
    }
}
