package com.acenexus.tata.nexusbot.template;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.acenexus.tata.nexusbot.constants.Actions.ABOUT;
import static com.acenexus.tata.nexusbot.constants.Actions.CLEAR_HISTORY;
import static com.acenexus.tata.nexusbot.constants.Actions.CONFIRM_CLEAR_HISTORY;
import static com.acenexus.tata.nexusbot.constants.Actions.DISABLE_AI;
import static com.acenexus.tata.nexusbot.constants.Actions.ENABLE_AI;
import static com.acenexus.tata.nexusbot.constants.Actions.HELP_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.MAIN_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_DEEPSEEK_R1;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_GEMMA2_9B;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_LLAMA3_70B;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_LLAMA_3_1_8B;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_LLAMA_3_3_70B;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_QWEN3_32B;
import static com.acenexus.tata.nexusbot.constants.Actions.REMINDER_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.SELECT_MODEL;
import static com.acenexus.tata.nexusbot.constants.Actions.TOGGLE_AI;
import static com.acenexus.tata.nexusbot.template.UIConstants.Colors;
import static com.acenexus.tata.nexusbot.template.UIConstants.Sizes;

@Service
public class MessageTemplateProviderImpl implements MessageTemplateProvider {

    public Message welcome() {
        return TextMessage.builder()
                .text("""
                        歡迎加入 NexusBot！
                                                
                        感謝您的支持，我將為您提供最佳的服務體驗。
                                                
                        可用功能：
                        - 輸入 'menu' 查看選單
                        - AI 智能對話
                                                
                        如有任何問題，請隨時與我互動！
                        """)
                .build();
    }

    public Message about() {
        return TextMessage.builder()
                .text("""
                        NexusBot v2.0
                                                
                        我是您的智能助手，具備以下功能：
                        - AI 智能對話回應
                                                
                        技術支援：Spring Boot 3.4.3 + LINE Bot SDK 6.0.0
                        """)
                .build();
    }

    public Message success(String message) {
        return TextMessage.builder()
                .text(message)
                .build();
    }

    public Message error(String message) {
        return TextMessage.builder()
                .text(message)
                .build();
    }

    public Message mainMenu() {
        return createFlexMenu(
                "NexusBot 功能選單",
                "請選擇一項功能開始操作",
                Arrays.asList(
                        createButton("🤖 AI 回應開關", TOGGLE_AI, Colors.PRIMARY),
                        createButton("📅 提醒功能", REMINDER_MENU, Colors.SUCCESS),
                        createButton("ℹ️ 說明與支援", HELP_MENU, Colors.INFO)
                )
        );
    }

    public Message aiSettingsMenu() {
        return aiSettingsMenu(false); // 預設狀態為關閉
    }

    public Message aiSettingsMenu(boolean currentStatus) {
        String statusText = currentStatus ? "目前狀態：已開啟" : "目前狀態：已關閉";

        return createFlexMenu(
                "AI 回應設定",
                "管理 AI 功能相關設定\n" + statusText,
                Arrays.asList(
                        createButton("開啟 AI 回應", ENABLE_AI, Colors.SUCCESS),
                        createButton("關閉 AI 回應", DISABLE_AI, Colors.ERROR),
                        createButton("選擇 AI 模型", SELECT_MODEL, Colors.INFO),
                        createButton("清除歷史對話", CLEAR_HISTORY, Colors.ERROR),
                        createButton("返回主選單", MAIN_MENU, Colors.SECONDARY)
                )
        );
    }

    @Override
    public Message aiModelSelectionMenu(String currentModel) {
        String modelDisplayName = getModelDisplayName(currentModel);

        return createFlexMenu(
                "AI 模型選擇",
                "選擇您偏好的 AI 模型\n目前使用：" + modelDisplayName,
                Arrays.asList(
                        createButton("Llama 3.1 8B (快速創意)", MODEL_LLAMA_3_1_8B, currentModel.equals("llama-3.1-8b-instant") ? Colors.SUCCESS : Colors.PRIMARY),
                        createButton("Llama 3.3 70B (精準強力)", MODEL_LLAMA_3_3_70B, currentModel.equals("llama-3.3-70b-versatile") ? Colors.SUCCESS : Colors.PRIMARY),
                        createButton("Llama 3 70B (詳細平衡)", MODEL_LLAMA3_70B, currentModel.equals("llama3-70b-8192") ? Colors.SUCCESS : Colors.PRIMARY),
                        createButton("Gemma2 9B (高度創意)", MODEL_GEMMA2_9B, currentModel.equals("gemma2-9b-it") ? Colors.SUCCESS : Colors.PRIMARY),
                        createButton("DeepSeek R1 (邏輯推理)", MODEL_DEEPSEEK_R1, currentModel.equals("deepseek-r1-distill-llama-70b") ? Colors.SUCCESS : Colors.PRIMARY),
                        createButton("Qwen3 32B (多語平衡)", MODEL_QWEN3_32B, currentModel.equals("qwen/qwen3-32b") ? Colors.SUCCESS : Colors.PRIMARY),
                        createButton("返回主選單", MAIN_MENU, Colors.SECONDARY)
                )
        );
    }

    public Message helpMenu() {
        return createFlexMenu(
                "說明與支援",
                "瞭解如何使用 NexusBot",
                Arrays.asList(
                        createButton("關於 NexusBot", ABOUT, Colors.INFO),
                        createButton("返回主選單", MAIN_MENU, Colors.SECONDARY)
                )
        );
    }

    public Message clearHistoryConfirmation() {
        return createFlexMenu(
                "確認清除歷史對話",
                "此動作將清除所有歷史對話記錄\n請確認是否繼續",
                Arrays.asList(
                        createButton("確認清除", CONFIRM_CLEAR_HISTORY, Colors.ERROR),
                        createButton("返回設定", TOGGLE_AI, Colors.SECONDARY)
                )
        );
    }

    public String imageResponse(String messageId) {
        return "收到您的圖片\n圖片ID: " + messageId;
    }

    public String stickerResponse(String packageId, String stickerId) {
        return String.format("很可愛的貼圖\n貼圖包ID: %s\n貼圖ID: %s", packageId, stickerId);
    }

    public String videoResponse(String messageId) {
        return "收到您的影片\n影片ID: " + messageId;
    }

    public String audioResponse(String messageId) {
        return "收到您的音檔\n音檔ID: " + messageId;
    }

    public String fileResponse(String fileName, long fileSize) {
        return String.format("收到您的檔案\n檔名: %s\n大小: %d bytes", fileName, fileSize);
    }

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
        return "Hello everyone! I'm NexusBot!\nHappy to join this " +
                ("group".equals(sourceType) ? "group" : "room") + "!";
    }

    public String memberJoinedMessage(int memberCount) {
        return "Welcome new members!\n" + memberCount + " new friends joined the group!";
    }

    @Override
    public Message systemStats(long totalRooms, long aiEnabledRooms, long adminRooms,
                               long totalMessages, long userMessages, long aiMessages,
                               long todayActiveRooms, long weekActiveRooms, String avgProcessingTime) {

        double aiEnabledPercent = totalRooms > 0 ? (aiEnabledRooms * 100.0 / totalRooms) : 0.0;

        return createStatsCard(
                "NexusBot 系統狀態",
                Arrays.asList(
                        createStatRow("■", "聊天室統計", String.format("總計：%,d 間\nAI啟用：%,d 間 (%.1f%%)\n管理員：%,d 間", totalRooms, aiEnabledRooms, aiEnabledPercent, adminRooms)),
                        createStatRow("●", "訊息統計", String.format("總計：%,d 條\n用戶：%,d 條\nAI回應：%,d 條", totalMessages, userMessages, aiMessages)),
                        createStatRow("▲", "活躍度", String.format("今日：%,d 間\n本週：%,d 間", todayActiveRooms, weekActiveRooms)),
                        createStatRow("◆", "AI 性能", "平均響應時間：" + avgProcessingTime)
                )
        );
    }

    private FlexMessage createFlexMenu(String title, String subtitle, List<Button> buttons) {
        // 標題
        Text titleText = Text.builder()
                .text(title)
                .size(FlexFontSize.XL)
                .align(FlexAlign.CENTER)
                .color(Colors.TEXT_PRIMARY)
                .weight(Text.TextWeight.BOLD)
                .wrap(true)
                .build();

        // 副標題
        Text subtitleText = Text.builder()
                .text(subtitle)
                .size(FlexFontSize.SM)
                .color(Colors.TEXT_SECONDARY)
                .align(FlexAlign.CENTER)
                .wrap(true)
                .margin(FlexMarginSize.SM)
                .build();

        // 分隔線
        Separator separator = Separator.builder()
                .margin(FlexMarginSize.LG)
                .color(Colors.SEPARATOR)
                .build();

        // 組合按鈕和間隔
        List<FlexComponent> components = new ArrayList<>();
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
                .backgroundColor(Colors.BACKGROUND)
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

    private String getModelDisplayName(String modelId) {
        return switch (modelId) {
            case "llama-3.1-8b-instant" -> "Llama 3.1 8B (快速創意)";
            case "llama-3.3-70b-versatile" -> "Llama 3.3 70B (精準強力)";
            case "llama3-70b-8192" -> "Llama 3 70B (詳細平衡)";
            case "gemma2-9b-it" -> "Gemma2 9B (高度創意)";
            case "deepseek-r1-distill-llama-70b" -> "DeepSeek R1 (邏輯推理)";
            case "qwen/qwen3-32b" -> "Qwen3 32B (多語平衡)";
            default -> modelId; // 如果找不到匹配，返回原始ID
        };
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
                .height(Sizes.SPACING_MD)
                .build();
    }

    private FlexMessage createStatsCard(String title, List<Box> statRows) {
        // 標題
        Text titleText = Text.builder()
                .text(title)
                .size(FlexFontSize.LG)
                .weight(Text.TextWeight.BOLD)
                .color(Colors.PRIMARY)
                .align(FlexAlign.CENTER)
                .build();

        // 建立內容容器
        List<FlexComponent> components = new ArrayList<>();
        components.add(titleText);
        components.add(createSpacer());
        components.addAll(statRows);

        // 主容器
        Box mainBox = Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(components)
                .paddingAll(FlexPaddingSize.LG)
                .backgroundColor(Colors.BACKGROUND)
                .spacing(FlexMarginSize.SM)
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

    private Box createStatRow(String icon, String category, String value) {
        // 標題行（圖示 + 類別）
        Text headerText = Text.builder()
                .text(icon + " " + category)
                .size(FlexFontSize.SM)
                .weight(Text.TextWeight.BOLD)
                .color(Colors.PRIMARY)
                .build();

        // 數值行
        Text valueText = Text.builder()
                .text(value)
                .size(FlexFontSize.XS)
                .color(Colors.SECONDARY)
                .wrap(true)
                .build();

        return Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(Arrays.asList(headerText, valueText))
                .spacing(FlexMarginSize.XS)
                .build();
    }

    public Message reminderMenu() {
        return createFlexMenu(
                "📅 提醒功能",
                "管理您的提醒設定",
                Arrays.asList(
                        createButton("🔙 返回主選單", MAIN_MENU, Colors.SECONDARY)
                )
        );
    }
}