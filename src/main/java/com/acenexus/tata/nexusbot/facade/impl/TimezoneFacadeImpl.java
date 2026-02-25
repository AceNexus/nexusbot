package com.acenexus.tata.nexusbot.facade.impl;

import com.acenexus.tata.nexusbot.chatroom.ChatRoomAccessor;
import com.acenexus.tata.nexusbot.chatroom.ChatRoomManager;
import com.acenexus.tata.nexusbot.email.EmailInputStateService;
import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.entity.TimezoneInputState;
import com.acenexus.tata.nexusbot.facade.TimezoneFacade;
import com.acenexus.tata.nexusbot.reminder.ReminderStateManager;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.acenexus.tata.nexusbot.timezone.TimezoneInputStateService;
import com.acenexus.tata.nexusbot.util.TimezoneValidator;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 時區設定功能 Facade 實作
 */
@Service
@RequiredArgsConstructor
public class TimezoneFacadeImpl implements TimezoneFacade {

    private static final Logger logger = LoggerFactory.getLogger(TimezoneFacadeImpl.class);

    private final ChatRoomAccessor chatRoomAccessor;
    private final ChatRoomManager chatRoomManager;
    private final TimezoneInputStateService timezoneInputStateService;
    private final MessageTemplateProvider messageTemplateProvider;
    private final ReminderStateManager reminderStateManager;
    private final EmailInputStateService emailInputStateService;

    @Override
    public Message showSettings(String roomId) {
        // 獲取聊天室（使用 USER 類型，因為只需要讀取時區）
        ChatRoom chatRoom = chatRoomAccessor.getOrCreateChatRoom(roomId, ChatRoom.RoomType.USER);
        String currentTimezone = chatRoom.getTimezone();
        String timezoneDisplay = TimezoneValidator.getDisplayName(currentTimezone);

        logger.debug("Showing timezone settings for room: {}, current timezone: {}", roomId, currentTimezone);
        return messageTemplateProvider.timezoneSettingsMenu(currentTimezone, timezoneDisplay);
    }

    @Override
    public Message keepTimezone(String roomId) {
        ChatRoom chatRoom = chatRoomAccessor.getOrCreateChatRoom(roomId, ChatRoom.RoomType.USER);
        String currentTimezone = chatRoom.getTimezone();
        String timezoneDisplay = TimezoneValidator.getDisplayName(currentTimezone);

        logger.info("Room {} chose to keep current timezone: {}", roomId, currentTimezone);
        return messageTemplateProvider.success(String.format("已維持目前時區：%s", timezoneDisplay));
    }

    @Override
    public Message startChangingTimezone(String roomId) {
        // 互斥保護：清除其他流程狀態
        reminderStateManager.clearState(roomId);
        emailInputStateService.clearWaitingForEmailInput(roomId);

        ChatRoom chatRoom = chatRoomAccessor.getOrCreateChatRoom(roomId, ChatRoom.RoomType.USER);
        String currentTimezone = chatRoom.getTimezone();

        timezoneInputStateService.setWaitingForTimezoneInput(roomId);
        logger.info("Room {} started changing timezone from: {}", roomId, currentTimezone);
        return messageTemplateProvider.timezoneChangePrompt(currentTimezone);
    }

    @Override
    public Message cancelTimezoneChange(String roomId) {
        timezoneInputStateService.clearWaitingForTimezoneInput(roomId);

        ChatRoom chatRoom = chatRoomAccessor.getOrCreateChatRoom(roomId, ChatRoom.RoomType.USER);
        String currentTimezone = chatRoom.getTimezone();
        String timezoneDisplay = TimezoneValidator.getDisplayName(currentTimezone);

        logger.info("Room {} cancelled timezone change", roomId);
        return messageTemplateProvider.timezoneCancelMessage(currentTimezone, timezoneDisplay);
    }

    @Override
    public Message handleTimezoneInput(String roomId, String input) {
        try {
            input = input.trim();

            // 解析時區
            String resolvedTimezone = TimezoneValidator.resolveTimezone(input);

            if (resolvedTimezone == null) {
                logger.warn("Failed to resolve timezone from input: {} for room: {}", input, roomId);
                return messageTemplateProvider.timezoneParseError(input);
            }

            // 儲存解析後的時區到狀態
            timezoneInputStateService.saveResolvedTimezone(roomId, resolvedTimezone, input);

            String timezoneDisplay = TimezoneValidator.getDisplayName(resolvedTimezone);
            logger.info("Resolved timezone for room {}: {} (from input: {})", roomId, resolvedTimezone, input);

            return messageTemplateProvider.timezoneConfirmation(resolvedTimezone, timezoneDisplay, input);
        } catch (Exception e) {
            logger.error("Error processing timezone input for room {}: {}", roomId, e.getMessage(), e);
            timezoneInputStateService.clearWaitingForTimezoneInput(roomId);
            return messageTemplateProvider.error("處理時區輸入時發生錯誤。");
        }
    }

    @Override
    public Message confirmTimezoneChange(String roomId) {
        try {
            // 獲取儲存的時區輸入狀態
            Optional<TimezoneInputState> stateOpt = timezoneInputStateService.getTimezoneInputState(roomId);

            if (stateOpt.isEmpty() || stateOpt.get().getResolvedTimezone() == null) {
                logger.warn("No timezone input state found for room: {}", roomId);
                return messageTemplateProvider.error("找不到待確認的時區設定，請重新操作。");
            }

            TimezoneInputState state = stateOpt.get();
            String newTimezone = state.getResolvedTimezone();

            // 獲取聊天室並更新時區
            ChatRoom chatRoom = chatRoomAccessor.getOrCreateChatRoom(roomId, ChatRoom.RoomType.USER);
            boolean success = chatRoomManager.setTimezone(roomId, chatRoom.getRoomType(), newTimezone);

            // 清除輸入狀態
            timezoneInputStateService.clearWaitingForTimezoneInput(roomId);

            if (success) {
                String timezoneDisplay = TimezoneValidator.getDisplayName(newTimezone);
                logger.info("Successfully updated timezone for room {} to: {}", roomId, newTimezone);
                return messageTemplateProvider.timezoneUpdateSuccess(newTimezone, timezoneDisplay);
            } else {
                logger.error("Failed to update timezone for room {}", roomId);
                return messageTemplateProvider.error("更新時區失敗，請稍後再試。");
            }
        } catch (Exception e) {
            logger.error("Error confirming timezone change for room {}: {}", roomId, e.getMessage(), e);
            timezoneInputStateService.clearWaitingForTimezoneInput(roomId);
            return messageTemplateProvider.error("確認時區變更時發生錯誤。");
        }
    }

    @Override
    public boolean isWaitingForTimezoneInput(String roomId) {
        return timezoneInputStateService.isWaitingForTimezoneInput(roomId);
    }
}
