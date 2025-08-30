package com.acenexus.tata.nexusbot.template;

import com.linecorp.bot.model.message.Message;

/**
 * 訊息模板提供者介面
 */
public interface MessageTemplateProvider {

    Message welcome();

    Message about();

    Message success(String message);

    Message error(String message);

    Message mainMenu();

    Message aiSettingsMenu();

    Message aiSettingsMenu(boolean currentStatus);

    Message aiModelSelectionMenu();

    Message aiModelSelectionMenu(String currentModel);

    Message helpMenu();

    Message clearHistoryConfirmation();

    Message postbackResponse(String data);

    String imageResponse(String messageId);

    String stickerResponse(String packageId, String stickerId);

    String videoResponse(String messageId);

    String audioResponse(String messageId);

    String fileResponse(String fileName, long fileSize);

    String locationResponse(String title, String address, double latitude, double longitude);

    String unknownMessage();

    String defaultTextResponse(String messageText);

    String groupJoinMessage(String sourceType);

    String memberJoinedMessage(int memberCount);

    Message systemStats(long totalRooms, long aiEnabledRooms, long adminRooms,
                        long totalMessages, long userMessages, long aiMessages,
                        long todayActiveRooms, long weekActiveRooms, String avgProcessingTime);
}