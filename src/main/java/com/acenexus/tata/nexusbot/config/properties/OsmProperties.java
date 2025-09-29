package com.acenexus.tata.nexusbot.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OpenStreetMap API 配置屬性
 */
@Data
@Component
@ConfigurationProperties(prefix = "osm")
public class OsmProperties {

    /**
     * Overpass API 設定
     */
    private final Overpass overpass = new Overpass();

    /**
     * Nominatim API 設定
     */
    private final Nominatim nominatim = new Nominatim();

    /**
     * 搜尋設定
     */
    private final Search search = new Search();

    @Data
    public static class Overpass {
        /**
         * Overpass API 基礎 URL
         */
        private String baseUrl = "https://overpass-api.de/api/interpreter";

        /**
         * 請求超時時間（毫秒）
         */
        private int timeoutMs = 15000;

        /**
         * 用戶代理標頭
         */
        private String userAgent = "NexusBot/1.0";
    }

    @Data
    public static class Nominatim {
        /**
         * Nominatim API 基礎 URL
         */
        private String baseUrl = "https://nominatim.openstreetmap.org";

        /**
         * 請求超時時間（毫秒）
         */
        private int timeoutMs = 10000;

        /**
         * 用戶代理標頭
         */
        private String userAgent = "NexusBot/1.0";

        /**
         * 電子郵件（禮貌性要求）
         */
        private String email = "nexusbot@example.com";
    }

    @Data
    public static class Search {
        /**
         * 預設搜尋半徑（公尺）
         */
        private int defaultRadius = 1000;

        /**
         * 最大搜尋半徑（公尺）
         */
        private int maxRadius = 5000;

        /**
         * 最大返回結果數量
         */
        private int maxResults = 10;

        /**
         * 廁所標籤列表（優先順序）
         */
        private String[] toiletTags = {
                "amenity=toilets",
                "amenity=public_bookcase", // 有些公共設施包含廁所
                "building=toilets"
        };

        /**
         * 包含廁所的設施標籤
         */
        private String[] facilityTags = {
                "amenity=restaurant",
                "amenity=cafe",
                "amenity=fast_food",
                "shop=convenience",
                "shop=supermarket",
                "amenity=fuel"
        };
    }
}