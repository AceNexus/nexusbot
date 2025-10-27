package com.acenexus.tata.nexusbot.command.handlers;

import com.acenexus.tata.nexusbot.command.CommandContext;
import com.acenexus.tata.nexusbot.command.CommandHandler;
import com.acenexus.tata.nexusbot.command.CommandResult;
import com.acenexus.tata.nexusbot.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 認證命令處理器
 * 處理 /auth 命令和密碼輸入
 * 優先級: 1 (最高)
 */
@Component
@RequiredArgsConstructor
public class AuthCommandHandler implements CommandHandler {

    private final AdminService adminService;

    @Override
    public boolean canHandle(CommandContext context) {
        // 處理 /auth 命令或正在等待密碼輸入的聊天室
        return context.getNormalizedText().equals("/auth") || adminService.isAuthPending(context.getRoomId(), context.getRoomType());
    }

    @Override
    public CommandResult handle(CommandContext context) {
        String response = adminService.processAuthCommand(context.getRoomId(), context.getRoomType(), context.getMessageText());
        return response != null ? CommandResult.withText(response) : CommandResult.notHandled();
    }

    @Override
    public int getPriority() {
        return 1; // 最高優先級
    }
}
