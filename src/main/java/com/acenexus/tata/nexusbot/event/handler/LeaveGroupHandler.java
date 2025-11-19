package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.linecorp.bot.model.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 處理機器人離開群組事件
 */
@Component
public class LeaveGroupHandler implements LineBotEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(LeaveGroupHandler.class);

    @Override
    public boolean canHandle(LineBotEvent event) {
        return event.getEventType() == EventType.LEAVE;
    }

    @Override
    public Message handle(LineBotEvent event) {
        logger.info("Bot left group: {}", event.getRoomId());
        return null; // leave 事件不需要回覆
    }

    @Override
    public int getPriority() {
        return 4;
    }
}
