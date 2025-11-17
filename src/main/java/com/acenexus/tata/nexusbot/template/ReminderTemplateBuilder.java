package com.acenexus.tata.nexusbot.template;

import com.acenexus.tata.nexusbot.entity.Reminder;
import com.linecorp.bot.model.message.Message;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.acenexus.tata.nexusbot.constants.Actions.ADD_REMINDER;
import static com.acenexus.tata.nexusbot.constants.Actions.CANCEL_REMINDER_INPUT;
import static com.acenexus.tata.nexusbot.constants.Actions.CHANNEL_BOTH;
import static com.acenexus.tata.nexusbot.constants.Actions.CHANNEL_EMAIL;
import static com.acenexus.tata.nexusbot.constants.Actions.CHANNEL_LINE;
import static com.acenexus.tata.nexusbot.constants.Actions.LIST_REMINDERS;
import static com.acenexus.tata.nexusbot.constants.Actions.MAIN_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.REMINDER_MENU;
import static com.acenexus.tata.nexusbot.constants.Actions.REPEAT_DAILY;
import static com.acenexus.tata.nexusbot.constants.Actions.REPEAT_ONCE;
import static com.acenexus.tata.nexusbot.constants.Actions.REPEAT_WEEKLY;
import static com.acenexus.tata.nexusbot.constants.Actions.TODAY_REMINDERS;
import static com.acenexus.tata.nexusbot.constants.Actions.reminderCompleted;
import static com.acenexus.tata.nexusbot.constants.TimeFormatters.STANDARD_TIME;

/**
 * Reminder 提醒相關的訊息範本建構器
 */
@Component
public class ReminderTemplateBuilder extends FlexMessageTemplateBuilder {

    /**
     * 提醒管理主選單
     */
    public Message reminderMenu() {
        return createCard(
                "提醒管理",
                "智慧提醒管理系統可以幫助您設定重要事項的提醒通知。支援單次提醒和週期性提醒設定。",
                Arrays.asList(
                        createPrimaryButton("新增提醒", ADD_REMINDER),
                        createNeutralButton("活躍提醒列表", LIST_REMINDERS),
                        createNeutralButton("今日提醒記錄", TODAY_REMINDERS),
                        createNavigateButton("返回主選單", MAIN_MENU)
                )
        );
    }

    /**
     * 提醒重複類型選擇選單
     */
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

    /**
     * 通知管道選擇選單
     */
    public Message reminderNotificationChannelMenu() {
        return createCard(
                "通知方式設定",
                "請選擇提醒的通知方式。Email 通知可節省 LINE 訊息額度，適合日常使用。",
                Arrays.asList(
                        createPrimaryButton("LINE 通知", CHANNEL_LINE),
                        createNeutralButton("Email 通知", CHANNEL_EMAIL),
                        createNeutralButton("雙重通知", CHANNEL_BOTH),
                        createSecondaryButton("取消設定", CANCEL_REMINDER_INPUT)
                )
        );
    }

    /**
     * 提醒輸入選單（時間或內容）
     */
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

    /**
     * 提醒輸入選單（帶已設定時間）
     */
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

    /**
     * 提醒建立成功訊息
     */
    public Message reminderCreatedSuccess(String reminderTime, String repeatType, String content) {
        String description = String.format(
                "提醒已成功建立：\n\n時間：%s\n頻率：%s\n內容：%s\n\n系統將在指定時間發送提醒通知。",
                reminderTime, repeatType, content);

        return createCard("提醒建立成功", description, Arrays.asList(
                createSuccessButton("返回提醒管理", REMINDER_MENU),
                createNavigateButton("返回主選單", MAIN_MENU)
        ));
    }

    /**
     * 提醒輸入錯誤訊息（時間格式錯誤）
     */
    public Message reminderInputError(String userInput, String aiResult) {
        String description = String.format(
                "無法解析您輸入的時間格式：\n\n您的輸入：%s\n系統解析：%s\n\n請使用正確的時間格式，例如：\n• 標準格式：2025-01-01 15:00\n• 自然語言：明天下午3點\n• 相對時間：30分鐘後",
                userInput, aiResult);

        return createCard("時間格式錯誤", description, Arrays.asList(
                createPrimaryButton("重新設定", ADD_REMINDER),
                createSecondaryButton("返回提醒管理", REMINDER_MENU)
        ));
    }

    /**
     * 提醒列表
     */
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

    /**
     * 今日提醒記錄
     */
    public Message todayReminderLogs(java.util.List<com.acenexus.tata.nexusbot.reminder.ReminderLogService.TodayReminderLog> logs) {
        if (logs.isEmpty()) {
            return createCard(
                    "今日提醒記錄",
                    "今天還沒有發送任何提醒",
                    Arrays.asList(
                            createPrimaryButton("新增提醒", ADD_REMINDER),
                            createSecondaryButton("返回提醒管理", REMINDER_MENU)
                    )
            );
        }

        StringBuilder contentBuilder = new StringBuilder();
        for (int i = 0; i < logs.size(); i++) {
            com.acenexus.tata.nexusbot.reminder.ReminderLogService.TodayReminderLog log = logs.get(i);

            String status = log.isConfirmed() ? "已確認" : "待確認";

            contentBuilder.append(String.format(
                    "%d. %s\n發送時間：%s\n狀態：%s\n\n",
                    i + 1,
                    log.content(),
                    log.sentTime().format(STANDARD_TIME),
                    status
            ));
        }

        return createCard("今日提醒記錄", contentBuilder.toString(), Arrays.asList(
                createNeutralButton("查看活躍提醒", LIST_REMINDERS),
                createSecondaryButton("返回提醒管理", REMINDER_MENU)
        ));
    }

    /**
     * 建立提醒通知訊息
     */
    public Message buildReminderNotification(String enhancedContent, String originalContent, String repeatType, Long reminderId) {
        String repeatDescription = switch (repeatType.toUpperCase()) {
            case "DAILY" -> "每日提醒";
            case "WEEKLY" -> "每週提醒";
            case "ONCE" -> "一次性提醒";
            default -> "提醒";
        };

        String currentTime = java.time.LocalDateTime.now().format(STANDARD_TIME);

        String description = String.format(
                "提醒時間\n%s\n\n提醒事項\n%s\n\n貼心小提醒\n%s\n\n類型：%s",
                currentTime, originalContent, enhancedContent, repeatDescription
        );

        return createCard("提醒時間到了", description, Arrays.asList(
                createSuccessButton("確認已執行", reminderCompleted(reminderId))
        ));
    }
}
