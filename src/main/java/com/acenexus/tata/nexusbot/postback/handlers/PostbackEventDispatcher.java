package com.acenexus.tata.nexusbot.postback.handlers;

import com.acenexus.tata.nexusbot.chatroom.ChatRoomManager;
import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.postback.PostbackHandler;
import com.acenexus.tata.nexusbot.service.MessageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Postback 事件分發器
 * 負責接收 LINE Postback 事件，根據動作類型分發給對應的 Handler 處理。
 * 遵循 Chain of Responsibility Pattern，依照優先順序逐一嘗試各個 Handler。
 * <p>
 * <架構設計>
 * PostbackEventHandler
 * ↓
 * PostbackEventDispatcher (分發器)
 * ↓
 * ├─ ReminderPostbackHandler (提醒功能)
 * ├─ EmailPostbackHandler (Email 功能)
 * ├─ LocationPostbackHandler (位置功能)
 * ├─ AIPostbackHandler (AI 對話功能)
 * └─ NavigationPostbackHandler (導航功能)
 * </pre>
 *
 * @see PostbackHandler
 */
@Component
@RequiredArgsConstructor
public class PostbackEventDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(PostbackEventDispatcher.class);

    private final List<PostbackHandler> handlers;
    private final MessageService messageService;
    private final ChatRoomManager chatRoomManager;

    /**
     * 分發 Postback 事件給對應的 Handler 處理
     *
     * @param event LINE Postback Event JSON 物件
     */
    public void dispatch(JsonNode event) {
        try {
            // 1. 提取基本資訊
            JsonNode postback = event.get("postback");
            if (postback == null) {
                logger.warn("Postback event missing 'postback' field");
                return;
            }

            String action = postback.get("data").asText();
            String replyToken = event.get("replyToken").asText();

            // 2. 獲取來源資訊
            JsonNode source = event.get("source");
            String sourceType = source.get("type").asText();
            String roomId = sourceType.equals("group") ? source.get("groupId").asText() : source.get("userId").asText();

            ChatRoom.RoomType roomType = chatRoomManager.determineRoomType(sourceType);

            logger.info("Dispatching postback: roomId={}, roomType={}, action={}", roomId, roomType, action);

            // 3. 按優先順序尋找可處理的 Handler
            List<PostbackHandler> sortedHandlers = handlers.stream()
                    .sorted(Comparator.comparingInt(PostbackHandler::getPriority))
                    .toList();

            for (PostbackHandler handler : sortedHandlers) {
                if (handler.canHandle(action)) {
                    logger.debug("Handler {} can handle action: {}", handler.getClass().getSimpleName(), action);

                    Message response = handler.handle(action, roomId, roomType.name(), replyToken, event);

                    // 4. 如果 Handler 回傳訊息，則發送並結束分發
                    if (response != null) {
                        messageService.sendMessage(replyToken, response);
                        logger.info("Postback handled by {} for action: {}", handler.getClass().getSimpleName(), action);
                        return;
                    }
                }
            }

            // 5. 沒有任何 Handler 可以處理
            logger.warn("No handler found for postback action: {}", action);
            messageService.sendMessage(replyToken, createUnknownActionResponse(action));

        } catch (Exception e) {
            logger.error("Error dispatching postback event", e);
            try {
                String replyToken = event.get("replyToken").asText();
                messageService.sendMessage(replyToken, createErrorResponse());
            } catch (Exception ex) {
                logger.error("Failed to send error response", ex);
            }
        }
    }

    /**
     * 建立未知動作的回應訊息
     */
    private Message createUnknownActionResponse(String action) {
        return com.linecorp.bot.model.message.TextMessage.builder()
                .text("抱歉，無法識別此操作。請返回主選單重新選擇。")
                .build();
    }

    /**
     * 建立錯誤回應訊息
     */
    private Message createErrorResponse() {
        return com.linecorp.bot.model.message.TextMessage.builder()
                .text("處理您的請求時發生錯誤，請稍後再試。")
                .build();
    }
}
