package com.acenexus.tata.nexusbot.constants;

public class BotMessages {

    public static final String GREETING = "你好！我是 NexusBot，很高興為您服務！";
    public static final String ABOUT = "我是 NexusBot v1.0，一個智能 LINE 機器人助手。";

    public static final String WELCOME_MESSAGE =
            "🎉 歡迎加入 NexusBot！\n\n" +
                    "感謝您的支持，我將為您提供最佳的服務體驗。\n\n" +
                    "✨ 可用功能：\n" +
                    "• 輸入 'help' 查看指令\n" +
                    "• 輸入 'menu' 查看選單\n" +
                    "• 發送圖片、影片、位置等多媒體內容\n\n" +
                    "如有任何問題，請隨時與我互動！";

    public static String getHelpMessage() {
        return "📖 NexusBot 使用說明\n\n" +
                "🔤 文字指令：\n" +
                "• hello / hi / 你好 - 打招呼\n" +
                "• help / 幫助 - 顯示此說明\n" +
                "• menu / 選單 - 顯示功能選單\n" +
                "• about / 關於 - 關於機器人\n\n" +
                "❓ 如有其他問題，請直接輸入訊息與我互動！";
    }

    public static String getMenuMessage() {
        return "🎛️ NexusBot 功能選單\n\n" +
                "📋 主要功能：\n" +
                "1️⃣ 文字訊息處理\n" +
                "2️⃣ 多媒體內容支援\n" +
                "3️⃣ 位置資訊服務\n" +
                "4️⃣ 檔案處理功能\n" +
                "5️⃣ 互動式按鈕\n\n" +
                "💡 試試發送不同類型的訊息給我，看看我如何回應！";
    }

    public static String getDefaultTextResponse(String messageText) {
        return "收到您的訊息：" + messageText + "\n請輸入 'help' 查看可用指令。";
    }

    public static String getImageResponse(String messageId) {
        return "收到您的圖片！\n圖片ID: " + messageId;
    }

    public static String getStickerResponse(String packageId, String stickerId) {
        return String.format("很可愛的貼圖！😊\n貼圖包ID: %s\n貼圖ID: %s", packageId, stickerId);
    }

    public static String getVideoResponse(String messageId) {
        return "收到您的影片！\n影片ID: " + messageId;
    }

    public static String getAudioResponse(String messageId) {
        return "收到您的音檔！\n音檔ID: " + messageId;
    }

    public static String getFileResponse(String fileName, long fileSize) {
        return String.format("收到您的檔案！\n檔名: %s\n大小: %d bytes", fileName, fileSize);
    }

    public static String getLocationResponse(String title, String address, double latitude, double longitude) {
        StringBuilder response = new StringBuilder("收到您的位置資訊！");
        if (title != null && !title.trim().isEmpty()) {
            response.append("\n地點名稱: ").append(title);
        }
        if (address != null && !address.trim().isEmpty()) {
            response.append("\n地址: ").append(address);
        }
        response.append(String.format("\n座標: %.6f, %.6f", latitude, longitude));
        return response.toString();
    }

    public static final String UNKNOWN_MESSAGE_TYPE = "收到您的訊息，但目前無法識別此類型。";

    public static String getGroupJoinMessage(String sourceType) {
        return "🎉 Hello everyone! I'm NexusBot!\nHappy to join this " + 
               ("group".equals(sourceType) ? "group" : "room") + "!";
    }

    public static String getMemberJoinedMessage(int memberCount) {
        return "🎊 Welcome new members!\n" + memberCount + " new friends joined the group!";
    }

    public static String getPostbackResponse(String data) {
        return "Button clicked: " + data + "\nThank you for the interaction!";
    }
}