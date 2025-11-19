package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.linecorp.bot.model.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 處理使用者封鎖機器人事件
 */
@Component
public class UnfollowHandler implements LineBotEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(UnfollowHandler.class);

    @Override
    public boolean canHandle(LineBotEvent event) {
        return event.getEventType() == EventType.UNFOLLOW;
    }

    @Override
    public Message handle(LineBotEvent event) {
        logger.info("User {} unfollowed the bot", event.getUserId());
        return null; // unfollow 事件不需要回覆
    }

    @Override
    public int getPriority() {
        return 4;
    }
}
