package com.acenexus.tata.nexusbot.facade;

import com.linecorp.bot.model.message.Message;

/**
 * 導航功能 Facade - 協調導航選單
 */
public interface NavigationFacade {

    /**
     * 顯示主選單
     */
    Message showMainMenu();

    /**
     * 顯示說明選單
     */
    Message showHelpMenu();

    /**
     * 顯示關於頁面
     */
    Message showAbout();
}
