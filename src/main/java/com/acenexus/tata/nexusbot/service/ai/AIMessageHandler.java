package com.acenexus.tata.nexusbot.service.ai;

import com.acenexus.tata.nexusbot.entity.ChatRoom;

/**
 * AI 消息處理接口
 */
public interface AIMessageHandler {

    /**
     * 處理 AI 對話
     *
     * @param roomId      聊天室 ID
     * @param roomType    聊天室類型
     * @param userId      用戶 ID
     * @param messageText 消息內容
     * @param replyToken  回覆 Token
     */
    void handleAIMessage(String roomId, ChatRoom.RoomType roomType, String userId, String messageText, String replyToken);
}
