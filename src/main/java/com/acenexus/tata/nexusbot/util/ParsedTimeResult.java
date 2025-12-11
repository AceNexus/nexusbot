package com.acenexus.tata.nexusbot.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 時間解析結果
 * <p>
 * 封裝 AI 解析的日期時間與時區資訊
 */
@Getter
@AllArgsConstructor
public class ParsedTimeResult {

    /**
     * 解析後的本地時間
     */
    private final LocalDateTime dateTime;

    /**
     * 解析後的時區（可能為 null）
     * - 如果使用者明確指定時區（例如「紐約時間下午3點」），此欄位有值
     * - 如果未指定時區（例如「明天下午3點」），此欄位為 null，應使用 ChatRoom 預設時區
     */
    private final String timezone;

    /**
     * 檢查是否包含時區資訊
     *
     * @return true 如果使用者明確指定了時區
     */
    public boolean hasTimezone() {
        return timezone != null && !timezone.isEmpty();
    }
}
