package com.acenexus.tata.nexusbot.postback.handlers;

import com.acenexus.tata.nexusbot.chatroom.ChatRoomManager;
import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.postback.PostbackHandler;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static com.acenexus.tata.nexusbot.constants.Actions.FIND_TOILETS;

/**
 * 位置功能 Handler - 處理廁所搜尋
 */
@Component
@Order(4)
@RequiredArgsConstructor
public class LocationPostbackHandler implements PostbackHandler {

    private static final Logger logger = LoggerFactory.getLogger(LocationPostbackHandler.class);

    private final ChatRoomManager chatRoomManager;
    private final MessageTemplateProvider messageTemplateProvider;

    @Override
    public boolean canHandle(String action) {
        return FIND_TOILETS.equals(action);
    }

    @Override
    public Message handle(String action, String roomId, String roomType, String replyToken, JsonNode event) {
        logger.info("LocationPostbackHandler handling action: {} for room: {}", action, roomId);

        ChatRoom.RoomType type = ChatRoom.RoomType.valueOf(roomType);

        return switch (action) {
            case FIND_TOILETS -> {
                chatRoomManager.setWaitingForToiletSearch(roomId, type, true);
                logger.info("Set waiting for toilet search for room: {}", roomId);
                yield messageTemplateProvider.findToiletsInstruction();
            }

            default -> {
                logger.warn("Unexpected action in LocationPostbackHandler: {}", action);
                yield null;
            }
        };
    }

    @Override
    public int getPriority() {
        return 4;
    }
}
