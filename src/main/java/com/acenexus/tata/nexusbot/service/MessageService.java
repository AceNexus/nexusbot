package com.acenexus.tata.nexusbot.service;

import com.linecorp.bot.model.message.Message;

import java.util.List;

/**
 * 消息發送服務接口
 */
public interface MessageService {

    /**
     * 發送文字回覆
     *
     * @param replyToken  回覆 Token
     * @param messageText 消息文字
     */
    void sendReply(String replyToken, String messageText);

    /**
     * 發送 Message 回覆
     *
     * @param replyToken 回覆 Token
     * @param message    Message 對象
     */
    void sendMessage(String replyToken, Message message);

    /**
     * 發送多則訊息
     *
     * @param replyToken   回覆 Token
     * @param messageTexts 消息文字列表
     */
    void sendMultipleReplies(String replyToken, List<String> messageTexts);

    /**
     * 檢查訊息長度是否超過限制
     *
     * @param message 消息文字
     * @return true 如果超過限制
     */
    boolean isMessageTooLong(String message);

    /**
     * 拆分長訊息
     *
     * @param longMessage 長消息
     * @return 拆分後的消息列表
     */
    List<String> splitLongMessage(String longMessage);
}
