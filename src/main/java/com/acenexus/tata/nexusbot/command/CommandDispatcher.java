package com.acenexus.tata.nexusbot.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 命令分發器
 */
@Slf4j
@Component
public class CommandDispatcher {

    private final List<CommandHandler> handlers;

    public CommandDispatcher(List<CommandHandler> handlers) {
        // 按優先級排序（數字越小優先級越高）
        this.handlers = handlers.stream().sorted(Comparator.comparingInt(CommandHandler::getPriority)).toList();
        log.info("Initialized CommandDispatcher with {} handlers", handlers.size());
        handlers.forEach(h -> log.debug("  - {} (priority: {})", h.getClass().getSimpleName(), h.getPriority()));
    }

    /**
     * 分發命令到合適的處理器
     *
     * @param context 命令上下文
     * @return 命令執行結果
     */
    public CommandResult dispatch(CommandContext context) {
        for (CommandHandler handler : handlers) {
            if (handler.canHandle(context)) {
                log.debug("Command handled by {}", handler.getClass().getSimpleName());
                return handler.handle(context);
            }
        }

        log.debug("No handler found for command: {}", context.getNormalizedText());
        return CommandResult.notHandled();
    }
}
