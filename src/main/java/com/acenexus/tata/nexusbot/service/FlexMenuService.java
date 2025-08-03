package com.acenexus.tata.nexusbot.service;

import com.linecorp.bot.model.action.PostbackAction;
import com.linecorp.bot.model.message.FlexMessage;
import com.linecorp.bot.model.message.Message;
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
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlexMenuService {
    private static final Logger logger = LoggerFactory.getLogger(FlexMenuService.class);

    private static final class ColorTheme {
        static final String PRIMARY = "#1976D2";
        static final String INFO = "#0288D1";
        static final String TEXT_PRIMARY = "#212121";
        static final String TEXT_SECONDARY = "#757575";
        static final String SEPARATOR = "#E0E0E0";
        static final String CARD = "#FFFFFF";
        static final String SUCCESS = "#4CAF50";
        static final String WARNING = "#FF9800";
    }

    /**
     * 創建主功能選單
     */
    public Message createMenuFlexMessage() {
        // 標題
        Text title = Text.builder()
                .text("NexusBot 功能選單")
                .size(FlexFontSize.XL)
                .align(FlexAlign.CENTER)
                .color(ColorTheme.TEXT_PRIMARY)
                .wrap(true)
                .build();

        // 副標題
        Text subtitle = Text.builder()
                .text("請選擇一項功能開始操作")
                .size(FlexFontSize.SM)
                .color(ColorTheme.TEXT_SECONDARY)
                .align(FlexAlign.CENTER)
                .wrap(true)
                .margin(FlexMarginSize.SM)
                .build();

        // 分隔線
        Separator separator = Separator.builder()
                .margin(FlexMarginSize.LG)
                .color(ColorTheme.SEPARATOR)
                .build();

        // 頭部容器
        Box headerBox = Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(Arrays.asList(title, subtitle, separator))
                .paddingAll(FlexPaddingSize.LG)
                .backgroundColor(ColorTheme.CARD)
                .build();

        // 功能按鈕
        List<FlexComponent> buttons = createMenuButtons();

        // 按鈕容器
        Box buttonBox = Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(buttons)
                .paddingAll(FlexPaddingSize.LG)
                .backgroundColor(ColorTheme.CARD)
                .build();

        // 主容器
        Box mainBox = Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(Arrays.asList(headerBox, buttonBox))
                .build();

        // Bubble 容器
        Bubble bubble = Bubble.builder()
                .body(mainBox)
                .build();

        FlexMessage flexMessage = FlexMessage.builder()
                .altText("NexusBot 功能選單")
                .contents(bubble)
                .build();

        logger.info("Created flex menu message");
        return flexMessage;
    }

    private List<FlexComponent> createMenuButtons() {
        return Arrays.asList(
                createButton("🤖 AI 回應開關", "action=toggle_ai", ColorTheme.PRIMARY, "功能選單：切換 AI 回應開關"),
                createSpacer(),
                createButton("💊 用藥管理", "action=medication_menu", ColorTheme.SUCCESS, "功能選單：用藥管理"),
                createSpacer()
        );
    }

    /**
     * 創建功能按鈕
     */
    private Button createButton(String label, String action, String color, String displayText) {
        return Button.builder()
                .style(Button.ButtonStyle.PRIMARY)
                .color(color)
                .action(PostbackAction.builder()
                        .label(label)
                        .data(action)
                        .displayText(displayText)
                        .build())
                .build();
    }

    /**
     * 創建間隔
     */
    private Box createSpacer() {
        return Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(Arrays.asList())
                .height("8px")
                .build();
    }
}