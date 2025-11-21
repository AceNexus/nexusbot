package com.acenexus.tata.nexusbot.email;

import com.acenexus.tata.nexusbot.entity.Email;

import java.util.List;

/**
 * Email 管理服務介面
 * 管理聊天室的多個電子郵件地址
 */
public interface EmailManager {

    /**
     * 新增 Email
     *
     * @param roomId       聊天室 ID
     * @param emailAddress Email 地址
     * @return 新增的 Email 實體
     */
    Email addEmail(String roomId, String emailAddress);

    /**
     * 獲取聊天室所有啟用的 Email
     *
     * @param roomId 聊天室 ID
     * @return Email 列表
     */
    List<Email> getActiveEmails(String roomId);

    /**
     * 獲取聊天室所有已啟用通知的 Email 地址
     *
     * @param roomId 聊天室 ID
     * @return Email 地址列表
     */
    List<String> getEnabledEmailAddresses(String roomId);

    /**
     * 啟用 Email 通知
     *
     * @param emailId Email ID
     * @param roomId  聊天室 ID
     * @return 是否成功
     */
    boolean enableEmail(Long emailId, String roomId);

    /**
     * 停用 Email 通知
     *
     * @param emailId Email ID
     * @param roomId  聊天室 ID
     * @return 是否成功
     */
    boolean disableEmail(Long emailId, String roomId);

    /**
     * 刪除 Email（軟刪除）
     *
     * @param emailId Email ID
     * @param roomId  聊天室 ID
     * @return 是否成功
     */
    boolean deleteEmail(Long emailId, String roomId);
}
