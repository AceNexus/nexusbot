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

    // ============= 選單導航動作 =============
    public static final String MAIN_MENU = "action=main_menu";
    public static final String HELP_MENU = "action=help_menu";
    public static final String ABOUT = "action=about";
}