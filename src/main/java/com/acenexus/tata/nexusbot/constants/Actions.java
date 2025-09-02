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
    public static final String MODEL_LLAMA3_70B = "model=llama3-70b-8192";
    public static final String MODEL_GEMMA2_9B = "model=gemma2-9b-it";
    public static final String MODEL_DEEPSEEK_R1 = "model=deepseek-r1-distill-llama-70b";
    public static final String MODEL_QWEN3_32B = "model=qwen/qwen3-32b";

    // ============= 選單導航動作 =============
    public static final String MAIN_MENU = "action=main_menu";
    public static final String HELP_MENU = "action=help_menu";
    public static final String ABOUT = "action=about";

    // ============= 提醒功能動作 =============
    public static final String REMINDER_MENU = "action=reminder_menu";
    public static final String ADD_REMINDER = "action=add_reminder";
    public static final String CANCEL_REMINDER_INPUT = "action=cancel_reminder_input";

    // 重複類型選擇
    public static final String REPEAT_ONCE = "repeat=ONCE";
    public static final String REPEAT_DAILY = "repeat=DAILY";
    public static final String REPEAT_WEEKLY = "repeat=WEEKLY";
}