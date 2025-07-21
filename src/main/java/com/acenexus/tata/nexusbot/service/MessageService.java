package com.acenexus.tata.nexusbot.service;

import com.linecorp.bot.messaging.client.MessagingApiClient;
import com.linecorp.bot.messaging.model.Message;
import com.linecorp.bot.messaging.model.ReplyMessageRequest;
import com.linecorp.bot.messaging.model.TextMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessagingApiClient messagingApiClient;

    public void sendReply(String replyToken, String messageText) {
        try {
            // 檢查參數有效性
            if (replyToken == null || replyToken.trim().isEmpty()) {
                log.warn("ReplyToken 為空，無法發送回覆訊息");
                return;
            }

            if (messageText == null || messageText.trim().isEmpty()) {
                log.warn("訊息內容為空，不發送回覆");
                return;
            }

            // 檢查訊息長度限制
            if (isMessageTooLong(messageText)) {
                log.warn("訊息長度 {} 超過限制，自動分割", messageText.length());
                List<String> splitMessages = splitLongMessage(messageText);
                sendMultipleReplies(replyToken, splitMessages);
                return;
            }

            // 發送訊息
            TextMessage textMessage = new TextMessage(messageText);
            ReplyMessageRequest request = new ReplyMessageRequest(
                    replyToken,
                    List.of(textMessage),
                    false // notificationDisabled
            );

            messagingApiClient.replyMessage(request);
            log.info("成功回覆訊息給用戶，內容長度: {} 字元", messageText.length());

        } catch (Exception e) {
            // 記錄錯誤但不拋出異常，避免影響 webhook 的 200 回應
            log.error("回覆訊息時發生錯誤，ReplyToken: {}, 錯誤: {}", replyToken, e.getMessage(), e);
        }
    }

    /**
     * 回覆多則訊息給用戶
     *
     * @param replyToken   LINE 平台提供的回覆權杖
     * @param messageTexts 要回覆的多則文字內容
     */
    public void sendMultipleReplies(String replyToken, List<String> messageTexts) {
        try {
            List<Message> textMessages = messageTexts.stream()
                    .map(text -> (Message) new TextMessage(text))
                    .toList();

            ReplyMessageRequest request = new ReplyMessageRequest(
                    replyToken,
                    textMessages,
                    false
            );

            messagingApiClient.replyMessage(request);
            log.info("成功回覆 {} 則訊息給用戶", messageTexts.size());

        } catch (Exception e) {
            log.error("回覆多則訊息時發生錯誤: {}", e.getMessage(), e);
        }
    }

    /**
     * 檢查訊息長度是否超過 LINE 限制
     * LINE Bot 單則訊息最大長度為 5000 字元
     *
     * @param message 要檢查的訊息
     * @return 是否超過長度限制
     */
    public boolean isMessageTooLong(String message) {
        return message.length() > 5000;
    }

    /**
     * 將過長的訊息拆分成多則較短的訊息
     *
     * @param longMessage 過長的訊息
     * @return 拆分後的訊息列表
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