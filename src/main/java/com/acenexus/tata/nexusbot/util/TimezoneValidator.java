package com.acenexus.tata.nexusbot.util;

import lombok.extern.slf4j.Slf4j;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Map;

/**
 * 時區驗證與轉換工具
 * 功能：
 * 1. 驗證 IANA 時區 ID 是否有效
 * 2. 提供使用者友善名稱到 IANA ID 的對照
 * 3. 提供常用時區列表
 */
@Slf4j
public class TimezoneValidator {

    /**
     * 使用者友善名稱 → IANA 時區 ID 對照表
     * 用於自然語言解析與使用者輸入
     */
    private static final Map<String, String> TIMEZONE_ALIASES = Map.ofEntries(
            // 台灣
            Map.entry("台北", "Asia/Taipei"),
            Map.entry("台灣", "Asia/Taipei"),
            Map.entry("taipei", "Asia/Taipei"),
            Map.entry("taiwan", "Asia/Taipei"),

            // 日本
            Map.entry("東京", "Asia/Tokyo"),
            Map.entry("日本", "Asia/Tokyo"),
            Map.entry("tokyo", "Asia/Tokyo"),
            Map.entry("japan", "Asia/Tokyo"),

            // 香港
            Map.entry("香港", "Asia/Hong_Kong"),
            Map.entry("hong kong", "Asia/Hong_Kong"),
            Map.entry("hk", "Asia/Hong_Kong"),

            // 新加坡
            Map.entry("新加坡", "Asia/Singapore"),
            Map.entry("singapore", "Asia/Singapore"),

            // 韓國
            Map.entry("首爾", "Asia/Seoul"),
            Map.entry("韓國", "Asia/Seoul"),
            Map.entry("seoul", "Asia/Seoul"),
            Map.entry("korea", "Asia/Seoul"),

            // 中國
            Map.entry("上海", "Asia/Shanghai"),
            Map.entry("中國", "Asia/Shanghai"),
            Map.entry("shanghai", "Asia/Shanghai"),
            Map.entry("china", "Asia/Shanghai"),

            // 美國東岸
            Map.entry("紐約", "America/New_York"),
            Map.entry("美國東岸", "America/New_York"),
            Map.entry("美東", "America/New_York"),
            Map.entry("new york", "America/New_York"),
            Map.entry("ny", "America/New_York"),

            // 美國西岸
            Map.entry("洛杉磯", "America/Los_Angeles"),
            Map.entry("美國西岸", "America/Los_Angeles"),
            Map.entry("美西", "America/Los_Angeles"),
            Map.entry("los angeles", "America/Los_Angeles"),
            Map.entry("la", "America/Los_Angeles"),

            // 英國
            Map.entry("倫敦", "Europe/London"),
            Map.entry("英國", "Europe/London"),
            Map.entry("london", "Europe/London"),
            Map.entry("uk", "Europe/London")
    );

    /**
     * 常用時區列表（按地理位置分組）
     */
    public static final Map<String, String> COMMON_TIMEZONES = Map.ofEntries(
            Map.entry("Asia/Taipei", "台北 (GMT+8)"),
            Map.entry("Asia/Tokyo", "東京 (GMT+9)"),
            Map.entry("Asia/Hong_Kong", "香港 (GMT+8)"),
            Map.entry("Asia/Singapore", "新加坡 (GMT+8)"),
            Map.entry("Asia/Seoul", "首爾 (GMT+9)"),
            Map.entry("Asia/Shanghai", "上海 (GMT+8)"),
            Map.entry("America/New_York", "紐約 (GMT-5/-4)"),
            Map.entry("America/Los_Angeles", "洛杉磯 (GMT-8/-7)"),
            Map.entry("Europe/London", "倫敦 (GMT+0/+1)")
    );

    /**
     * 驗證時區 ID 是否有效
     *
     * @param timezoneId IANA 時區 ID（例如：Asia/Taipei）
     * @return true 如果時區有效
     */
    public static boolean isValidTimezone(String timezoneId) {
        if (timezoneId == null || timezoneId.isBlank()) {
            return false;
        }

        try {
            ZoneId.of(timezoneId);
            return true;
        } catch (DateTimeException e) {
            log.warn("Invalid timezone ID: {}", timezoneId);
            return false;
        }
    }

    /**
     * 將使用者輸入轉換為標準 IANA 時區 ID
     * <p>
     * 支援：
     * 1. 標準 IANA ID（例如：Asia/Taipei）
     * 2. 使用者友善名稱（例如：台北、紐約）
     *
     * @param input 使用者輸入
     * @return IANA 時區 ID，如果無法識別則返回 null
     */
    public static String resolveTimezone(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String normalized = input.trim().toLowerCase();

        // 1. 檢查是否為標準 IANA ID
        if (isValidTimezone(input.trim())) {
            return input.trim();
        }

        // 2. 檢查別名對照表
        String resolved = TIMEZONE_ALIASES.get(normalized);
        if (resolved != null) {
            log.debug("Resolved timezone alias '{}' to '{}'", input, resolved);
            return resolved;
        }

        // 3. 無法識別
        log.warn("Unable to resolve timezone: {}", input);
        return null;
    }

    /**
     * 取得時區的顯示名稱（中文或英文）
     *
     * @param timezoneId IANA 時區 ID
     * @return 顯示名稱，例如「台北 (GMT+8)」
     */
    public static String getDisplayName(String timezoneId) {
        if (timezoneId == null) {
            return "未知時區";
        }

        String displayName = COMMON_TIMEZONES.get(timezoneId);
        return displayName != null ? displayName : timezoneId;
    }
}
