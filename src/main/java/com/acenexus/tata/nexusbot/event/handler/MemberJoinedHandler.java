package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.acenexus.tata.nexusbot.service.MessageService;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 處理成員加入群組事件
 */
@Component
@RequiredArgsConstructor
public class MemberJoinedHandler implements LineBotEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(MemberJoinedHandler.class);
    private final MessageTemplateProvider messageTemplateProvider;
    private final MessageService messageService;

    @Override
    public boolean canHandle(LineBotEvent event) {
        return event.getEventType() == EventType.MEMBER_JOINED;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Message handle(LineBotEvent event) {
        // 從 payload 取得加入的成員清單
        Object joinedUsersObj = event.getPayload().get("joinedUsers");
        int memberCount = joinedUsersObj instanceof List ? ((List<?>) joinedUsersObj).size() : 0;

        logger.info("{} members joined group: {}", memberCount, event.getRoomId());

        // 使用 MessageService.sendReply 發送純文字訊息
        String welcomeMessage = messageTemplateProvider.memberJoinedMessage(memberCount);
        messageService.sendReply(event.getReplyToken(), welcomeMessage);
        return null; // 已經透過 MessageService 發送，不需要再回傳 Message
    }

    @Override
    public int getPriority() {
        return 4;
    }
}
