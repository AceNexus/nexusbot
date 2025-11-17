package com.acenexus.tata.nexusbot.facade;

import com.acenexus.tata.nexusbot.chatroom.ChatRoomManager;
import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.location.LocationService;
import com.acenexus.tata.nexusbot.service.MessageService;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.TextMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 位置功能 Facade 實作
 */
@Service
@RequiredArgsConstructor
public class LocationFacadeImpl implements LocationFacade {

    private static final Logger logger = LoggerFactory.getLogger(LocationFacadeImpl.class);

    private final ChatRoomManager chatRoomManager;
    private final LocationService locationService;
    private final MessageService messageService;
    private final MessageTemplateProvider messageTemplateProvider;

    @Override
    public Message startToiletSearch(String roomId, ChatRoom.RoomType roomType) {
        chatRoomManager.setWaitingForToiletSearch(roomId, roomType, true);
        logger.info("Set waiting for toilet search for room: {}", roomId);
        return messageTemplateProvider.findToiletsInstruction();
    }

    @Override
    public Message handleLocationMessage(String roomId, String title, String address, double latitude, double longitude, String replyToken) {
        logger.info("Location message processed from room {}: title={}, address={}, lat={}, lon={}", roomId, title, address, latitude, longitude);

        // 檢查是否正在等待位置以搜尋廁所
        boolean isWaitingForToiletSearch = chatRoomManager.isWaitingForToiletSearch(roomId);

        if (isWaitingForToiletSearch) {
            // 清除廁所搜尋等待狀態
            chatRoomManager.updateWaitingForToiletSearch(roomId, false);
            logger.info("Processing toilet search for room {} with location: lat={}, lon={}", roomId, latitude, longitude);

            // 非同步搜尋廁所
            CompletableFuture.runAsync(() -> searchToilets(latitude, longitude, replyToken, title, address));

            return null; // 非同步處理,不需要立即回傳訊息
        } else {
            // 一般位置訊息處理,僅回覆位置資訊
            logger.info("General location message processed for room {}", roomId);
            String response = messageTemplateProvider.locationResponse(title, address, latitude, longitude);
            return TextMessage.builder().text(response).build();
        }
    }

    private void searchToilets(double latitude, double longitude, String replyToken, String title, String address) {
        try {
            locationService.findNearbyToilets(latitude, longitude, 1000)
                    .thenAccept(toilets -> {
                        Message response = messageTemplateProvider.nearbyToiletsResponse(toilets, latitude, longitude);
                        messageService.sendMessage(replyToken, response);
                    })
                    .exceptionally(throwable -> {
                        logger.error("Error finding nearby toilets", throwable);
                        String fallbackResponse = messageTemplateProvider.locationResponse(title, address, latitude, longitude);
                        messageService.sendReply(replyToken, fallbackResponse);
                        return null;
                    });
        } catch (Exception e) {
            logger.error("Error processing location for toilet search", e);
            String fallbackResponse = messageTemplateProvider.locationResponse(title, address, latitude, longitude);
            messageService.sendReply(replyToken, fallbackResponse);
        }
    }
}
