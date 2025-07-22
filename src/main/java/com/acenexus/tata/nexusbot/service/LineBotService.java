package com.acenexus.tata.nexusbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LineBotService {

    private final MessageService messageService;

    /**
     * 處理文字訊息
     */
    public void handleTextMessage(String userId, String messageText, String replyToken) {
        try {
            log.info("處理文字訊息 - 用戶: {}, 內容: {}", userId, messageText);

            // 基本指令處理
            String response = switch (messageText.toLowerCase().trim()) {
                case "hello", "hi", "你好" -> "你好！我是 NexusBot，很高興為您服務！";
                case "help", "幫助", "?" -> getHelpMessage();
                case "menu", "選單" -> getMenuMessage();
                case "about", "關於" -> "我是 NexusBot v1.0，一個智能 LINE 機器人助手。";
                default -> "收到您的訊息：" + messageText + "\n請輸入 'help' 查看可用指令。";
            };

            messageService.sendReply(replyToken, response);

        } catch (Exception e) {
            log.error("處理文字訊息時發生錯誤 - 用戶: {}, 錯誤: {}", userId, e.getMessage(), e);
            messageService.sendReply(replyToken, "處理訊息時發生錯誤，請稍後再試。");
        }
    }

    /**
     * 處理圖片訊息
     */
    public void handleImageMessage(String userId, String messageId, String replyToken) {
        try {
            log.info("處理圖片訊息 - 用戶: {}, 訊息ID: {}", userId, messageId);

            String response = "收到您的圖片！\n" +
                    "圖片ID: " + messageId + "\n" +
                    "您可以發送文字訊息與我互動。";

            messageService.sendReply(replyToken, response);

        } catch (Exception e) {
            log.error("處理圖片訊息時發生錯誤 - 用戶: {}, 錯誤: {}", userId, e.getMessage(), e);
            messageService.sendReply(replyToken, "處理圖片時發生錯誤。");
        }
    }

    /**
     * 處理影片訊息
     */
    public void handleVideoMessage(String userId, String messageId, String replyToken) {
        try {
            log.info("處理影片訊息 - 用戶: {}, 訊息ID: {}", userId, messageId);

            String response = "收到您的影片！\n" +
                    "影片ID: " + messageId + "\n" +
                    "感謝您的分享！";

            messageService.sendReply(replyToken, response);

        } catch (Exception e) {
            log.error("處理影片訊息時發生錯誤 - 用戶: {}, 錯誤: {}", userId, e.getMessage(), e);
            messageService.sendReply(replyToken, "處理影片時發生錯誤。");
        }
    }

    /**
     * 處理音訊訊息
     */
    public void handleAudioMessage(String userId, String messageId, String replyToken) {
        try {
            log.info("處理音訊訊息 - 用戶: {}, 訊息ID: {}", userId, messageId);

            String response = "收到您的語音訊息！\n" +
                    "音訊ID: " + messageId + "\n" +
                    "目前暫不支援語音識別功能。";

            messageService.sendReply(replyToken, response);

        } catch (Exception e) {
            log.error("處理音訊訊息時發生錯誤 - 用戶: {}, 錯誤: {}", userId, e.getMessage(), e);
            messageService.sendReply(replyToken, "處理音訊時發生錯誤。");
        }
    }

    /**
     * 處理檔案訊息
     */
    public void handleFileMessage(String userId, String messageId, String fileName, long fileSize, String replyToken) {
        try {
            log.info("處理檔案訊息 - 用戶: {}, 檔名: {}, 大小: {} bytes", userId, fileName, fileSize);

            String sizeString = formatFileSize(fileSize);
            String response = "收到您的檔案！\n" +
                    "檔案名稱: " + fileName + "\n" +
                    "檔案大小: " + sizeString + "\n" +
                    "檔案ID: " + messageId;

            messageService.sendReply(replyToken, response);

        } catch (Exception e) {
            log.error("處理檔案訊息時發生錯誤 - 用戶: {}, 錯誤: {}", userId, e.getMessage(), e);
            messageService.sendReply(replyToken, "處理檔案時發生錯誤。");
        }
    }

    /**
     * 處理位置訊息
     */
    public void handleLocationMessage(String userId, String title, String address, double latitude, double longitude, String replyToken) {
        try {
            log.info("處理位置訊息 - 用戶: {}, 地點: {}", userId, title);

            String response = "收到您的位置資訊！\n" +
                    "地點: " + (title != null ? title : "未命名地點") + "\n" +
                    "地址: " + (address != null ? address : "無地址資訊") + "\n" +
                    "座標: " + String.format("%.6f, %.6f", latitude, longitude);

            messageService.sendReply(replyToken, response);

        } catch (Exception e) {
            log.error("處理位置訊息時發生錯誤 - 用戶: {}, 錯誤: {}", userId, e.getMessage(), e);
            messageService.sendReply(replyToken, "處理位置資訊時發生錯誤。");
        }
    }

    /**
     * 處理貼圖訊息
     */
    public void handleStickerMessage(String userId, String packageId, String stickerId, String replyToken) {
        try {
            log.info("處理貼圖訊息 - 用戶: {}, 貼圖: {}:{}", userId, packageId, stickerId);

            String response = "很可愛的貼圖！😊\n" +
                    "貼圖包ID: " + packageId + "\n" +
                    "貼圖ID: " + stickerId;

            messageService.sendReply(replyToken, response);

        } catch (Exception e) {
            log.error("處理貼圖訊息時發生錯誤 - 用戶: {}, 錯誤: {}", userId, e.getMessage(), e);
            messageService.sendReply(replyToken, "處理貼圖時發生錯誤。");
        }
    }

    /**
     * 處理 Postback 事件
     */
    public void handlePostback(String userId, String data, String dateParam, String timeParam, String datetimeParam, String replyToken) {
        try {
            log.info("處理 Postback - 用戶: {}, 資料: {}", userId, data);

            String response = switch (data) {
                case "action_help" -> getHelpMessage();
                case "action_menu" -> getMenuMessage();
                case "action_about" -> "關於 NexusBot\n\n我是一個功能豐富的 LINE 機器人，提供多種服務功能。";
                default -> "收到按鈕點擊：" + data + "\n感謝您的互動！";
            };

            // 如果有日期時間參數
            if (dateParam != null) {
                response += "\n選擇的日期：" + dateParam;
            }
            if (timeParam != null) {
                response += "\n選擇的時間：" + timeParam;
            }
            if (datetimeParam != null) {
                response += "\n選擇的日期時間：" + datetimeParam;
            }

            messageService.sendReply(replyToken, response);

        } catch (Exception e) {
            log.error("處理 Postback 時發生錯誤 - 用戶: {}, 錯誤: {}", userId, e.getMessage(), e);
            messageService.sendReply(replyToken, "處理按鈕點擊時發生錯誤。");
        }
    }

    /**
     * 處理新用戶加好友事件
     */
    public void handleNewFollower(String userId, String replyToken) {
        try {
            log.info("新用戶加好友 - 用戶: {}", userId);

            String welcomeMessage = "🎉 歡迎加入 NexusBot！\n\n" +
                    "感謝您的支持，我將為您提供最佳的服務體驗。\n\n" +
                    "✨ 可用功能：\n" +
                    "• 輸入 'help' 查看指令\n" +
                    "• 輸入 'menu' 查看選單\n" +
                    "• 發送圖片、影片、位置等多媒體內容\n\n" +
                    "如有任何問題，請隨時與我互動！";

            messageService.sendReply(replyToken, welcomeMessage);

        } catch (Exception e) {
            log.error("處理新用戶加好友時發生錯誤 - 用戶: {}, 錯誤: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * 處理用戶取消好友事件
     */
    public void handleUnfollow(String userId) {
        try {
            log.info("用戶取消好友 - 用戶: {}", userId);
            // 可以在這裡記錄用戶離開的統計資訊
            // 注意：UnfollowEvent 沒有 replyToken，無法回覆訊息

        } catch (Exception e) {
            log.error("處理用戶取消好友時發生錯誤 - 用戶: {}, 錯誤: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * 處理加入群組事件
     */
    public void handleJoinGroup(String groupId, String replyToken) {
        try {
            log.info("加入群組 - 群組ID: {}", groupId);

            String joinMessage = "🎉 大家好！我是 NexusBot！\n\n" +
                    "很高興加入這個群組，我將為大家提供服務。\n\n" +
                    "📋 群組功能：\n" +
                    "• 輸入 'help' 查看可用指令\n" +
                    "• 支援多媒體訊息處理\n" +
                    "• 互動式按鈕功能\n\n" +
                    "請多多指教！ 🤖";

            messageService.sendReply(replyToken, joinMessage);

        } catch (Exception e) {
            log.error("處理加入群組時發生錯誤 - 群組: {}, 錯誤: {}", groupId, e.getMessage(), e);
        }
    }

    /**
     * 處理加入聊天室事件
     */
    public void handleJoinRoom(String roomId, String replyToken) {
        try {
            log.info("加入聊天室 - 聊天室ID: {}", roomId);

            String joinMessage = "👋 大家好！我是 NexusBot！\n\n" +
                    "很高興加入這個聊天室，準備好為大家服務了！\n\n" +
                    "輸入 'help' 查看我能做什麼 😊";

            messageService.sendReply(replyToken, joinMessage);

        } catch (Exception e) {
            log.error("處理加入聊天室時發生錯誤 - 聊天室: {}, 錯誤: {}", roomId, e.getMessage(), e);
        }
    }

    /**
     * 處理離開群組事件
     */
    public void handleLeaveGroup(String groupId) {
        try {
            log.info("離開群組 - 群組ID: {}", groupId);
            // 可以在這裡記錄群組使用統計
            // 注意：LeaveEvent 沒有 replyToken，無法回覆訊息

        } catch (Exception e) {
            log.error("處理離開群組時發生錯誤 - 群組: {}, 錯誤: {}", groupId, e.getMessage(), e);
        }
    }

    /**
     * 處理離開聊天室事件
     */
    public void handleLeaveRoom(String roomId) {
        try {
            log.info("離開聊天室 - 聊天室ID: {}", roomId);
            // 可以在這裡記錄聊天室使用統計

        } catch (Exception e) {
            log.error("處理離開聊天室時發生錯誤 - 聊天室: {}, 錯誤: {}", roomId, e.getMessage(), e);
        }
    }

    // ========== 群組成員變動事件處理方法 ==========

    /**
     * 處理群組新成員加入事件
     */
    public void handleMemberJoinedGroup(String groupId, int memberCount, String replyToken) {
        try {
            log.info("群組新成員加入 - 群組: {}, 人數: {}", groupId, memberCount);

            String welcomeMessage = "🎊 歡迎新成員加入！\n\n" +
                    "有 " + memberCount + " 位新朋友加入了群組～\n" +
                    "歡迎大家多多互動！ 😊";

            messageService.sendReply(replyToken, welcomeMessage);

        } catch (Exception e) {
            log.error("處理群組新成員加入時發生錯誤 - 群組: {}, 錯誤: {}", groupId, e.getMessage(), e);
        }
    }

    /**
     * 處理群組成員離開事件
     */
    public void handleMemberLeftGroup(String groupId, int memberCount) {
        try {
            log.info("群組成員離開 - 群組: {}, 人數: {}", groupId, memberCount);
            // 可以在這裡記錄成員變動統計
            // 注意：MemberLeftEvent 沒有 replyToken，無法回覆訊息

        } catch (Exception e) {
            log.error("處理群組成員離開時發生錯誤 - 群組: {}, 錯誤: {}", groupId, e.getMessage(), e);
        }
    }

    // ========== 輔助方法 ==========

    /**
     * 取得幫助訊息
     */
    private String getHelpMessage() {
        return "📖 NexusBot 使用說明\n\n" +
                "🔤 文字指令：\n" +
                "• hello / hi / 你好 - 打招呼\n" +
                "• help / 幫助 - 顯示此說明\n" +
                "• menu / 選單 - 顯示功能選單\n" +
                "• about / 關於 - 關於機器人\n\n" +
                "📱 支援功能：\n" +
                "• 文字訊息互動\n" +
                "• 圖片、影片、音訊處理\n" +
                "• 位置資訊分享\n" +
                "• 檔案上傳處理\n" +
                "• 貼圖互動\n" +
                "• 按鈕式選單\n\n" +
                "❓ 如有其他問題，請直接輸入訊息與我互動！";
    }

    /**
     * 取得選單訊息
     */
    private String getMenuMessage() {
        return "🎛️ NexusBot 功能選單\n\n" +
                "📋 主要功能：\n" +
                "1️⃣ 文字訊息處理\n" +
                "2️⃣ 多媒體內容支援\n" +
                "3️⃣ 位置資訊服務\n" +
                "4️⃣ 檔案處理功能\n" +
                "5️⃣ 互動式按鈕\n\n" +
                "🔧 管理功能：\n" +
                "• 群組管理\n" +
                "• 成員歡迎\n" +
                "• 事件記錄\n\n" +
                "💡 試試發送不同類型的訊息給我，看看我如何回應！";
    }

    /**
     * 格式化檔案大小
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}