package com.acenexus.tata.nexusbot.constants;

/**
 * LINE Bot Postback 動作常數定義
 * 統一管理所有按鈕點擊的 action 字串
 */
public final class Actions {

    private Actions() {
    }

    // ============= AI 相關動作 =============
    public static final String TOGGLE_AI = "action=toggle_ai";
    public static final String ENABLE_AI = "action=enable_ai";
    public static final String DISABLE_AI = "action=disable_ai";
    public static final String SELECT_MODEL = "action=select_model";
    public static final String CLEAR_HISTORY = "action=clear_history";
    public static final String CONFIRM_CLEAR_HISTORY = "action=confirm_clear_history";

    // ============= AI 模型選擇動作 =============
    public static final String MODEL_LLAMA_3_1_8B = "model=llama-3.1-8b-instant";
    public static final String MODEL_LLAMA_3_3_70B = "model=llama-3.3-70b-versatile";

    // ============= 選單導航動作 =============
    public static final String MAIN_MENU = "action=main_menu";
    public static final String HELP_MENU = "action=help_menu";
    public static final String ABOUT = "action=about";

    // ============= 位置服務動作 =============
    public static final String FIND_TOILETS = "action=find_toilets";

    // ============= 提醒功能動作 =============
    public static final String REMINDER_MENU = "action=reminder_menu";
    public static final String ADD_REMINDER = "action=add_reminder";
    public static final String LIST_REMINDERS = "action=list_reminders";
    public static final String TODAY_REMINDERS = "action=today_reminders";
    public static final String DELETE_REMINDER = "action=delete_reminder";
    public static final String CANCEL_REMINDER_INPUT = "action=cancel_reminder_input";

    // ============= 提醒回報動作 =============
    public static final String REMINDER_COMPLETED = "action=reminder_completed";

    // 重複類型選擇
    public static final String REPEAT_ONCE = "repeat=ONCE";
    public static final String REPEAT_DAILY = "repeat=DAILY";
    public static final String REPEAT_WEEKLY = "repeat=WEEKLY";

    // 通知管道選擇
    public static final String CHANNEL_LINE = "channel=LINE";
    public static final String CHANNEL_EMAIL = "channel=EMAIL";
    public static final String CHANNEL_BOTH = "channel=BOTH";

    // ============= 時區設定動作 (系統全域) =============
    public static final String TIMEZONE_SETTINGS = "action=timezone_settings";
    public static final String KEEP_TIMEZONE = "action=keep_timezone";
    public static final String SETTINGS_CHANGE_TIMEZONE = "action=settings_change_timezone";
    public static final String SETTINGS_CONFIRM_TIMEZONE = "action=settings_confirm_timezone";
    public static final String SETTINGS_CANCEL_TIMEZONE = "action=settings_cancel_timezone";

    // 提醒流程專用的時區動作
    public static final String REMINDER_CHANGE_TIMEZONE = "action=reminder_change_timezone";
    public static final String REMINDER_CONFIRM_TIMEZONE = "action=reminder_confirm_timezone";
    public static final String REMINDER_CANCEL_TIMEZONE = "action=reminder_cancel_timezone";

    // 提醒時間修改動作
    public static final String CHANGE_TIME = "action=change_time";

    // ============= Email 通知動作 =============
    public static final String EMAIL_MENU = "action=email_menu";
    public static final String ADD_EMAIL = "action=add_email";
    public static final String DELETE_EMAIL = "action=delete_email";
    public static final String TOGGLE_EMAIL_STATUS = "action=toggle_email_status";
    public static final String CANCEL_EMAIL_INPUT = "action=cancel_email_input";

    // ============= 動態參數工具方法 =============

    /**
     * 構建提醒已完成的 action 字串
     */
    public static String reminderCompleted(Long id) {
        return REMINDER_COMPLETED + "&id=" + id;
    }

    /**
     * 構建刪除 Email 的 action 字串
     */
    public static String deleteEmail(Long id) {
        return DELETE_EMAIL + "&id=" + id;
    }

    /**
     * 構建切換 Email 狀態的 action 字串
     */
    public static String toggleEmailStatus(Long id) {
        return TOGGLE_EMAIL_STATUS + "&id=" + id;
    }
}