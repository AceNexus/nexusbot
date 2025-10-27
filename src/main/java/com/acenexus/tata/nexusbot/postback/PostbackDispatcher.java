package com.acenexus.tata.nexusbot.postback;

import com.acenexus.tata.nexusbot.postback.handlers.AIPostbackHandler;
import com.acenexus.tata.nexusbot.postback.handlers.EmailPostbackHandler;
import com.acenexus.tata.nexusbot.postback.handlers.LocationPostbackHandler;
import com.acenexus.tata.nexusbot.postback.handlers.NavigationPostbackHandler;
import com.acenexus.tata.nexusbot.postback.handlers.PostbackEventDispatcher;
import com.acenexus.tata.nexusbot.postback.handlers.ReminderPostbackHandler;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Postback 事件處理器
 * 委派給 PostbackEventDispatcher 分發事件到各領域 Handler。
 *
 * @see PostbackEventDispatcher
 * @see NavigationPostbackHandler
 * @see AIPostbackHandler
 * @see ReminderPostbackHandler
 * @see EmailPostbackHandler
 * @see LocationPostbackHandler
 */
@Component
@RequiredArgsConstructor
public class PostbackDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(PostbackDispatcher.class);

    private final PostbackEventDispatcher dispatcher;

    /**
     * 處理 Postback 事件
     */
    public void handle(JsonNode event) {
        dispatcher.dispatch(event);
    }
}
