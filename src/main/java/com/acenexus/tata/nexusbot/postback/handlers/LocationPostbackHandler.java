package com.acenexus.tata.nexusbot.postback.handlers;

import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.facade.LocationFacade;
import com.acenexus.tata.nexusbot.postback.PostbackHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.acenexus.tata.nexusbot.constants.Actions.FIND_TOILETS;

/**
 * 位置功能 Handler
 * 職責：純路由，將請求委派給 LocationFacade
 */
@Component
@Order(4)
@RequiredArgsConstructor
public class LocationPostbackHandler implements PostbackHandler {

    private static final Logger logger = LoggerFactory.getLogger(LocationPostbackHandler.class);

    private final LocationFacade locationFacade;

    @Override
    public boolean canHandle(String action) {
        return FIND_TOILETS.equals(action);
    }

    @Override
    public Message handle(String action, String roomId, String roomType, String replyToken, JsonNode event) {
        logger.info("LocationPostbackHandler handling action: {} for room: {}", action, roomId);

        ChatRoom.RoomType type = ChatRoom.RoomType.valueOf(roomType);

        return switch (action) {
            case FIND_TOILETS -> locationFacade.startToiletSearch(roomId, type);
            default -> null;
        };
    }

    @Override
    public int getPriority() {
        return 4;
    }
}
