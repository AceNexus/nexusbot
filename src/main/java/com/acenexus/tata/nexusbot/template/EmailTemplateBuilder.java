package com.acenexus.tata.nexusbot.template;

import com.linecorp.bot.model.message.FlexMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.flex.component.Box;
import com.linecorp.bot.model.message.flex.component.FlexComponent;
import com.linecorp.bot.model.message.flex.component.Text;
import com.linecorp.bot.model.message.flex.container.Bubble;
import com.linecorp.bot.model.message.flex.unit.FlexFontSize;
import com.linecorp.bot.model.message.flex.unit.FlexLayout;
import com.linecorp.bot.model.message.flex.unit.FlexMarginSize;
import com.linecorp.bot.model.message.flex.unit.FlexPaddingSize;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.acenexus.tata.nexusbot.constants.Actions.ADD_EMAIL;
import static com.acenexus.tata.nexusbot.constants.Actions.CANCEL_EMAIL_INPUT;
import static com.acenexus.tata.nexusbot.constants.Actions.EMAIL_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.MAIN_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.deleteEmail;
import static com.acenexus.tata.nexusbot.constants.Actions.toggleEmailStatus;
import static com.acenexus.tata.nexusbot.template.UIConstants.BorderRadius;
import static com.acenexus.tata.nexusbot.template.UIConstants.Colors;
import static com.acenexus.tata.nexusbot.template.UIConstants.Spacing;

/**
 * Email 通知相關的訊息範本建構器
 */
@Component
public class EmailTemplateBuilder extends FlexMessageTemplateBuilder {

    /**
     * Email 設定選單
     */
    public Message emailSettingsMenu(List<com.acenexus.tata.nexusbot.entity.Email> emails) {
        String title = "Email 通知設定";

        // 建立主要內容組件
        List<FlexComponent> components = new ArrayList<>();
        components.add(createTitleComponent(title));

        if (emails.isEmpty()) {
            // 空狀態提示
            Text emptyText = Text.builder()
                    .text("目前尚未新增任何 Email 地址\n\n設定 Email 通知後，提醒將同時發送至您的信箱，確保不錯過重要事項。")
                    .size(FlexFontSize.SM)
                    .color(Colors.TEXT_SECONDARY)
                    .wrap(true)
                    .margin(FlexMarginSize.LG)
                    .build();
            components.add(emptyText);
        } else {
            // 信箱數量統計
            Text countText = Text.builder()
                    .text(String.format("已設定 %d 個信箱", emails.size()))
                    .size(FlexFontSize.SM)
                    .color(Colors.TEXT_SECONDARY)
                    .margin(FlexMarginSize.MD)
                    .build();
            components.add(countText);

            // Email 列表
            for (com.acenexus.tata.nexusbot.entity.Email email : emails) {
                components.add(createEmailItemBox(email));
            }
        }

        // 分隔線
        components.add(createSeparator());

        // 底部按鈕區域
        List<FlexComponent> bottomButtons = new ArrayList<>();
        bottomButtons.add(createPrimaryButton("+ 新增信箱", ADD_EMAIL));
        bottomButtons.add(createSpacing(Spacing.SM));
        bottomButtons.add(createNavigateButton("返回主選單", MAIN_MENU));

        Box buttonContainer = Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(bottomButtons)
                .paddingTop(FlexPaddingSize.MD)
                .build();

        components.add(buttonContainer);

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
                .altText(title)
                .contents(bubble)
                .build();
    }

    /**
     * 建立 Email 項目 Box
     */
    private Box createEmailItemBox(com.acenexus.tata.nexusbot.entity.Email email) {
        boolean isEnabled = Boolean.TRUE.equals(email.getIsEnabled());
        String statusIcon = isEnabled ? "●" : "○";
        String statusColor = isEnabled ? Colors.SUCCESS : Colors.GRAY;
        String toggleLabel = isEnabled ? "停用" : "啟用";

        // 狀態指示器 + Email 地址
        Box emailInfo = Box.builder()
                .layout(FlexLayout.HORIZONTAL)
                .contents(Arrays.asList(
                        Text.builder()
                                .text(statusIcon)
                                .size(FlexFontSize.SM)
                                .color(statusColor)
                                .flex(0)
                                .build(),
                        Text.builder()
                                .text(email.getEmail())
                                .size(FlexFontSize.SM)
                                .color(Colors.TEXT_PRIMARY)
                                .wrap(true)
                                .margin(FlexMarginSize.XS)
                                .build()
                ))
                .build();

        // 操作按鈕行
        Box actionButtons = Box.builder()
                .layout(FlexLayout.HORIZONTAL)
                .contents(Arrays.asList(
                        createButtonWithFlex(toggleLabel,
                                toggleEmailStatus(email.getId()),
                                ButtonType.NEUTRAL, 1),
                        createButtonWithFlex("刪除",
                                deleteEmail(email.getId()),
                                ButtonType.DANGER, 1)
                ))
                .spacing(FlexMarginSize.SM)
                .margin(FlexMarginSize.XS)
                .build();

        // Email 項目容器（包含資訊和按鈕）
        return Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(Arrays.asList(emailInfo, actionButtons))
                .backgroundColor(Colors.BORDER)
                .cornerRadius(BorderRadius.MD)
                .paddingAll(FlexPaddingSize.SM)
                .margin(FlexMarginSize.XS)
                .build();
    }

    /**
     * Email 輸入提示訊息
     */
    public Message emailInputPrompt() {
        return createCard(
                "新增 Email",
                "請輸入您的電子郵件地址。\n\n請確保輸入正確的 Email 格式，例如：user@example.com",
                Arrays.asList(
                        createSecondaryButton("取消新增", CANCEL_EMAIL_INPUT),
                        createNavigateButton("返回 Email 設定", EMAIL_MENU)
                )
        );
    }

    /**
     * Email 新增成功訊息
     */
    public Message emailAddSuccess(String email) {
        return createCard(
                "Email 新增成功",
                String.format("已成功新增 Email：\n%s\n\n此 Email 已自動啟用通知功能。", email),
                Arrays.asList(
                        createSuccessButton("返回 Email 設定", EMAIL_MENU),
                        createNavigateButton("返回主選單", MAIN_MENU)
                )
        );
    }

    /**
     * Email 格式錯誤訊息
     */
    public Message emailInvalidFormat() {
        return createCard(
                "Email 格式錯誤",
                "您輸入的 Email 格式不正確。\n\n請輸入有效的電子郵件地址，例如：\n• user@gmail.com\n• example@outlook.com\n• name@company.com",
                Arrays.asList(
                        createPrimaryButton("重新輸入", ADD_EMAIL),
                        createSecondaryButton("返回 Email 設定", EMAIL_MENU)
                )
        );
    }
}
