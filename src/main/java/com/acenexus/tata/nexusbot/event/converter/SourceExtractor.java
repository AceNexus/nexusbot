package com.acenexus.tata.nexusbot.event.converter;

import com.acenexus.tata.nexusbot.event.RoomType;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * 從 LINE Webhook source 提取資訊
 */
public final class SourceExtractor {

    private SourceExtractor() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 提取聊天室 ID（一對一聊天使用 userId，群組聊天使用 groupId）
     *
     * @param source source 欄位
     * @return 聊天室 ID
     */
    public static String extractRoomId(JsonNode source) {
        String sourceType = source.path("type").asText();
        return "group".equals(sourceType) ? source.path("groupId").asText() : source.path("userId").asText();
    }

    /**
     * @param source source 欄位
     * @return 聊天室類型（USER 或 GROUP）
     */
    public static RoomType extractRoomType(JsonNode source) {
        String sourceType = source.path("type").asText();
        return "group".equals(sourceType) ? RoomType.GROUP : RoomType.USER;
    }

    /**
     * @param source source 欄位
     * @return 使用者 ID
     */
    public static String extractUserId(JsonNode source) {
        return source.path("userId").asText();
    }
}
