package com.acenexus.tata.nexusbot.command;

/**
 * 命令處理器統一接口
 * 所有命令處理器都需要實現此接口
 */
public interface CommandHandler {

    /**
     * 判斷是否可以處理該命令
     *
     * @param context 命令上下文
     * @return true 如果可以處理
     */
    boolean canHandle(CommandContext context);

    /**
     * 處理命令
     *
     * @param context 命令上下文
     * @return 命令執行結果
     */
    CommandResult handle(CommandContext context);

    /**
     * 獲取處理器優先級（數字越小優先級越高）
     *
     * @return 優先級
     */
    int getPriority();
}
