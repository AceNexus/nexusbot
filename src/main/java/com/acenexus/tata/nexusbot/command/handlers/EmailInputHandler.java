package com.acenexus.tata.nexusbot.command.handlers;

import com.acenexus.tata.nexusbot.command.CommandContext;
import com.acenexus.tata.nexusbot.command.CommandHandler;
import com.acenexus.tata.nexusbot.command.CommandResult;
import com.acenexus.tata.nexusbot.facade.EmailFacade;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Email 輸入處理器
 * 處理 Email 地址輸入流程
 * 優先級: 4
 */
@Component
@RequiredArgsConstructor
public class EmailInputHandler implements CommandHandler {

    private final EmailFacade emailFacade;

    @Override
    public boolean canHandle(CommandContext context) {
        return emailFacade.isWaitingForEmailInput(context.getRoomId());
    }

    @Override
    public CommandResult handle(CommandContext context) {
        Message message = emailFacade.handleEmailInput(context.getRoomId(), context.getMessageText());
        return CommandResult.withMessage(message);
    }

    @Override
    public int getPriority() {
        return 4;
    }
}
