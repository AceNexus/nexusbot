package com.acenexus.tata.nexusbot.chatroom;

import com.acenexus.tata.nexusbot.entity.ChatRoom;

/**
 * 聊天室管理服務介面
 */
public interface ChatRoomManager {

    /**
     * 檢查聊天室的 AI 是否啟用
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @return AI 是否啟用
     */
    boolean isAiEnabled(String roomId, ChatRoom.RoomType roomType);

    /**
     * 啟用聊天室的 AI 回應
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @return 是否成功啟用
     */
    boolean enableAi(String roomId, ChatRoom.RoomType roomType);

    /**
     * 停用聊天室的 AI 回應
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @return 是否成功停用
     */
    boolean disableAi(String roomId, ChatRoom.RoomType roomType);

    /**
     * 根據來源類型判斷聊天室類型
     *
     * @param sourceType 來源類型
     * @return 聊天室類型
     */
    ChatRoom.RoomType determineRoomType(String sourceType);

    /**
     * 獲取聊天室的 AI 模型
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @return AI 模型名稱
     */
    String getAiModel(String roomId, ChatRoom.RoomType roomType);

    /**
     * 設定聊天室的 AI 模型
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @param model    AI 模型名稱
     * @return 是否成功設定
     */
    boolean setAiModel(String roomId, ChatRoom.RoomType roomType, String model);

    /**
     * 清除聊天室的歷史對話記錄
     *
     * @param roomId 聊天室 ID
     */
    void clearChatHistory(String roomId);

    /**
     * 找到或創建聊天室
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @return 聊天室實體
     */
    ChatRoom findOrCreateChatRoom(String roomId, ChatRoom.RoomType roomType);
}