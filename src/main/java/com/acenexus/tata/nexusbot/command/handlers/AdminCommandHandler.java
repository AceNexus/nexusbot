package com.acenexus.tata.nexusbot.command.handlers;

import com.acenexus.tata.nexusbot.command.CommandContext;
import com.acenexus.tata.nexusbot.command.CommandHandler;
import com.acenexus.tata.nexusbot.command.CommandResult;
import com.acenexus.tata.nexusbot.service.AdminService;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 管理員命令處理器
 * 處理管理員專屬命令（如 /stats）
 * 優先級: 2
 */
@Component
@RequiredArgsConstructor
public class AdminCommandHandler implements CommandHandler {

    private final AdminService adminService;

    @Override
    public boolean canHandle(CommandContext context) {
        // 只有管理員聊天室才能執行管理員命令
        return adminService.isAdminRoom(context.getRoomId(), context.getRoomType());
    }

    @Override
    public CommandResult handle(CommandContext context) {
        Message message = adminService.processAdminCommand(context.getRoomId(), context.getRoomType(), context.getMessageText());
        return message != null ? CommandResult.withMessage(message) : CommandResult.notHandled();
    }

    @Override
    public int getPriority() {
        return 2;
    }
}
