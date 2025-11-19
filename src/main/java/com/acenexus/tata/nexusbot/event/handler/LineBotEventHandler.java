package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.linecorp.bot.model.message.Message;

/**
 * LINE Bot 事件處理器統一介面
 * 所有事件處理器都應實作此介面，以實現統一的事件處理架構
 */
public interface LineBotEventHandler {
    /**
     * 判斷是否可以處理此事件
     * 此方法可以執行業務邏輯判斷，包括查詢狀態、權限等
     *
     * @param event 統一的 LINE Bot 事件
     * @return true 表示此 Handler 可以處理此事件
     */
    boolean canHandle(LineBotEvent event);

    /**
     * 處理事件並返回回覆訊息
     *
     * @param event 統一的 LINE Bot 事件
     * @return 回覆訊息，如果不需要回覆則返回 null
     */
    Message handle(LineBotEvent event);

    /**
     * 取得處理器的優先級
     * 數字越小優先級越高，Dispatcher 會依此排序
     *
     * @return 優先級
     */
    int getPriority();
}
