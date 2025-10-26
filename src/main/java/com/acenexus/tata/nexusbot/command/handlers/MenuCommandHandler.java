package com.acenexus.tata.nexusbot.command.handlers;

import com.acenexus.tata.nexusbot.command.CommandContext;
import com.acenexus.tata.nexusbot.command.CommandHandler;
import com.acenexus.tata.nexusbot.command.CommandResult;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 選單命令處理器
 * 處理 "menu" 或 "選單" 命令
 * 優先級: 5 (最低)
 */
@Component
@RequiredArgsConstructor
public class MenuCommandHandler implements CommandHandler {

    private final MessageTemplateProvider messageTemplateProvider;

    private static final Set<String> MENU_COMMANDS = Set.of("menu", "選單");

    @Override
    public boolean canHandle(CommandContext context) {
        return MENU_COMMANDS.contains(context.getNormalizedText());
    }

    @Override
    public CommandResult handle(CommandContext context) {
        return CommandResult.withMessage(messageTemplateProvider.mainMenu());
    }

    @Override
    public int getPriority() {
        return 5; // 最低優先級
    }
}
