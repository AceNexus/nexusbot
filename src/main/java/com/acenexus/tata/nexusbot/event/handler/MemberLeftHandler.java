package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.linecorp.bot.model.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 處理成員離開群組事件
 */
@Component
public class MemberLeftHandler implements LineBotEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(MemberLeftHandler.class);

    @Override
    public boolean canHandle(LineBotEvent event) {
        return event.getEventType() == EventType.MEMBER_LEFT;
    }

    @Override
    public Message handle(LineBotEvent event) {
        logger.info("Group member left: {}", event.getRoomId());
        return null; // memberLeft 事件通常不需要回覆
    }

    @Override
    public int getPriority() {
        return 4;
    }
}
