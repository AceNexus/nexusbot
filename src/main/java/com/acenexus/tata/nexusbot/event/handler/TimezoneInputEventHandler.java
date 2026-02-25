package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.acenexus.tata.nexusbot.facade.TimezoneFacade;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 處理時區輸入流程中的使用者輸入
 */
@Component
@RequiredArgsConstructor
public class TimezoneInputEventHandler implements LineBotEventHandler {

    private final TimezoneFacade timezoneFacade;

    @Override
    public boolean canHandle(LineBotEvent event) {
        if (event.getEventType() != EventType.TEXT_MESSAGE) {
            return false;
        }

        // 檢查是否在時區輸入流程中
        return timezoneFacade.isWaitingForTimezoneInput(event.getRoomId());
    }

    @Override
    public Message handle(LineBotEvent event) {
        String text = event.getPayloadString("text");
        return timezoneFacade.handleTimezoneInput(event.getRoomId(), text);
    }

    @Override
    public int getPriority() {
        return 4; // 狀態流程處理
    }
}
