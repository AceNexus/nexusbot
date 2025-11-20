package com.acenexus.tata.nexusbot.event;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.util.Collections;
import java.util.Map;

/**
 * 通用的 LINE Bot 事件模型
 * 用於封裝事件的共同屬性與各事件的特定資料
 *
 * @see EventType 事件類型定義
 * @see RoomType 聊天室類型定義
 */
@Getter
@Builder
@ToString
public class LineBotEvent {
    /**
     * 聊天室 ID
     * 一對一聊天：等於 userId
     * 群組聊天：等於 groupId
     */
    @NonNull
    private final String roomId;

    /**
     * 聊天室類型
     * @see RoomType
     */
    @NonNull
    private final RoomType roomType;

    /**
     * 使用者 ID（事件觸發者）
     */
    @NonNull
    private final String userId;

    /**
     * 事件類型
     * @see EventType
     */
    @NonNull
    private final EventType eventType;

    /**
     * LINE 回覆 Token
     * 用於發送回覆訊息，每個 token 只能使用一次
     */
    @NonNull
    private final String replyToken;

    /**
     * 事件特定資料
     * 根據不同的 eventType，payload 包含不同的 key-value pairs
     * Payload 結構範例：
     * TEXT_MESSAGE: {"text": "訊息內容"}
     * IMAGE_MESSAGE: {"messageId": "12345"}
     * STICKER_MESSAGE: {"packageId": "1", "stickerId": "1"}
     * POSTBACK: {"action": "reminder_create", "params": {...}}
     * LOCATION_MESSAGE: {"title": "地點", "address": "地址", "latitude": 25.0, "longitude": 121.5}
     *
     * @see EventType 各事件類型的 payload 定義
     */
    @NonNull
    @Builder.Default
    private final Map<String, Object> payload = Collections.emptyMap();

    /**
     * 原始 LINE 事件 JSON
     * 保留原始資料供特殊需求查詢使用，可為 null
     */
    private final JsonNode rawEvent;

    /**
     * 從 payload 取得字串值
     *
     * @param key payload 的 key
     * @return 對應的字串值，若不存在或類型不符則返回 null
     */
    public String getPayloadString(String key) {
        Object value = payload.get(key);
        return value instanceof String ? (String) value : null;
    }

    /**
     * 從 payload 取得長整數值
     *
     * @param key payload 的 key
     * @return 對應的長整數值,若不存在或類型不符則返回 null
     */
    public Long getPayloadLong(String key) {
        Object value = payload.get(key);
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    /**
     * 從 payload 取得雙精度浮點數值
     *
     * @param key payload 的 key
     * @return 對應的 double 值，若不存在或類型不符則返回 null
     */
    public Double getPayloadDouble(String key) {
        Object value = payload.get(key);
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    /**
     * 從 payload 中解析 query string 格式的參數
     * 用於解析如 "action=DELETE_EMAIL&id=123" 格式的資料，提取特定參數值。
     *
     * @param key   payload 的 key（通常是 "action"）
     * @param param 要提取的參數名稱（如 "id"）
     * @return 參數值，若不存在或解析失敗則返回 null
     */
    public String getPayloadParameter(String key, String param) {
        String data = getPayloadString(key);
        if (data == null) {
            return null;
        }

        String searchKey = param + "=";
        int startIndex = data.indexOf(searchKey);
        if (startIndex == -1) {
            return null;
        }

        startIndex += searchKey.length();
        int endIndex = data.indexOf('&', startIndex);

        return endIndex == -1 ? data.substring(startIndex) : data.substring(startIndex, endIndex);
    }

    /**
     * 從 payload 中解析 query string 格式的 Long 參數
     * 用於解析如 "action=DELETE_EMAIL&id=123" 格式的資料，提取並轉換為 Long 型別。
     * 內建錯誤處理，解析失敗時返回 null。
     *
     * @param key   payload 的 key（通常是 "action"）
     * @param param 要提取的參數名稱（如 "id"）
     * @return Long 值，若不存在或解析失敗則返回 null
     */
    public Long getPayloadParameterAsLong(String key, String param) {
        String value = getPayloadParameter(key, param);
        if (value == null) {
            return null;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 取得標準化的文字訊息（小寫 + trim）
     * 適用於命令比對等場景，統一文字格式。
     *
     * @return 標準化的文字（toLowerCase + trim），若不是文字訊息則返回 null
     */
    public String getNormalizedText() {
        String text = getPayloadString("text");
        return text != null ? text.toLowerCase().trim() : null;
    }

    /**
     * 檢查 postback action 是否為指定值之一
     * 簡化 Postback handler 的 action 檢查邏輯，自動處理 null 檢查。
     *
     * @param actions 要檢查的 action 值（一個或多個）
     * @return {@code true} 表示 action 匹配任一指定值，{@code false} 表示不匹配或 action 為 null
     */
    public boolean hasAction(String... actions) {
        String action = getPayloadString("action");
        if (action == null) {
            return false;
        }

        for (String targetAction : actions) {
            if (targetAction.equals(action)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 檢查 postback action 是否以指定前綴開頭
     * 用於動態 action 的比對，如 "DELETE_EMAIL&id=123" 以 "DELETE_EMAIL" 開頭。
     *
     * @param prefix action 前綴
     * @return {@code true} 表示 action 以指定前綴開頭，{@code false} 表示不匹配或 action 為 null
     */
    public boolean actionStartsWith(String prefix) {
        String action = getPayloadString("action");
        return action != null && action.startsWith(prefix);
    }
}
