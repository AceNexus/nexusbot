package com.acenexus.tata.nexusbot.command;

import com.linecorp.bot.model.message.Message;
import lombok.Getter;

/**
 * 命令執行結果
 */
@Getter
public class CommandResult {
    private final boolean handled;
    private final Message message;
    private final String textResponse;

    private CommandResult(boolean handled, Message message, String textResponse) {
        this.handled = handled;
        this.message = message;
        this.textResponse = textResponse;
    }

    /**
     * 創建未處理的結果
     */
    public static CommandResult notHandled() {
        return new CommandResult(false, null, null);
    }

    /**
     * 創建已處理但無回應的結果
     */
    public static CommandResult handled() {
        return new CommandResult(true, null, null);
    }

    /**
     * 創建帶 Message 的結果
     */
    public static CommandResult withMessage(Message message) {
        return new CommandResult(true, message, null);
    }

    /**
     * 創建帶文字回應的結果
     */
    public static CommandResult withText(String text) {
        return new CommandResult(true, null, text);
    }
}
