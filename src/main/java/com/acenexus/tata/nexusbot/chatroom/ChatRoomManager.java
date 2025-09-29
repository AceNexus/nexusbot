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
     * 設定聊天室的管理員狀態
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @param isAdmin  是否為管理員
     * @return 是否成功設定
     */
    boolean setAdminStatus(String roomId, ChatRoom.RoomType roomType, boolean isAdmin);

    /**
     * 設定聊天室的認證等待狀態
     *
     * @param roomId      聊天室 ID
     * @param roomType    聊天室類型
     * @param authPending 是否正在等待密碼輸入
     * @return 是否成功設定
     */
    boolean setAuthPending(String roomId, ChatRoom.RoomType roomType, boolean authPending);

    /**
     * 檢查聊天室是否正在等待密碼輸入
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @return 是否正在等待密碼輸入
     */
    boolean isAuthPending(String roomId, ChatRoom.RoomType roomType);

    /**
     * 檢查聊天室是否為管理員
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @return 是否為管理員聊天室
     */
    boolean isAdminRoom(String roomId, ChatRoom.RoomType roomType);

    /**
     * 設定聊天室的廁所搜尋狀態（等待位置）
     * 智能處理：如果記錄不存在且需要建立新記錄，則需要 roomType
     *
     * @param roomId                 聊天室 ID
     * @param roomType               聊天室類型（可選，僅在建立新記錄時需要）
     * @param waitingForToiletSearch 是否正在等待位置進行廁所搜尋
     * @return 是否成功設定
     */
    boolean setWaitingForToiletSearch(String roomId, ChatRoom.RoomType roomType, boolean waitingForToiletSearch);

    /**
     * 設定聊天室的廁所搜尋狀態（僅修改現有記錄）
     * 用於已確定記錄存在的情況
     *
     * @param roomId                 聊天室 ID
     * @param waitingForToiletSearch 是否正在等待位置進行廁所搜尋
     * @return 是否成功設定
     */
    boolean updateWaitingForToiletSearch(String roomId, boolean waitingForToiletSearch);

    /**
     * 檢查聊天室是否正在等待位置進行廁所搜尋
     *
     * @param roomId 聊天室 ID
     * @return 是否正在等待位置進行廁所搜尋
     */
    boolean isWaitingForToiletSearch(String roomId);
}