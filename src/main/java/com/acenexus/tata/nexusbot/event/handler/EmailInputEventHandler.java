package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.acenexus.tata.nexusbot.facade.EmailFacade;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 處理 Email 輸入流程中的使用者輸入
 */
@Component
@RequiredArgsConstructor
public class EmailInputEventHandler implements LineBotEventHandler {

    private final EmailFacade emailFacade;

    @Override
    public boolean canHandle(LineBotEvent event) {
        if (event.getEventType() != EventType.TEXT_MESSAGE) {
            return false;
        }

        // 檢查是否在 Email 輸入流程中
        return emailFacade.isWaitingForEmailInput(event.getRoomId());
    }

    @Override
    public Message handle(LineBotEvent event) {
        String text = event.getPayloadString("text");
        return emailFacade.handleEmailInput(event.getRoomId(), text);
    }

    @Override
    public int getPriority() {
        return 5; // 狀態流程處理，順序次於提醒與時區流程
    }
}
