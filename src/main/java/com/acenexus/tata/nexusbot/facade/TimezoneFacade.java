package com.acenexus.tata.nexusbot.facade;

import com.linecorp.bot.model.message.Message;

/**
 * 時區設定功能 Facade - 協調時區設定流程
 */
public interface TimezoneFacade {

    /**
     * 顯示時區設定選單
     */
    Message showSettings(String roomId);

    /**
     * 保持目前時區不變
     */
    Message keepTimezone(String roomId);

    /**
     * 開始變更時區流程（提示使用者輸入）
     */
    Message startChangingTimezone(String roomId);

    /**
     * 取消時區變更
     */
    Message cancelTimezoneChange(String roomId);

    /**
     * 處理使用者輸入的時區文字
     */
    Message handleTimezoneInput(String roomId, String input);

    /**
     * 確認時區變更
     */
    Message confirmTimezoneChange(String roomId);

    /**
     * 檢查聊天室是否正在等待時區輸入
     */
    boolean isWaitingForTimezoneInput(String roomId);

}
