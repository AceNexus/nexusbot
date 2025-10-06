package com.acenexus.tata.nexusbot.facade;

import com.linecorp.bot.model.message.Message;

/**
 * Email 功能 Facade - 協調 Email 管理流程
 */
public interface EmailFacade {

    Message showMenu(String roomId);

    Message startAddingEmail(String roomId);

    Message cancelAddingEmail(String roomId);

    Message deleteEmail(Long emailId, String roomId);

    Message toggleEmailStatus(Long emailId, String roomId);

    Message handleEmailInput(String roomId, String email);

    boolean isWaitingForEmailInput(String roomId);

    void clearEmailInputState(String roomId);
}
