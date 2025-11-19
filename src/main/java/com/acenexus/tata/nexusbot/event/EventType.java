package com.acenexus.tata.nexusbot.event;

/**
 * LINE Bot 事件類型枚舉
 * 定義所有可處理的事件類型
 */
public enum EventType {
    // ========== 訊息類型 ==========
    /**
     * 文字訊息
     * payload keys: text (String)
     */
    TEXT_MESSAGE,

    /**
     * 圖片訊息
     * payload keys: messageId (String)
     */
    IMAGE_MESSAGE,

    /**
     * 貼圖訊息
     * payload keys: packageId (String), stickerId (String)
     */
    STICKER_MESSAGE,

    /**
     * 影片訊息
     * payload keys: messageId (String)
     */
    VIDEO_MESSAGE,

    /**
     * 音訊訊息
     * payload keys: messageId (String)
     */
    AUDIO_MESSAGE,

    /**
     * 檔案訊息
     * payload keys: messageId (String), fileName (String), fileSize (Long)
     */
    FILE_MESSAGE,

    /**
     * 位置訊息
     * payload keys: title (String), address (String), latitude (Double), longitude (Double)
     */
    LOCATION_MESSAGE,

    // ========== 互動類型 ==========
    /**
     * Postback 事件（按鈕回傳）
     * payload keys: action (String), params (Map<String, String>, optional)
     */
    POSTBACK,

    // ========== 關係類型 ==========
    /**
     * 使用者加入好友
     * payload: 無額外資料
     */
    FOLLOW,

    /**
     * 使用者封鎖機器人
     * payload: 無額外資料
     */
    UNFOLLOW,

    // ========== 群組類型 ==========
    /**
     * 機器人加入群組
     * payload: 無額外資料
     */
    JOIN,

    /**
     * 機器人離開群組
     * payload: 無額外資料
     */
    LEAVE,

    /**
     * 成員加入群組
     * payload keys: joinedUsers (List<String>)
     */
    MEMBER_JOINED,

    /**
     * 成員離開群組
     * payload keys: leftUsers (List<String>)
     */
    MEMBER_LEFT,

    // ========== 未知類型 ==========
    /**
     * 未知事件類型（預設值）
     */
    UNKNOWN
}
