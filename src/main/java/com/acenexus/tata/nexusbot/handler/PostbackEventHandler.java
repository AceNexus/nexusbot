package com.acenexus.tata.nexusbot.handler;

import com.acenexus.tata.nexusbot.handler.postback.PostbackEventDispatcher;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Postback 事件處理器
 * 委派給 PostbackEventDispatcher 分發事件到各領域 Handler。
 *
 * @see com.acenexus.tata.nexusbot.handler.postback.PostbackEventDispatcher
 * @see com.acenexus.tata.nexusbot.handler.postback.NavigationPostbackHandler
 * @see com.acenexus.tata.nexusbot.handler.postback.AIPostbackHandler
 * @see com.acenexus.tata.nexusbot.handler.postback.ReminderPostbackHandler
 * @see com.acenexus.tata.nexusbot.handler.postback.EmailPostbackHandler
 * @see com.acenexus.tata.nexusbot.handler.postback.LocationPostbackHandler
 */
@Component
@RequiredArgsConstructor
public class PostbackEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(PostbackEventHandler.class);

    private final PostbackEventDispatcher dispatcher;

    /**
     * 處理 Postback 事件
     */
    public void handle(JsonNode event) {
        dispatcher.dispatch(event);
    }
}
