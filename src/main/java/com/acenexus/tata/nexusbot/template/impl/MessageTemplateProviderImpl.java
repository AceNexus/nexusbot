package com.acenexus.tata.nexusbot.template.impl;

import com.acenexus.tata.nexusbot.entity.Reminder;
import com.acenexus.tata.nexusbot.location.ToiletLocation;
import com.acenexus.tata.nexusbot.template.AISettingsTemplateBuilder;
import com.acenexus.tata.nexusbot.template.EmailTemplateBuilder;
import com.acenexus.tata.nexusbot.template.LocationTemplateBuilder;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.acenexus.tata.nexusbot.template.NavigationTemplateBuilder;
import com.acenexus.tata.nexusbot.template.ReminderTemplateBuilder;
import com.acenexus.tata.nexusbot.template.TimezoneTemplateBuilder;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 訊息範本提供者的門面實作
 * 將請求委派給各個專門的 Builder
 */
@Service
@RequiredArgsConstructor
public class MessageTemplateProviderImpl implements MessageTemplateProvider {

    private final NavigationTemplateBuilder navigationBuilder;
    private final AISettingsTemplateBuilder aiSettingsBuilder;
    private final ReminderTemplateBuilder reminderBuilder;
    private final EmailTemplateBuilder emailBuilder;
    private final LocationTemplateBuilder locationBuilder;
    private final TimezoneTemplateBuilder timezoneBuilder;

    // ==================== 導航與通用範本 ====================

    @Override
    public Message welcome() {
        return navigationBuilder.welcome();
    }

    @Override
    public Message about() {
        return navigationBuilder.about();
    }

    @Override
    public Message success(String message) {
        return navigationBuilder.success(message);
    }

    @Override
    public Message error(String message) {
        return navigationBuilder.error(message);
    }

    @Override
    public Message mainMenu() {
        return navigationBuilder.mainMenu();
    }

    @Override
    public Message helpMenu() {
        return navigationBuilder.helpMenu();
    }

    @Override
    public Message postbackResponse(String data) {
        return navigationBuilder.postbackResponse(data);
    }

    @Override
    public String imageResponse(String messageId) {
        return navigationBuilder.imageResponse(messageId);
    }

    @Override
    public String stickerResponse(String packageId, String stickerId) {
        return navigationBuilder.stickerResponse(packageId, stickerId);
    }

    @Override
    public String videoResponse(String messageId) {
        return navigationBuilder.videoResponse(messageId);
    }

    @Override
    public String audioResponse(String messageId) {
        return navigationBuilder.audioResponse(messageId);
    }

    @Override
    public String fileResponse(String fileName, long fileSize) {
        return navigationBuilder.fileResponse(fileName, fileSize);
    }

    @Override
    public String locationResponse(String title, String address, double latitude, double longitude) {
        return navigationBuilder.locationResponse(title, address, latitude, longitude);
    }

    @Override
    public String unknownMessage() {
        return navigationBuilder.unknownMessage();
    }

    @Override
    public String defaultTextResponse(String messageText) {
        return navigationBuilder.defaultTextResponse(messageText);
    }

    @Override
    public String groupJoinMessage(String sourceType) {
        return navigationBuilder.groupJoinMessage(sourceType);
    }

    @Override
    public String memberJoinedMessage(int memberCount) {
        return navigationBuilder.memberJoinedMessage(memberCount);
    }

    @Override
    public Message systemStats(long totalRooms, long aiEnabledRooms, long adminRooms,
                               long totalMessages, long userMessages, long aiMessages,
                               long todayActiveRooms, long weekActiveRooms, String avgProcessingTime) {
        return navigationBuilder.systemStats(totalRooms, aiEnabledRooms, adminRooms,
                totalMessages, userMessages, aiMessages,
                todayActiveRooms, weekActiveRooms, avgProcessingTime);
    }

    // ==================== AI 設定範本 ====================

    @Override
    public Message aiSettingsMenu(boolean currentStatus) {
        return aiSettingsBuilder.aiSettingsMenu(currentStatus);
    }

    @Override
    public Message aiModelSelectionMenu(String currentModel) {
        return aiSettingsBuilder.aiModelSelectionMenu(currentModel);
    }

    @Override
    public Message clearHistoryConfirmation() {
        return aiSettingsBuilder.clearHistoryConfirmation();
    }

    // ==================== 提醒範本 ====================

    @Override
    public Message reminderMenu() {
        return reminderBuilder.reminderMenu();
    }

    @Override
    public Message reminderRepeatTypeMenu() {
        return reminderBuilder.reminderRepeatTypeMenu();
    }

    @Override
    public Message reminderNotificationChannelMenu() {
        return reminderBuilder.reminderNotificationChannelMenu();
    }

    @Override
    public Message reminderInputMenu(String step, String reminderTime, String timezoneDisplay) {
        return reminderBuilder.reminderInputMenu(step, reminderTime, timezoneDisplay);
    }

    @Override
    public Message reminderCreatedSuccess(String reminderTime, String repeatType, String content, String timezoneDisplay) {
        return reminderBuilder.reminderCreatedSuccess(reminderTime, repeatType, content, timezoneDisplay);
    }

    @Override
    public Message reminderInputError(String userInput, String aiResult) {
        return reminderBuilder.reminderInputError(userInput, aiResult);
    }

    @Override
    public Message reminderList(List<Reminder> reminders, Map<Long, String> userResponseStatuses) {
        return reminderBuilder.reminderList(reminders, userResponseStatuses);
    }

    @Override
    public Message todayReminderLogs(java.util.List<com.acenexus.tata.nexusbot.reminder.ReminderLogService.TodayReminderLog> logs) {
        return reminderBuilder.todayReminderLogs(logs);
    }

    @Override
    public Message buildReminderNotification(String enhancedContent, String originalContent, String repeatType,
                                             Long reminderId, String timezoneDisplay, String reminderTimeDisplay) {
        return reminderBuilder.buildReminderNotification(enhancedContent, originalContent, repeatType, reminderId, timezoneDisplay, reminderTimeDisplay);
    }

    @Override
    public Message timezoneInputMenu(String currentTimezone) {
        return reminderBuilder.timezoneInputMenu(currentTimezone);
    }

    @Override
    public Message timezoneConfirmationMenu(String resolvedTimezone, String timezoneDisplay,
                                            String newReminderTime, String originalInput) {
        return reminderBuilder.timezoneConfirmationMenu(resolvedTimezone, timezoneDisplay, newReminderTime, originalInput);
    }

    @Override
    public Message timezoneInputError(String userInput) {
        return reminderBuilder.timezoneInputError(userInput);
    }

    // ==================== 時區設定範本 ====================

    @Override
    public Message timezoneSettingsMenu(String currentTimezone, String timezoneDisplay) {
        return timezoneBuilder.timezoneSettingsMenu(currentTimezone, timezoneDisplay);
    }

    @Override
    public Message timezoneChangePrompt(String currentTimezone) {
        return timezoneBuilder.timezoneChangePrompt(currentTimezone);
    }

    @Override
    public Message timezoneConfirmation(String resolvedTimezone, String timezoneDisplay, String originalInput) {
        return timezoneBuilder.timezoneConfirmationMenu(resolvedTimezone, timezoneDisplay, originalInput);
    }

    @Override
    public Message timezoneParseError(String userInput) {
        return timezoneBuilder.timezoneInputError(userInput);
    }

    @Override
    public Message timezoneUpdateSuccess(String newTimezone, String timezoneDisplay) {
        return timezoneBuilder.timezoneUpdateSuccess(newTimezone, timezoneDisplay);
    }

    @Override
    public Message timezoneCancelMessage(String currentTimezone, String timezoneDisplay) {
        return timezoneBuilder.timezoneCancelMessage(currentTimezone, timezoneDisplay);
    }

    // ==================== Email 範本 ====================

    @Override
    public Message emailSettingsMenu(List<com.acenexus.tata.nexusbot.entity.Email> emails) {
        return emailBuilder.emailSettingsMenu(emails);
    }

    @Override
    public Message emailInputPrompt() {
        return emailBuilder.emailInputPrompt();
    }

    @Override
    public Message emailAddSuccess(String email) {
        return emailBuilder.emailAddSuccess(email);
    }

    @Override
    public Message emailInvalidFormat() {
        return emailBuilder.emailInvalidFormat();
    }

    // ==================== 台股分析範本 ====================

    @Override
    public Message stockAnalysisMenu() {
        return navigationBuilder.stockAnalysisMenu();
    }

    // ==================== 位置搜尋範本 ====================

    @Override
    public Message nearbyToiletsResponse(List<ToiletLocation> toilets, double userLatitude, double userLongitude) {
        return locationBuilder.nearbyToiletsResponse(toilets, userLatitude, userLongitude);
    }

    @Override
    public Message findToiletsInstruction() {
        return locationBuilder.findToiletsInstruction();
    }
}
