package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.linecorp.bot.model.message.Message;

/**
 * LINE Bot 事件處理器統一介面
 * 所有事件處理器都應實作此介面，以實現統一的事件處理架構。
 * Dispatcher 使用責任鏈模式（Chain of Responsibility）依優先級將事件分發給 Handler。
 *
 * @see com.acenexus.tata.nexusbot.event.handler.LineBotEventDispatcher
 */
public interface LineBotEventHandler {
    /**
     * 判斷是否可以處理此事件
     * 此方法會被 Dispatcher 按優先級依序呼叫，直到找到第一個返回 {@code true} 的 Handler。
     *
     * @param event 統一的 LINE Bot 事件
     * @return {@code true} 表示此 Handler 可以處理此事件，{@code false} 表示不處理
     */
    boolean canHandle(LineBotEvent event);

    /**
     * 處理事件並返回回覆訊息
     *
     * @param event 統一的 LINE Bot 事件
     * @return 回覆訊息，如果不需要回覆則返回 {@code null}
     */
    Message handle(LineBotEvent event);

    /**
     * 取得處理器的優先級
     *
     * @return 優先級數值，越小優先級越高
     */
    int getPriority();
}
