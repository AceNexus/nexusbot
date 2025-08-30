package com.acenexus.tata.nexusbot.service;

import com.acenexus.tata.nexusbot.chatroom.ChatRoomManager;
import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final ChatRoomManager chatRoomManager;
    private final DynamicPasswordService dynamicPasswordService;
    private final SystemStatsService systemStatsService;

    /**
     * 處理認證相關命令
     *
     * @param roomId   聊天室 ID
     * @param roomType 聊天室類型
     * @param message  訊息內容
     * @return 處理結果，null 表示非認證相關命令
     */
    public String processAuthCommand(String roomId, ChatRoom.RoomType roomType, String message) {
        if (message == null) {
            return null;
        }

        String trimmedMessage = message.trim();

        // 處理開始認證命令 /auth
        if (trimmedMessage.equals("/auth")) {
            return handleAuthStart(roomId, roomType);
        }

        // 如果聊天室正在等待密碼輸入，處理密碼
        if (chatRoomManager.isAuthPending(roomId, roomType)) {
            return handlePasswordInput(roomId, roomType, trimmedMessage);
        }

        return null;
    }

    /**
     * 處理管理員命令
     */
    public Message processAdminCommand(String roomId, ChatRoom.RoomType roomType, String message) {
        // 檢查是否為管理員聊天室
        if (!chatRoomManager.isAdminRoom(roomId, roomType)) {
            return null;
        }

        if (message == null) {
            return null;
        }

        String command = message.trim().toLowerCase();

        return switch (command) {
            case "/stats" -> systemStatsService.getSystemStats();
            default -> null; // 非管理員命令
        };
    }


    /**
     * 處理開始認證命令
     */
    private String handleAuthStart(String roomId, ChatRoom.RoomType roomType) {
        // 設定聊天室為等待密碼輸入狀態
        boolean success = chatRoomManager.setAuthPending(roomId, roomType, true);
        if (success) {
            log.info("Admin authentication started for room: {}", roomId);
            return "請輸入密碼";
        } else {
            return "認證失敗：無法啟動認證流程";
        }
    }

    /**
     * 處理密碼輸入
     */
    private String handlePasswordInput(String roomId, ChatRoom.RoomType roomType, String inputPassword) {
        String currentPassword = dynamicPasswordService.getCurrentPassword();

        if (currentPassword.equals(inputPassword.trim())) {
            // 認證成功，設定聊天室為管理員，清除等待狀態
            boolean adminSuccess = chatRoomManager.setAdminStatus(roomId, roomType, true);
            boolean pendingSuccess = chatRoomManager.setAuthPending(roomId, roomType, false);

            if (adminSuccess && pendingSuccess) {
                log.info("Admin authentication successful for room: {} with dynamic password", roomId);
                return "此聊天室現在擁有管理員權限";
            } else {
                return "認證失敗：無法設定管理員權限";
            }
        } else {
            // 密碼錯誤，清除等待狀態
            chatRoomManager.setAuthPending(roomId, roomType, false);
            log.warn("Failed admin authentication attempt for room: {} (expected: {}, got: {})", roomId, currentPassword, inputPassword);
            return "認證失敗：密碼錯誤";
        }
    }
}