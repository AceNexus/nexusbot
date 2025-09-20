package com.acenexus.tata.nexusbot.template;

import com.acenexus.tata.nexusbot.entity.Reminder;
import com.linecorp.bot.model.message.Message;

import java.util.List;
import java.util.Map;

/**
 * 訊息模板提供者介面
 */
public interface MessageTemplateProvider {

    Message welcome();

    Message about();

    Message success(String message);

    Message error(String message);

    Message mainMenu();

    Message aiSettingsMenu(boolean currentStatus);

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

    Message reminderMenu();

    Message reminderRepeatTypeMenu();

    Message reminderInputMenu(String step);

    Message reminderInputMenu(String step, String reminderTime);

    Message reminderCreatedSuccess(String reminderTime, String repeatType, String content);

    Message reminderInputError(String userInput, String aiResult);

    Message reminderList(List<Reminder> reminders, Map<Long, String> userResponseStatuses);

    Message buildReminderNotification(String enhancedContent, String originalContent, String repeatType, Long reminderId);
}