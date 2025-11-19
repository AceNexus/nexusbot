package com.acenexus.tata.nexusbot.event.converter;

import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 事件轉換器介面
 * 負責將 LINE Webhook 原始 JSON 事件轉換為統一的 {@link LineBotEvent} 模型
 */
public interface EventConverter {
    /**
     * 判斷是否可以轉換此事件
     *
     * @param event LINE Webhook 原始事件 JSON
     * @return true 表示此 Converter 可以處理此事件
     */
    boolean canConvert(JsonNode event);

    /**
     * 轉換事件為統一的 LineBotEvent 模型
     *
     * @param event LINE Webhook 原始事件 JSON
     * @return 轉換後的 LineBotEvent
     * @throws IllegalArgumentException 當事件格式不正確時
     */
    LineBotEvent convert(JsonNode event);
}
