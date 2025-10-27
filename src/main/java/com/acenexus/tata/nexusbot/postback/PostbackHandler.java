package com.acenexus.tata.nexusbot.postback;

import com.acenexus.tata.nexusbot.postback.handlers.PostbackEventDispatcher;
import com.fasterxml.jackson.databind.JsonNode;
import com.linecorp.bot.model.message.Message;

/**
 * Postback 事件處理器介面
 * 定義所有 Postback Handler 的統一介面，遵循 Strategy Pattern。
 * 每個實作類別負責處理特定領域的 Postback 動作。
 *
 * @see PostbackEventDispatcher
 */
public interface PostbackHandler {

    /**
     * 判斷此 Handler 是否能處理指定的 Postback 動作
     *
     * @param action Postback 動作字串 (e.g., "action=main_menu", "repeat=once")
     * @return 如果此 Handler 可以處理該動作則回傳 true，否則回傳 false
     */
    boolean canHandle(String action);

    /**
     * 處理 Postback 事件並回傳回應訊息
     *
     * @param action     Postback 動作字串
     * @param roomId     聊天室 ID (userId 或 groupId)
     * @param roomType   聊天室類型 (USER 或 GROUP)
     * @param replyToken LINE 回覆 Token
     * @param event      完整的 LINE Event JSON 物件
     * @return 要回傳給使用者的訊息，若無需回應則回傳 null
     */
    Message handle(String action, String roomId, String roomType, String replyToken, JsonNode event);

    /**
     * 取得 Handler 的優先順序
     * 數字越小優先順序越高。用於決定多個 Handler 都能處理同一動作時的執行順序。
     *
     * @return 優先順序 (預設為 100)
     */
    default int getPriority() {
        return 100;
    }
}
