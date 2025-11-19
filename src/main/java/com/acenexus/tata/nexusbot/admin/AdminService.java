package com.acenexus.tata.nexusbot.admin;

import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.linecorp.bot.model.message.Message;

/**
 * 管理員服務接口
 */
public interface AdminService {

    /**
     * 處理認證相關命令
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @param message  訊息內容
     * @return 處理結果，null 表示非認證相關命令
     */
    String processAuthCommand(String roomId, ChatRoom.RoomType roomType, String message);

    /**
     * 處理管理員命令
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @param message  訊息內容
     * @return 處理結果 Message，null 表示非管理員命令
     */
    Message processAdminCommand(String roomId, ChatRoom.RoomType roomType, String message);

    /**
     * 檢查聊天室是否正在等待密碼輸入
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @return true 如果正在等待密碼輸入
     */
    boolean isAuthPending(String roomId, ChatRoom.RoomType roomType);

    /**
     * 檢查是否為管理員聊天室
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @return true 如果是管理員聊天室
     */
    boolean isAdminRoom(String roomId, ChatRoom.RoomType roomType);
}
