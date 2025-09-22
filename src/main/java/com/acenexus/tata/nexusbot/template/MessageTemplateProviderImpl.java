package com.acenexus.tata.nexusbot.template;

import com.acenexus.tata.nexusbot.entity.Reminder;
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
import com.linecorp.bot.model.message.flex.unit.FlexFontSize;
import com.linecorp.bot.model.message.flex.unit.FlexLayout;
import com.linecorp.bot.model.message.flex.unit.FlexMarginSize;
import com.linecorp.bot.model.message.flex.unit.FlexPaddingSize;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.acenexus.tata.nexusbot.constants.Actions.ABOUT;
import static com.acenexus.tata.nexusbot.constants.Actions.ADD_REMINDER;
import static com.acenexus.tata.nexusbot.constants.Actions.CANCEL_REMINDER_INPUT;
import static com.acenexus.tata.nexusbot.constants.Actions.CLEAR_HISTORY;
import static com.acenexus.tata.nexusbot.constants.Actions.CONFIRM_CLEAR_HISTORY;
import static com.acenexus.tata.nexusbot.constants.Actions.DISABLE_AI;
import static com.acenexus.tata.nexusbot.constants.Actions.ENABLE_AI;
import static com.acenexus.tata.nexusbot.constants.Actions.HELP_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.LIST_REMINDERS;
import static com.acenexus.tata.nexusbot.constants.Actions.MAIN_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_DEEPSEEK_R1;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_GEMMA2_9B;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_LLAMA3_70B;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_LLAMA_3_1_8B;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_LLAMA_3_3_70B;
import static com.acenexus.tata.nexusbot.constants.Actions.MODEL_QWEN3_32B;
import static com.acenexus.tata.nexusbot.constants.Actions.REMINDER_COMPLETED;
import static com.acenexus.tata.nexusbot.constants.Actions.REMINDER_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.REPEAT_DAILY;
import static com.acenexus.tata.nexusbot.constants.Actions.REPEAT_ONCE;
import static com.acenexus.tata.nexusbot.constants.Actions.REPEAT_WEEKLY;
import static com.acenexus.tata.nexusbot.constants.Actions.SELECT_MODEL;
import static com.acenexus.tata.nexusbot.constants.Actions.TOGGLE_AI;
import static com.acenexus.tata.nexusbot.constants.TimeFormatters.STANDARD_TIME;
import static com.acenexus.tata.nexusbot.template.UIConstants.BorderRadius;
import static com.acenexus.tata.nexusbot.template.UIConstants.Colors;
import static com.acenexus.tata.nexusbot.template.UIConstants.Spacing;
import static com.acenexus.tata.nexusbot.template.UIConstants.Status;

@Service
public class MessageTemplateProviderImpl implements MessageTemplateProvider {

    @Override
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

    @Override
    public Message about() {
        return createCard(
                "NexusBot v2.0",
                "專業 AI 智能助手平台\n\n核心功能包括智能對話、提醒管理、多模型支援等服務。採用現代化架構設計，提供穩定可靠的使用體驗。",
                Arrays.asList(
                        createNavigateButton("返回主選單", MAIN_MENU)
                )
        );
    }

    @Override
    public Message success(String message) {
        return createStatusCard("操作成功", message, Colors.SUCCESS);
    }

    @Override
    public Message error(String message) {
        return createStatusCard("操作失敗", message, Colors.ERROR);
    }

    @Override
    public Message mainMenu() {
        return createCard(
                "功能選單",
                "請選擇您需要使用的功能服務",
                Arrays.asList(
                        createPrimaryButton("AI 智能對話", TOGGLE_AI),
                        createNeutralButton("提醒管理", REMINDER_MENU),
                        createNeutralButton("說明與支援", HELP_MENU)
                )
        );
    }

    @Override
    public Message aiSettingsMenu(boolean currentStatus) {
        String title = currentStatus ? "AI 智能對話已啟用" : "AI 智能對話已停用";
        String description = currentStatus
                ? "系統將自動回應您的訊息並提供智能助手服務。您可以隨時調整設定或選擇不同的 AI 模型。"
                : "目前為手動模式，系統不會自動回應訊息。您可以啟用 AI 功能來獲得智能助手服務。";

        List<Button> buttons = new ArrayList<>();
        if (currentStatus) {
            buttons.add(createWarningButton("停用 AI", DISABLE_AI));
            buttons.add(createNeutralButton("選擇模型", SELECT_MODEL));
            buttons.add(createDangerButton("清除歷史", CLEAR_HISTORY));
        } else {
            buttons.add(createPrimaryButton("啟用 AI", ENABLE_AI));
            buttons.add(createNeutralButton("選擇模型", SELECT_MODEL));
        }
        buttons.add(createNavigateButton("返回主選單", MAIN_MENU));

        return createCard(title, description, buttons);
    }

    @Override
    public Message aiModelSelectionMenu(String currentModel) {
        String modelDisplayName = getModelDisplayName(currentModel);

        return createCard(
                "AI 模型選擇",
                "目前使用：" + modelDisplayName + "\n\n請選擇您希望使用的 AI 模型",
                Arrays.asList(
                        createModelButton("Llama 3.1 8B", "快速回應", MODEL_LLAMA_3_1_8B, currentModel.equals("llama-3.1-8b-instant")),
                        createModelButton("Llama 3.3 70B", "高精度回應", MODEL_LLAMA_3_3_70B, currentModel.equals("llama-3.3-70b-versatile")),
                        createModelButton("Llama 3 70B", "平衡性能", MODEL_LLAMA3_70B, currentModel.equals("llama3-70b-8192")),
                        createModelButton("Gemma2 9B", "創意對話", MODEL_GEMMA2_9B, currentModel.equals("gemma2-9b-it")),
                        createModelButton("DeepSeek R1", "邏輯推理", MODEL_DEEPSEEK_R1, currentModel.equals("deepseek-r1-distill-llama-70b")),
                        createModelButton("Qwen3 32B", "中文優化", MODEL_QWEN3_32B, currentModel.equals("qwen/qwen3-32b")),
                        createNavigateButton("返回 AI 設定", TOGGLE_AI),
                        createNavigateButton("返回主選單", MAIN_MENU)
                )
        );
    }

    @Override
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

    @Override
    public Message clearHistoryConfirmation() {
        return createCard(
                "清除對話歷史",
                "確認要清除所有 AI 對話記錄嗎？\n\n此操作將永久刪除所有對話內容、AI 學習記錄及聊天上下文。\n\n請注意：此操作無法復原。",
                Arrays.asList(
                        createDangerButton("確認清除", CONFIRM_CLEAR_HISTORY),
                        createPrimaryButton("取消操作", TOGGLE_AI)
                )
        );
    }

    @Override
    public String imageResponse(String messageId) {
        return "收到您的圖片\n圖片ID: " + messageId;
    }

    @Override
    public String stickerResponse(String packageId, String stickerId) {
        return String.format("很可愛的貼圖\n貼圖包ID: %s\n貼圖ID: %s", packageId, stickerId);
    }

    @Override
    public String videoResponse(String messageId) {
        return "收到您的影片\n影片ID: " + messageId;
    }

    @Override
    public String audioResponse(String messageId) {
        return "收到您的音檔\n音檔ID: " + messageId;
    }

    @Override
    public String fileResponse(String fileName, long fileSize) {
        return String.format("收到您的檔案\n檔名: %s\n大小: %d bytes", fileName, fileSize);
    }

    @Override
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

    @Override
    public Message postbackResponse(String data) {
        return TextMessage.builder()
                .text("按鈕點擊: " + data + "\n感謝您的互動！")
                .build();
    }

    @Override
    public String unknownMessage() {
        return "收到您的訊息，但目前無法識別此類型。";
    }

    @Override
    public String defaultTextResponse(String messageText) {
        return "我們已收到您的訊息：「" + messageText + "」\n輸入 menu 查看支援的指令。";
    }

    @Override
    public String groupJoinMessage(String sourceType) {
        return "Hello everyone! I'm NexusBot!\nHappy to join this " +
                ("group".equals(sourceType) ? "group" : "room") + "!";
    }

    @Override
    public String memberJoinedMessage(int memberCount) {
        return "Welcome new members!\n" + memberCount + " new friends joined the group!";
    }

    @Override
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

    @Override
    public Message reminderMenu() {
        return createCard(
                "提醒管理",
                "智慧提醒管理系統可以幫助您設定重要事項的提醒通知。支援單次提醒和週期性提醒設定。",
                Arrays.asList(
                        createPrimaryButton("新增提醒", ADD_REMINDER),
                        createNeutralButton("檢視提醒列表", LIST_REMINDERS),
                        createNavigateButton("返回主選單", MAIN_MENU)
                )
        );
    }

    @Override
    public Message reminderRepeatTypeMenu() {
        return createCard(
                "提醒頻率設定",
                "請選擇提醒的重複頻率。單次提醒適合一次性事件，重複提醒適合定期任務。",
                Arrays.asList(
                        createPrimaryButton("單次提醒", REPEAT_ONCE),
                        createNeutralButton("每日提醒", REPEAT_DAILY),
                        createNeutralButton("每週提醒", REPEAT_WEEKLY),
                        createSecondaryButton("取消設定", CANCEL_REMINDER_INPUT)
                )
        );
    }

    @Override
    public Message reminderInputMenu(String step) {
        String title = step.equals("time") ? "設定提醒時間" : "設定提醒內容";
        String description = step.equals("time") ?
                "請輸入提醒時間。您可以使用標準格式（例如：2025-01-01 13:00）或自然語言（例如：明天下午三點、30分鐘後）。" :
                "請輸入提醒內容。簡潔描述您需要被提醒的事項，例如：服藥、會議、運動等。";

        return createCard(title, description, Arrays.asList(
                createSecondaryButton("取消設定", CANCEL_REMINDER_INPUT),
                createNavigateButton("返回提醒管理", REMINDER_MENU)
        ));
    }

    @Override
    public Message reminderInputMenu(String step, String reminderTime) {
        String title = step.equals("time") ? "設定提醒時間" : "設定提醒內容";
        String description = step.equals("time") ?
                "請輸入提醒時間。您可以使用標準格式（例如：2025-01-01 13:00）或自然語言（例如：明天下午三點、30分鐘後）。" :
                "提醒時間已設定：" + reminderTime + "\n\n請輸入提醒內容。簡潔描述您需要被提醒的事項。";

        return createCard(title, description, Arrays.asList(
                createSecondaryButton("取消設定", CANCEL_REMINDER_INPUT),
                createNavigateButton("返回提醒管理", REMINDER_MENU)
        ));
    }

    @Override
    public Message reminderCreatedSuccess(String reminderTime, String repeatType, String content) {
        String description = String.format(
                "提醒已成功建立：\n\n時間：%s\n頻率：%s\n內容：%s\n\n系統將在指定時間發送提醒通知。",
                reminderTime, repeatType, content);

        return createCard("提醒建立成功", description, Arrays.asList(
                createSuccessButton("返回提醒管理", REMINDER_MENU),
                createNavigateButton("返回主選單", MAIN_MENU)
        ));
    }

    @Override
    public Message reminderInputError(String userInput, String aiResult) {
        String description = String.format(
                "無法解析您輸入的時間格式：\n\n您的輸入：%s\n系統解析：%s\n\n請使用正確的時間格式，例如：\n• 標準格式：2025-01-01 15:00\n• 自然語言：明天下午3點\n• 相對時間：30分鐘後",
                userInput, aiResult);

        return createCard("時間格式錯誤", description, Arrays.asList(
                createPrimaryButton("重新設定", ADD_REMINDER),
                createSecondaryButton("返回提醒管理", REMINDER_MENU)
        ));
    }

    @Override
    public Message reminderList(List<Reminder> reminders, Map<Long, String> userResponseStatuses) {
        if (reminders.isEmpty()) {
            return createCard(
                    "提醒列表",
                    "目前沒有任何提醒",
                    Arrays.asList(
                            createPrimaryButton("新增提醒", ADD_REMINDER),
                            createSecondaryButton("返回提醒管理", REMINDER_MENU)
                    )
            );
        }

        // 建構提醒列表內容
        StringBuilder contentBuilder = new StringBuilder();
        for (int i = 0; i < reminders.size(); i++) {
            Reminder reminder = reminders.get(i);
            String repeatTypeText = switch (reminder.getRepeatType()) {
                case "DAILY" -> "每日重複";
                case "WEEKLY" -> "每週重複";
                default -> "僅一次";
            };

            String userStatus = userResponseStatuses.getOrDefault(reminder.getId(), "無回應");
            String statusDisplay = "COMPLETED".equals(userStatus) ? "已執行" : "無回應";

            contentBuilder.append(String.format(
                    "%d. %s\n時間：%s\n頻率：%s\n狀態：%s\n\n",
                    i + 1,
                    reminder.getContent(),
                    reminder.getReminderTime().format(STANDARD_TIME),
                    repeatTypeText,
                    statusDisplay
            ));
        }

        return createCard("提醒列表", contentBuilder.toString(), Arrays.asList(
                createPrimaryButton("新增提醒", ADD_REMINDER),
                createSecondaryButton("返回提醒管理", REMINDER_MENU)
        ));
    }

    @Override
    public Message buildReminderNotification(String enhancedContent, String originalContent, String repeatType, Long reminderId) {
        String repeatDescription = switch (repeatType.toUpperCase()) {
            case "DAILY" -> "每日提醒";
            case "WEEKLY" -> "每週提醒";
            case "ONCE" -> "一次性提醒";
            default -> "提醒";
        };

        String currentTime = java.time.LocalDateTime.now().format(STANDARD_TIME);

        String description = String.format(
                "%s\n\n類型：%s\n時間：%s\n\n請確認您是否已執行此提醒。",
                enhancedContent, repeatDescription, currentTime
        );

        return createCard("提醒時間到了", description, Arrays.asList(
                createSuccessButton("確認已執行", REMINDER_COMPLETED + "&id=" + reminderId)
        ));
    }

    private FlexMessage createCard(String title, String description, List<Button> buttons) {
        return createProfessionalCard(title, title, description, buttons, false);
    }

    private FlexMessage createProfessionalCard(String altText, String title, String description,
                                               List<Button> buttons, boolean isHighlight) {
        List<FlexComponent> components = new ArrayList<>();

        // 標題區塊
        components.add(createTitleComponent(title));

        // 描述區塊
        components.add(createDescriptionComponent(description));

        // 按鈕區塊
        if (!buttons.isEmpty()) {
            components.add(createSeparator());
            components.add(createButtonContainer(buttons));
        }

        // 主容器
        Box mainBox = Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(components)
                .paddingAll(FlexPaddingSize.LG)
                .backgroundColor(isHighlight ? Colors.PRIMARY_LIGHT : Colors.BACKGROUND)
                .spacing(FlexMarginSize.SM)
                .cornerRadius(BorderRadius.MD)
                .build();

        Bubble bubble = Bubble.builder()
                .body(mainBox)
                .build();

        return FlexMessage.builder()
                .altText(altText)
                .contents(bubble)
                .build();
    }

    private Text createTitleComponent(String title) {
        return Text.builder()
                .text(title)
                .size(FlexFontSize.LG)
                .weight(Text.TextWeight.BOLD)
                .color(Colors.TEXT_PRIMARY)
                .wrap(true)
                .build();
    }

    private Text createDescriptionComponent(String description) {
        return Text.builder()
                .text(description)
                .size(FlexFontSize.SM)
                .color(Colors.TEXT_SECONDARY)
                .wrap(true)
                .margin(FlexMarginSize.MD)
                .build();
    }

    private Separator createSeparator() {
        return Separator.builder()
                .margin(FlexMarginSize.LG)
                .color(Colors.BORDER)
                .build();
    }

    private Box createButtonContainer(List<Button> buttons) {
        List<FlexComponent> buttonComponents = new ArrayList<>();

        for (int i = 0; i < buttons.size(); i++) {
            if (i > 0) {
                buttonComponents.add(createSpacing(Spacing.SM));
            }
            buttonComponents.add(buttons.get(i));
        }

        return Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(buttonComponents)
                .paddingTop(FlexPaddingSize.MD)
                .build();
    }

    private Box createSpacing(String height) {
        return Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(Arrays.asList())
                .height(height)
                .build();
    }


    private FlexMessage createStatusCard(String title, String message, String statusColor) {
        // 確定狀態類型和對應的背景色
        String backgroundColor = getStatusBackground(statusColor);
        String borderColor = getStatusBorderColor(statusColor);

        // 狀態圖示區域
        Box statusIcon = Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(Arrays.asList())
                .width("4px")
                .backgroundColor(borderColor)
                .cornerRadius(BorderRadius.SM)
                .build();

        // 標題文字
        Text titleText = Text.builder()
                .text(title)
                .size(FlexFontSize.LG)
                .weight(Text.TextWeight.BOLD)
                .color(Colors.TEXT_PRIMARY)
                .wrap(true)
                .build();

        // 訊息內容
        Text messageText = Text.builder()
                .text(message)
                .size(FlexFontSize.SM)
                .color(Colors.TEXT_SECONDARY)
                .wrap(true)
                .margin(FlexMarginSize.SM)
                .build();

        // 內容區域
        Box contentArea = Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(Arrays.asList(titleText, messageText))
                .flex(1)
                .paddingStart(FlexPaddingSize.MD)
                .build();

        // 主容器
        Box mainContainer = Box.builder()
                .layout(FlexLayout.HORIZONTAL)
                .contents(Arrays.asList(statusIcon, contentArea))
                .spacing(FlexMarginSize.MD)
                .paddingAll(FlexPaddingSize.LG)
                .backgroundColor(backgroundColor)
                .cornerRadius(BorderRadius.MD)
                .build();

        Bubble bubble = Bubble.builder()
                .body(mainContainer)
                .build();

        return FlexMessage.builder()
                .altText(title)
                .contents(bubble)
                .build();
    }

    private String getStatusBackground(String statusColor) {
        return switch (statusColor) {
            case "#059669", "#10B981" -> Status.SUCCESS_BACKGROUND;
            case "#D97706", "#F59E0B" -> Status.WARNING_BACKGROUND;
            case "#DC2626", "#EF4444" -> Status.ERROR_BACKGROUND;
            case "#0EA5E9", "#06B6D4" -> Status.INFO_BACKGROUND;
            default -> Colors.BACKGROUND;
        };
    }

    private String getStatusBorderColor(String statusColor) {
        return switch (statusColor) {
            case "#059669", "#10B981" -> Status.SUCCESS_BORDER;
            case "#D97706", "#F59E0B" -> Status.WARNING_BORDER;
            case "#DC2626", "#EF4444" -> Status.ERROR_BORDER;
            case "#0EA5E9", "#06B6D4" -> Status.INFO_BORDER;
            default -> Colors.BORDER;
        };
    }

    enum ButtonType {
        PRIMARY,    // 主要操作：開始使用、啟用功能等
        SECONDARY,  // 次要操作：返回、取消等
        SUCCESS,    // 成功操作：磺認、完成等
        DANGER,     // 危險操作：刪除、清除等
        WARNING,    // 警告操作：需要謹慎的操作
        NEUTRAL,    // 中性操作：查看、選擇等
        NAVIGATE,   // 導航操作：返回主選單、功能說明等
        INFO        // 資訊操作：資訊查詢、說明等
    }

    private Button createButton(String label, String action, ButtonType type) {
        var buttonStyle = getButtonStyle(type);
        var buttonColor = getButtonColor(type);

        return Button.builder()
                .style(buttonStyle)
                .color(buttonColor)
                .action(PostbackAction.builder()
                        .label(label)
                        .data(action)
                        .displayText(label)
                        .build())
                .build();
    }

    private Button createSelectionButton(String label, String action, boolean isSelected) {
        String displayLabel = isSelected ? "\u2713 " + label : label;
        String buttonColor = isSelected ? UIConstants.Button.SELECTED : UIConstants.Button.UNSELECTED;
        Button.ButtonStyle style = isSelected ? Button.ButtonStyle.PRIMARY : Button.ButtonStyle.SECONDARY;

        return Button.builder()
                .style(style)
                .color(buttonColor)
                .action(PostbackAction.builder()
                        .label(displayLabel)
                        .data(action)
                        .displayText(label)
                        .build())
                .build();
    }

    private Button createPrimaryButton(String label, String action) {
        return createButton(label, action, ButtonType.PRIMARY);
    }

    private Button createSecondaryButton(String label, String action) {
        return createButton(label, action, ButtonType.SECONDARY);
    }

    private Button createSuccessButton(String label, String action) {
        return createButton(label, action, ButtonType.SUCCESS);
    }

    private Button createDangerButton(String label, String action) {
        return createButton(label, action, ButtonType.DANGER);
    }

    private Button createNavigateButton(String label, String action) {
        return createButton(label, action, ButtonType.NAVIGATE);
    }

    private Button createNeutralButton(String label, String action) {
        return createButton(label, action, ButtonType.NEUTRAL);
    }

    private Button createWarningButton(String label, String action) {
        return createButton(label, action, ButtonType.WARNING);
    }

    private Button createModelButton(String name, String description, String action, boolean isSelected) {
        return createSelectionButton(name, action, isSelected);
    }

    private Button.ButtonStyle getButtonStyle(ButtonType type) {
        return switch (type) {
            case PRIMARY, SUCCESS, DANGER, WARNING, INFO -> Button.ButtonStyle.SECONDARY;
            case SECONDARY, NEUTRAL, NAVIGATE -> Button.ButtonStyle.SECONDARY;
        };
    }

    private String getButtonColor(ButtonType type) {
        return switch (type) {
            case PRIMARY -> UIConstants.Button.PRIMARY;
            case SUCCESS -> UIConstants.Button.SUCCESS;
            case DANGER -> UIConstants.Button.DANGER;
            case WARNING -> UIConstants.Button.WARNING;
            case INFO -> UIConstants.Button.INFO;
            case SECONDARY, NEUTRAL, NAVIGATE -> UIConstants.Button.SECONDARY;
        };
    }

    private String getModelDisplayName(String modelId) {
        return switch (modelId) {
            case "llama-3.1-8b-instant" -> "Llama 3.1 8B";
            case "llama-3.3-70b-versatile" -> "Llama 3.3 70B";
            case "llama3-70b-8192" -> "Llama 3 70B";
            case "gemma2-9b-it" -> "Gemma2 9B";
            case "deepseek-r1-distill-llama-70b" -> "DeepSeek R1";
            case "qwen/qwen3-32b" -> "Qwen3 32B";
            default -> modelId;
        };
    }
}