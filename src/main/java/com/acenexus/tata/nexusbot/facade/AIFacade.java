package com.acenexus.tata.nexusbot.facade;

import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.linecorp.bot.model.message.Message;

/**
 * AI 功能 Facade - 協調 AI 設定和對話歷史管理
 */
public interface AIFacade {

    /**
     * 顯示 AI 設定選單
     */
    Message showSettingsMenu(String roomId, ChatRoom.RoomType roomType);

    /**
     * 啟用 AI
     */
    Message enableAI(String roomId, ChatRoom.RoomType roomType);

    /**
     * 停用 AI
     */
    Message disableAI(String roomId, ChatRoom.RoomType roomType);

    /**
     * 顯示廠商與模型選擇 Carousel
     */
    Message showProviderAndModelMenu(String roomId, ChatRoom.RoomType roomType);

    /**
     * 選擇 AI 模型
     */
    Message selectModel(String roomId, ChatRoom.RoomType roomType, String modelId, String modelName);

    /**
     * 顯示清除歷史確認選單
     */
    Message showClearHistoryConfirmation();

    /**
     * 清除對話歷史
     */
    Message clearHistory(String roomId);
}
