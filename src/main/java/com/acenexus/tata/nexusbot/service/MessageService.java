package com.acenexus.tata.nexusbot.service;

import com.linecorp.bot.messaging.client.MessagingApiClient;
import com.linecorp.bot.messaging.model.Message;
import com.linecorp.bot.messaging.model.ReplyMessageRequest;
import com.linecorp.bot.messaging.model.TextMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);
    private final MessagingApiClient messagingApiClient;

    public void sendReply(String replyToken, String messageText) {
        try {
            if (replyToken == null || replyToken.trim().isEmpty()) {
                logger.warn("ReplyToken is empty, cannot send reply message");
                return;
            }

            if (messageText == null || messageText.trim().isEmpty()) {
                logger.warn("Message content is empty, not sending reply");
                return;
            }

            // 檢查訊息長度限制
            if (isMessageTooLong(messageText)) {
                logger.warn("Message length {} exceeds limit, auto-splitting", messageText.length());
                List<String> splitMessages = splitLongMessage(messageText);
                sendMultipleReplies(replyToken, splitMessages);
                return;
            }

            // 發送訊息
            TextMessage textMessage = new TextMessage(messageText);
            ReplyMessageRequest request = new ReplyMessageRequest(replyToken, List.of(textMessage), false);

            messagingApiClient.replyMessage(request);
            logger.info("Successfully replied to user, message length: {} characters", messageText.length());

        } catch (Exception e) {
            // 記錄錯誤但不拋出異常，避免影響 webhook 的 200 回應
            logger.error("Error sending reply message, ReplyToken: {}, Error: {}", replyToken, e.getMessage(), e);
        }
    }

    /**
     * 回覆多則訊息給用戶
     */
    public void sendMultipleReplies(String replyToken, List<String> messageTexts) {
        try {
            List<Message> textMessages = messageTexts.stream()
                    .map(text -> (Message) new TextMessage(text))
                    .toList();

            ReplyMessageRequest request = new ReplyMessageRequest(replyToken, textMessages, false);

            messagingApiClient.replyMessage(request);
            logger.info("Successfully sent {} messages to user", messageTexts.size());

        } catch (Exception e) {
            logger.error("Error sending multiple messages: {}", e.getMessage(), e);
        }
    }

    /**
     * 檢查訊息長度是否超過 LINE 限制
     * LINE Bot 單則訊息最大長度為 5000 字元
     */
    public boolean isMessageTooLong(String message) {
        return message.length() > 5000;
    }

    /**
     * 將過長的訊息拆分成多則較短的訊息
     */
    public List<String> splitLongMessage(String longMessage) {
        List<String> messages = new ArrayList<>();
        int maxLength = 4800; // 留一些緩衝空間

        for (int i = 0; i < longMessage.length(); i += maxLength) {
            int endIndex = Math.min(i + maxLength, longMessage.length());
            messages.add(longMessage.substring(i, endIndex));
        }

        return messages;
    }
}