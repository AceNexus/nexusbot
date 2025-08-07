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

import java.util.Arrays;
import java.util.List;

import static com.acenexus.tata.nexusbot.constants.Actions.ABOUT;
import static com.acenexus.tata.nexusbot.constants.Actions.DISABLE_AI;
import static com.acenexus.tata.nexusbot.constants.Actions.ENABLE_AI;
import static com.acenexus.tata.nexusbot.constants.Actions.HELP_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.MAIN_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.TOGGLE_AI;
import static com.acenexus.tata.nexusbot.template.UIConstants.Colors;
import static com.acenexus.tata.nexusbot.template.UIConstants.Icons;
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
                        createButton(Icons.AI + " AI 回應開關", TOGGLE_AI, Colors.PRIMARY),
                        createButton(Icons.INFO + " 說明與支援", HELP_MENU, Colors.INFO)
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
                        createButton(Icons.SUCCESS + " 開啟 AI 回應", ENABLE_AI, Colors.SUCCESS),
                        createButton(Icons.ERROR + " 關閉 AI 回應", DISABLE_AI, Colors.ERROR),
                        createButton(Icons.HOME + " 返回主選單", MAIN_MENU, Colors.SECONDARY)
                )
        );
    }

    public Message helpMenu() {
        return createFlexMenu(
                "說明與支援",
                "瞭解如何使用 NexusBot",
                Arrays.asList(
                        createButton(Icons.ABOUT + " 關於 NexusBot", ABOUT, Colors.INFO),
                        createButton(Icons.HOME + " 返回主選單", MAIN_MENU, Colors.SECONDARY)
                )
        );
    }

    public String imageResponse(String messageId) {
        return Icons.IMAGE + " 收到您的圖片\n圖片ID: " + messageId;
    }

    public String stickerResponse(String packageId, String stickerId) {
        return String.format(Icons.STICKER + " 很可愛的貼圖\n貼圖包ID: %s\n貼圖ID: %s", packageId, stickerId);
    }

    public String videoResponse(String messageId) {
        return Icons.VIDEO + " 收到您的影片\n影片ID: " + messageId;
    }

    public String audioResponse(String messageId) {
        return Icons.AUDIO + " 收到您的音檔\n音檔ID: " + messageId;
    }

    public String fileResponse(String fileName, long fileSize) {
        return String.format(Icons.FILE + " 收到您的檔案\n檔名: %s\n大小: %d bytes", fileName, fileSize);
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

    private FlexMessage createFlexMenu(String title, String subtitle, List<Button> buttons) {
        // 標題
        Text titleText = Text.builder()
                .text(Icons.STAR + " " + title)
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
}