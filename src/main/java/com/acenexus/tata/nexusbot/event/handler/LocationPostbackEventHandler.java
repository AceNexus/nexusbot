package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.acenexus.tata.nexusbot.facade.LocationFacade;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.acenexus.tata.nexusbot.constants.Actions.FIND_TOILETS;

/**
 * 處理位置相關的 Postback 事件
 */
@Component
@RequiredArgsConstructor
public class LocationPostbackEventHandler implements LineBotEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(LocationPostbackEventHandler.class);

    private final LocationFacade locationFacade;

    @Override
    public boolean canHandle(LineBotEvent event) {
        if (event.getEventType() != EventType.POSTBACK) {
            return false;
        }

        String action = event.getPayloadString("action");
        return FIND_TOILETS.equals(action);
    }

    @Override
    public Message handle(LineBotEvent event) {
        String action = event.getPayloadString("action");
        String roomId = event.getRoomId();
        ChatRoom.RoomType roomType = ChatRoom.RoomType.valueOf(event.getRoomType().name());

        logger.info("LocationPostbackEventHandler handling action: {} for room: {}", action, roomId);

        return switch (action) {
            case FIND_TOILETS -> locationFacade.startToiletSearch(roomId, roomType);
            default -> null;
        };
    }

    @Override
    public int getPriority() {
        return 4;
    }
}
