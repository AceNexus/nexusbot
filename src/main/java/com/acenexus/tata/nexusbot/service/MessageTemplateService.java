package com.acenexus.tata.nexusbot.service;

import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.message.FlexMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TextMessage;
import com.linecorp.bot.model.message.flex.component.Box;
import com.linecorp.bot.model.message.flex.component.Button;
import com.linecorp.bot.model.message.flex.component.FlexComponent;
import com.linecorp.bot.model.message.flex.component.Separator;
import com.linecorp.bot.model.message.flex.component.Text;
import com.linecorp.bot.model.message.flex.container.Bubble;
import com.linecorp.bot.model.message.flex.unit.FlexAlign;
import com.linecorp.bot.model.message.flex.unit.FlexFontSize;
import com.linecorp.bot.model.message.flex.unit.FlexLayout;
import com.linecorp.bot.model.message.flex.unit.FlexMarginSize;
import com.linecorp.bot.model.message.flex.unit.FlexPaddingSize;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class MessageTemplateService {

    // 顏色配置
    private static final String PRIMARY_COLOR = "#1976D2";
    private static final String SUCCESS_COLOR = "#4CAF50";
    private static final String INFO_COLOR = "#2196F3";
    private static final String WARNING_COLOR = "#FF9800";
    private static final String ERROR_COLOR = "#F44336";
    private static final String SECONDARY_COLOR = "#0288D1";

    public Message welcome() {
        return TextMessage.builder()
                .text("""
                        歡迎加入 NexusBot！
                                                
                        感謝您的支持，我將為您提供最佳的服務體驗。
                                                
                        ✨ 可用功能：
                        • 輸入 'menu' 查看選單
                        • AI 智能對話
                                                
                        如有任何問題，請隨時與我互動！
                        """)
                .build();
    }

    public Message about() {
        return TextMessage.builder()
                .text("""
                        🤖 NexusBot v2.0
                                                
                        我是您的智能助手，具備以下功能：
                        • AI 智能對話回應
                                                
                        技術支援：Spring Boot 3.4.3 + LINE Bot SDK 6.0.0
                        """)
                .build();
    }

    public Message success(String message) {
        return TextMessage.builder()
                .text("✅ " + message)
                .build();
    }

    public Message mainMenu() {
        return createFlexMenu(
                "NexusBot 功能選單",
                "請選擇一項功能開始操作",
                Arrays.asList(
                        createButton("🤖 AI 回應開關", "action=toggle_ai", PRIMARY_COLOR),
                        createButton("💊 用藥管理", "action=medication_menu", SUCCESS_COLOR),
                        createButton("❓ 說明與支援", "action=help_menu", INFO_COLOR)
                )
        );
    }

    public Message aiSettingsMenu() {
        return createFlexMenu(
                "AI 回應設定",
                "管理 AI 功能相關設定",
                Arrays.asList(
                        createButton("✅ 開啟 AI 回應", "action=enable_ai", SUCCESS_COLOR),
                        createButton("❌ 關閉 AI 回應", "action=disable_ai", ERROR_COLOR),
                        createButton("🔙 返回主選單", "action=main_menu", SECONDARY_COLOR)
                )
        );
    }

    public Message medicationMenu() {
        return createFlexMenu(
                "用藥管理系統",
                "管理您的用藥提醒與記錄",
                Arrays.asList(
                        createButton("📋 查看用藥清單", "action=view_medications", INFO_COLOR),
                        createButton("➕ 新增用藥提醒", "action=add_medication", SUCCESS_COLOR),
                        createButton("⏰ 設定提醒時間", "action=set_reminder", WARNING_COLOR),
                        createButton("🔙 返回主選單", "action=main_menu", SECONDARY_COLOR)
                )
        );
    }

    public Message helpMenu() {
        return createFlexMenu(
                "說明與支援",
                "瞭解如何使用 NexusBot",
                Arrays.asList(
                        createButton("ℹ️ 關於 NexusBot", "action=about", SECONDARY_COLOR),
                        createButton("🔙 返回主選單", "action=main_menu", SECONDARY_COLOR)
                )
        );
    }

    public String imageResponse(String messageId) {
        return "收到您的圖片！\n圖片ID: " + messageId;
    }

    public String stickerResponse(String packageId, String stickerId) {
        return String.format("很可愛的貼圖！😊\n貼圖包ID: %s\n貼圖ID: %s", packageId, stickerId);
    }

    public String videoResponse(String messageId) {
        return "收到您的影片！\n影片ID: " + messageId;
    }

    public String audioResponse(String messageId) {
        return "收到您的音檔！\n音檔ID: " + messageId;
    }

    public String fileResponse(String fileName, long fileSize) {
        return String.format("收到您的檔案！\n檔名: %s\n大小: %d bytes", fileName, fileSize);
    }

    public String locationResponse(String title, String address, double latitude, double longitude) {
        StringBuilder response = new StringBuilder("收到您的位置資訊！");
        if (title != null && !title.trim().isEmpty()) {
            response.append("\n地點名稱: ").append(title);
        }
        if (address != null && !address.trim().isEmpty()) {
            response.append("\n地址: ").append(address);
        }
        response.append(String.format("\n座標: %.6f, %.6f", latitude, longitude));
        return response.toString();
    }

    public Message postbackResponse(String data) {
        return TextMessage.builder()
                .text("按鈕點擊: " + data + "\n感謝您的互動！")
                .build();
    }

    public String unknownMessage() {
        return "收到您的訊息，但目前無法識別此類型。";
    }

    public String defaultTextResponse(String messageText) {
        return "我們已收到您的訊息：「" + messageText + "」\n輸入 menu 查看支援的指令。";
    }

    public String groupJoinMessage(String sourceType) {
        return "🎉 Hello everyone! I'm NexusBot!\nHappy to join this " +
                ("group".equals(sourceType) ? "group" : "room") + "!";
    }

    public String memberJoinedMessage(int memberCount) {
        return "🎊 Welcome new members!\n" + memberCount + " new friends joined the group!";
    }

    private FlexMessage createFlexMenu(String title, String subtitle, List<Button> buttons) {
        // 標題
        Text titleText = Text.builder()
                .text(title)
                .size(FlexFontSize.XL)
                .align(FlexAlign.CENTER)
                .color("#212121")
                .weight(Text.TextWeight.BOLD)
                .wrap(true)
                .build();

        // 副標題
        Text subtitleText = Text.builder()
                .text(subtitle)
                .size(FlexFontSize.SM)
                .color("#757575")
                .align(FlexAlign.CENTER)
                .wrap(true)
                .margin(FlexMarginSize.SM)
                .build();

        // 分隔線
        Separator separator = Separator.builder()
                .margin(FlexMarginSize.LG)
                .color("#E0E0E0")
                .build();

        // 組合按鈕和間隔
        List<FlexComponent> components = new java.util.ArrayList<>();
        components.add(titleText);
        components.add(subtitleText);
        components.add(separator);

        for (int i = 0; i < buttons.size(); i++) {
            components.add(buttons.get(i));
            if (i < buttons.size() - 1) {
                components.add(createSpacer());
            }
        }

        // 主容器
        Box mainBox = Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(components)
                .paddingAll(FlexPaddingSize.LG)
                .backgroundColor("#FFFFFF")
                .build();

        // Bubble 容器
        Bubble bubble = Bubble.builder()
                .body(mainBox)
                .build();

        return FlexMessage.builder()
                .altText(title)
                .contents(bubble)
                .build();
    }

    private Button createButton(String label, String action, String color) {
        return Button.builder()
                .style(Button.ButtonStyle.PRIMARY)
                .color(color)
                .action(PostbackAction.builder()
                        .label(label)
                        .data(action)
                        .displayText(label)
                        .build())
                .build();
    }

    private Box createSpacer() {
        return Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(Arrays.asList())
                .height("8px")
                .build();
    }
}