package com.acenexus.tata.nexusbot.command.handlers;

import com.acenexus.tata.nexusbot.command.CommandContext;
import com.acenexus.tata.nexusbot.command.CommandHandler;
import com.acenexus.tata.nexusbot.command.CommandResult;
import com.acenexus.tata.nexusbot.facade.ReminderFacade;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 提醒互動處理器
 * 處理提醒創建流程中的用戶輸入
 * 優先級: 3
 */
@Component
@RequiredArgsConstructor
public class ReminderInteractionHandler implements CommandHandler {

    private final ReminderFacade reminderFacade;

    @Override
    public boolean canHandle(CommandContext context) {
        return reminderFacade.isInReminderFlow(context.getRoomId());
    }

    @Override
    public CommandResult handle(CommandContext context) {
        Message message = reminderFacade.handleInteraction(context.getRoomId(), context.getMessageText(), context.getReplyToken());
        return message != null ? CommandResult.withMessage(message) : CommandResult.notHandled();
    }

    @Override
    public int getPriority() {
        return 3;
    }
}
