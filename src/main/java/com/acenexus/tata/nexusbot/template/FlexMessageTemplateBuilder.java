package com.acenexus.tata.nexusbot.template;

import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.message.FlexMessage;
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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.acenexus.tata.nexusbot.template.UIConstants.BorderRadius;
import static com.acenexus.tata.nexusbot.template.UIConstants.Colors;
import static com.acenexus.tata.nexusbot.template.UIConstants.Spacing;
import static com.acenexus.tata.nexusbot.template.UIConstants.Status;

/**
 * Flex Message 範本建構器的抽象基礎類別
 * 提供通用的卡片建立、按鈕樣式、組件生成等共用方法
 */
public abstract class FlexMessageTemplateBuilder {

    /**
     * 按鈕類型枚舉
     */
    protected enum ButtonType {
        PRIMARY,    // 主要操作：開始使用、啟用功能等
        SECONDARY,  // 次要操作：返回、取消等
        SUCCESS,    // 成功操作：確認、完成等
        DANGER,     // 危險操作：刪除、清除等
        WARNING,    // 警告操作：需要謹慎的操作
        NEUTRAL,    // 中性操作：查看、選擇等
        NAVIGATE,   // 導航操作：返回主選單、功能說明等
        INFO        // 資訊操作：資訊查詢、說明等
    }

    /**
     * 建立專業風格的卡片
     */
    protected FlexMessage createCard(String title, String description, List<Button> buttons) {
        return createProfessionalCard(title, title, description, buttons, false);
    }

    /**
     * 建立專業風格的卡片（完整參數）
     */
    protected FlexMessage createProfessionalCard(String altText, String title, String description,
                                                 List<Button> buttons, boolean isHighlight) {
        List<FlexComponent> components = new ArrayList<>();

        // 標題區塊
        components.add(createTitleComponent(title));

        // 描述區塊
        if (description != null && !description.trim().isEmpty()) {
            components.add(createDescriptionComponent(description));
        }

        // 按鈕區塊
        if (buttons != null && !buttons.isEmpty()) {
            components.add(createSeparator());
            components.add(createButtonContainer(buttons));
        }

        // 主容器
        Box mainBox = Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(components)
                .paddingAll(FlexPaddingSize.LG)
                .backgroundColor(Colors.BACKGROUND)
                .spacing(FlexMarginSize.MD)
                .build();

        Bubble bubble = Bubble.builder()
                .body(mainBox)
                .build();

        return FlexMessage.builder()
                .altText(altText)
                .contents(bubble)
                .build();
    }

    /**
     * 建立狀態卡片（成功、錯誤、警告等）
     */
    protected FlexMessage createStatusCard(String title, String message, String statusColor) {
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

    // ==================== 組件建立方法 ====================

    /**
     * 建立標題組件
     */
    protected Text createTitleComponent(String title) {
        return Text.builder()
                .text(title)
                .size(FlexFontSize.XL)
                .weight(Text.TextWeight.BOLD)
                .color(Colors.TEXT_PRIMARY)
                .wrap(true)
                .build();
    }

    /**
     * 建立描述組件
     */
    protected Text createDescriptionComponent(String description) {
        return Text.builder()
                .text(description)
                .size(FlexFontSize.SM)
                .color(Colors.TEXT_SECONDARY)
                .wrap(true)
                .margin(FlexMarginSize.LG)
                .build();
    }

    /**
     * 建立分隔線
     */
    protected Separator createSeparator() {
        return Separator.builder()
                .margin(FlexMarginSize.LG)
                .color(Colors.BORDER)
                .build();
    }

    /**
     * 建立按鈕容器
     */
    protected Box createButtonContainer(List<Button> buttons) {
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

    /**
     * 建立間距組件
     */
    protected Box createSpacing(String height) {
        return Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(Arrays.asList())
                .height(height)
                .build();
    }

    // ==================== 按鈕建立方法 ====================

    /**
     * 建立按鈕（通用方法）
     */
    protected Button createButton(String label, String action, ButtonType type) {
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

    /**
     * 建立帶 flex 屬性的按鈕
     */
    protected Button createButtonWithFlex(String label, String action, ButtonType type, int flex) {
        var buttonStyle = getButtonStyle(type);
        var buttonColor = getButtonColor(type);

        return Button.builder()
                .style(buttonStyle)
                .color(buttonColor)
                .flex(flex)
                .action(PostbackAction.builder()
                        .label(label)
                        .data(action)
                        .displayText(label)
                        .build())
                .build();
    }

    /**
     * 建立選擇按鈕（已選擇/未選擇狀態）
     */
    protected Button createSelectionButton(String label, String action, boolean isSelected) {
        String displayLabel = isSelected ? "✓ " + label : label;
        String buttonColor = isSelected ? UIConstants.Button.SELECTED : UIConstants.Button.UNSELECTED;
        Button.ButtonStyle style = isSelected ? Button.ButtonStyle.PRIMARY : Button.ButtonStyle.SECONDARY;

        return Button.builder()
                .style(style)
                .color(buttonColor)
                .action(PostbackAction.builder()
                        .label(displayLabel)
                        .data(action)
                        .displayText(label + (isSelected ? " (已選擇)" : ""))
                        .build())
                .build();
    }

    /**
     * 建立主要按鈕
     */
    protected Button createPrimaryButton(String label, String action) {
        return createButton(label, action, ButtonType.PRIMARY);
    }

    /**
     * 建立次要按鈕
     */
    protected Button createSecondaryButton(String label, String action) {
        return createButton(label, action, ButtonType.SECONDARY);
    }

    /**
     * 建立成功按鈕
     */
    protected Button createSuccessButton(String label, String action) {
        return createButton(label, action, ButtonType.SUCCESS);
    }

    /**
     * 建立危險按鈕
     */
    protected Button createDangerButton(String label, String action) {
        return createButton(label, action, ButtonType.DANGER);
    }

    /**
     * 建立導航按鈕
     */
    protected Button createNavigateButton(String label, String action) {
        return createButton(label, action, ButtonType.NAVIGATE);
    }

    /**
     * 建立中性按鈕
     */
    protected Button createNeutralButton(String label, String action) {
        return createButton(label, action, ButtonType.NEUTRAL);
    }

    /**
     * 建立警告按鈕
     */
    protected Button createWarningButton(String label, String action) {
        return createButton(label, action, ButtonType.WARNING);
    }

    /**
     * 建立 URI 按鈕（用於開啟網頁連結）
     */
    protected Button createUriButton(String label, String uriString, ButtonType type) {
        var buttonStyle = getButtonStyle(type);
        var buttonColor = getButtonColor(type);

        try {
            URI uri = URI.create(uriString);
            return Button.builder()
                    .style(buttonStyle)
                    .color(buttonColor)
                    .action(new URIAction(label, uri, null))
                    .build();
        } catch (IllegalArgumentException e) {
            return Button.builder()
                    .style(buttonStyle)
                    .color(buttonColor)
                    .action(PostbackAction.builder()
                            .label(label)
                            .data("invalid_uri")
                            .displayText("無效的連結")
                            .build())
                    .build();
        }
    }

    /**
     * 建立主要 URI 按鈕
     */
    protected Button createPrimaryUriButton(String label, String uri) {
        return createUriButton(label, uri, ButtonType.PRIMARY);
    }

    // ==================== 樣式工具方法 ====================

    /**
     * 取得按鈕樣式
     */
    private Button.ButtonStyle getButtonStyle(ButtonType type) {
        return switch (type) {
            case PRIMARY, SUCCESS, DANGER, WARNING, INFO -> Button.ButtonStyle.SECONDARY;
            case SECONDARY, NEUTRAL, NAVIGATE -> Button.ButtonStyle.SECONDARY;
        };
    }

    /**
     * 取得按鈕顏色
     */
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

    /**
     * 取得狀態背景色
     */
    private String getStatusBackground(String statusColor) {
        return switch (statusColor) {
            case "#059669", "#10B981" -> Status.SUCCESS_BACKGROUND;
            case "#D97706", "#F59E0B" -> Status.WARNING_BACKGROUND;
            case "#DC2626", "#EF4444" -> Status.ERROR_BACKGROUND;
            case "#0EA5E9", "#06B6D4" -> Status.INFO_BACKGROUND;
            default -> Colors.BACKGROUND;
        };
    }

    /**
     * 取得狀態邊框色
     */
    private String getStatusBorderColor(String statusColor) {
        return switch (statusColor) {
            case "#059669", "#10B981" -> Status.SUCCESS_BORDER;
            case "#D97706", "#F59E0B" -> Status.WARNING_BORDER;
            case "#DC2626", "#EF4444" -> Status.ERROR_BORDER;
            case "#0EA5E9", "#06B6D4" -> Status.INFO_BORDER;
            default -> Colors.BORDER;
        };
    }
}
