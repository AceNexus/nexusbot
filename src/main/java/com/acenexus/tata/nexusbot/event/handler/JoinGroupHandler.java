package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.acenexus.tata.nexusbot.event.RoomType;
import com.acenexus.tata.nexusbot.service.MessageService;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 處理機器人加入群組事件
 */
@Component
@RequiredArgsConstructor
public class JoinGroupHandler implements LineBotEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(JoinGroupHandler.class);
    private final MessageTemplateProvider messageTemplateProvider;
    private final MessageService messageService;

    @Override
    public boolean canHandle(LineBotEvent event) {
        return event.getEventType() == EventType.JOIN;
    }

    @Override
    public Message handle(LineBotEvent event) {
        logger.info("Bot joined group: {}", event.getRoomId());

        // 透過 MessageTemplateProvider 取得訊息
        String sourceType = event.getRoomType() == RoomType.GROUP ? "group" : "room";
        String joinMessage = messageTemplateProvider.groupJoinMessage(sourceType);

        // 使用 MessageService.sendReply 發送純文字訊息
        messageService.sendReply(event.getReplyToken(), joinMessage);
        return null; // 已經透過 MessageService 發送，不需要再回傳 Message
    }

    @Override
    public int getPriority() {
        return 4;
    }
}
