package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.acenexus.tata.nexusbot.facade.ReminderFacade;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 處理提醒創建流程中的使用者輸入
 */
@Component
@RequiredArgsConstructor
public class ReminderInteractionEventHandler implements LineBotEventHandler {

    private final ReminderFacade reminderFacade;

    @Override
    public boolean canHandle(LineBotEvent event) {
        if (event.getEventType() != EventType.TEXT_MESSAGE) {
            return false;
        }

        // 檢查是否在提醒創建流程中
        return reminderFacade.isInReminderFlow(event.getRoomId());
    }

    @Override
    public Message handle(LineBotEvent event) {
        String text = event.getPayloadString("text");
        return reminderFacade.handleInteraction(event.getRoomId(), text, event.getReplyToken());
    }

    @Override
    public int getPriority() {
        return 3; // 高優先級，在狀態流程處理
    }
}
