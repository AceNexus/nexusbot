package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 處理使用者加入好友事件
 */
@Component
@RequiredArgsConstructor
public class FollowHandler implements LineBotEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(FollowHandler.class);
    private final MessageTemplateProvider messageTemplateProvider;

    @Override
    public boolean canHandle(LineBotEvent event) {
        return event.getEventType() == EventType.FOLLOW;
    }

    @Override
    public Message handle(LineBotEvent event) {
        logger.info("User {} followed the bot", event.getUserId());
        return messageTemplateProvider.welcome();
    }

    @Override
    public int getPriority() {
        return 4;
    }
}
