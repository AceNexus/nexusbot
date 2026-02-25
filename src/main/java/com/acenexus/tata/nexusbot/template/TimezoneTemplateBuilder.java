package com.acenexus.tata.nexusbot.template;

import com.linecorp.bot.model.message.Message;
import org.springframework.stereotype.Component;

import java.util.Arrays;

import static com.acenexus.tata.nexusbot.constants.Actions.KEEP_TIMEZONE;
import static com.acenexus.tata.nexusbot.constants.Actions.MAIN_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.SETTINGS_CANCEL_TIMEZONE;
import static com.acenexus.tata.nexusbot.constants.Actions.SETTINGS_CHANGE_TIMEZONE;
import static com.acenexus.tata.nexusbot.constants.Actions.SETTINGS_CONFIRM_TIMEZONE;
import static com.acenexus.tata.nexusbot.constants.Actions.TIMEZONE_SETTINGS;

/**
 * 時區設定相關的訊息範本建構器
 */
@Component
public class TimezoneTemplateBuilder extends FlexMessageTemplateBuilder {

    /**
     * 時區設定選單 - 顯示目前時區，提供維持或變更選項
     */
    public Message timezoneSettingsMenu(String currentTimezone, String timezoneDisplay) {
        return createCard(
                "時區設定",
                String.format("目前聊天室時區：\n%s\n\n時區設定會影響提醒時間的解析。例如，當您說「明天早上8點」時，系統將以此時區為準。", timezoneDisplay),
                Arrays.asList(
                        createNeutralButton("維持不變", KEEP_TIMEZONE),
                        createPrimaryButton("變更時區", SETTINGS_CHANGE_TIMEZONE),
                        createNavigateButton("返回主選單", MAIN_MENU)
                )
        );
    }

    /**
     * 時區變更輸入提示
     */
    public Message timezoneChangePrompt(String currentTimezone) {
        return createCard(
                "變更時區",
                "請使用自然語言輸入您想要的時區。\n\n支援的輸入範例：\n• 台北、東京、紐約\n• Asia/Taipei、America/New_York\n• 上海、香港、倫敦\n\n請直接輸入時區名稱。",
                Arrays.asList(
                        createSecondaryButton("取消變更", SETTINGS_CANCEL_TIMEZONE),
                        createNavigateButton("返回時區設定", TIMEZONE_SETTINGS)
                )
        );
    }

    /**
     * 時區確認選單 - 顯示解析結果，讓使用者確認
     */
    public Message timezoneConfirmationMenu(String resolvedTimezone, String timezoneDisplay, String originalInput) {
        return createCard(
                "確認時區變更",
                String.format("您輸入的「%s」\n系統判定為：%s\n\n請確認是否要將聊天室時區變更為此時區？", originalInput, timezoneDisplay),
                Arrays.asList(
                        createSuccessButton("確認變更", SETTINGS_CONFIRM_TIMEZONE),
                        createSecondaryButton("取消", SETTINGS_CANCEL_TIMEZONE),
                        createNavigateButton("返回時區設定", TIMEZONE_SETTINGS)
                )
        );
    }

    /**
     * 時區輸入錯誤訊息
     */
    public Message timezoneInputError(String userInput) {
        return createCard(
                "無法識別時區",
                String.format("抱歉，無法識別您輸入的「%s」。\n\n請嘗試使用以下格式：\n• 常用城市：台北、東京、紐約、倫敦\n• IANA 格式：Asia/Taipei、America/New_York\n• 中文名稱：台灣、日本、美國東岸", userInput),
                Arrays.asList(
                        createPrimaryButton("重新輸入", SETTINGS_CHANGE_TIMEZONE),
                        createSecondaryButton("取消變更", SETTINGS_CANCEL_TIMEZONE),
                        createNavigateButton("返回時區設定", TIMEZONE_SETTINGS)
                )
        );
    }

    /**
     * 時區更新成功訊息
     */
    public Message timezoneUpdateSuccess(String newTimezone, String timezoneDisplay) {
        return createCard(
                "時區更新成功",
                String.format("聊天室時區已更新為：\n%s\n\n之後的提醒時間將以此時區為準。", timezoneDisplay),
                Arrays.asList(
                        createSuccessButton("返回時區設定", TIMEZONE_SETTINGS),
                        createNavigateButton("返回主選單", MAIN_MENU)
                )
        );
    }

    /**
     * 取消時區變更訊息
     */
    public Message timezoneCancelMessage(String currentTimezone, String timezoneDisplay) {
        return createCard(
                "已取消變更",
                String.format("已取消時區變更。\n\n目前聊天室時區維持為：\n%s", timezoneDisplay),
                Arrays.asList(
                        createNavigateButton("返回時區設定", TIMEZONE_SETTINGS),
                        createNavigateButton("返回主選單", MAIN_MENU)
                )
        );
    }
}
