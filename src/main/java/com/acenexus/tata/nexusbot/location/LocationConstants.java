package com.acenexus.tata.nexusbot.location;

/**
 * 位置服務相關常數
 */
public final class LocationConstants {

    private LocationConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 地理計算常數
     */
    public static final class Geography {
        private Geography() {
        }

        /**
         * 地球半徑（公尺）
         * 用於 Haversine 距離計算
         */
        public static final int EARTH_RADIUS_METERS = 6371000;

        /**
         * 緯度 1 度對應的距離（公尺）
         * 全球固定值
         */
        public static final double METERS_PER_DEGREE_LATITUDE = 111320.0;
    }

    /**
     * Overpass API 查詢常數
     */
    public static final class OverpassQuery {
        private OverpassQuery() {
        }

        /**
         * 查詢超時時間（秒）
         */
        public static final int TIMEOUT_SECONDS = 25;

        /**
         * 輸出格式
         */
        public static final String OUTPUT_FORMAT = "[out:json]";

        /**
         * 輸出幾何資料和元資料
         */
        public static final String OUTPUT_DETAILS = "out geom meta;";
    }

    /**
     * 廁所評分常數
     */
    public static final class Rating {
        private Rating() {
        }

        /**
         * 基礎評分
         */
        public static final double BASE_RATING = 3.0;

        /**
         * 最高評分
         */
        public static final double MAX_RATING = 5.0;

        /**
         * 有名稱的加分
         */
        public static final double BONUS_HAS_NAME = 0.5;

        /**
         * 無障礙設施加分
         */
        public static final double BONUS_WHEELCHAIR_ACCESSIBLE = 0.3;

        /**
         * 免費使用加分
         */
        public static final double BONUS_FREE = 0.2;

        /**
         * 有營業時間資訊加分
         */
        public static final double BONUS_HAS_OPENING_HOURS = 0.2;

        /**
         * 專門廁所設施加分
         */
        public static final double BONUS_DEDICATED_TOILET = 0.3;
    }

    /**
     * OSM 標籤常數
     */
    public static final class OsmTags {
        private OsmTags() {
        }

        // 標籤鍵
        public static final String AMENITY = "amenity";
        public static final String BUILDING = "building";
        public static final String SHOP = "shop";
        public static final String WHEELCHAIR = "wheelchair";
        public static final String TOILETS_WHEELCHAIR = "toilets:wheelchair";
        public static final String OPENING_HOURS = "opening_hours";
        public static final String NAME = "name";

        // 標籤值
        public static final String TOILETS = "toilets";
        public static final String RESTAURANT = "restaurant";
        public static final String CAFE = "cafe";
        public static final String CONVENIENCE = "convenience";
        public static final String FUEL = "fuel";
        public static final String YES = "yes";

        // 營業時間標準值
        public static final String OPEN_24_7 = "24/7";
        public static final String OPEN_24_HOURS = "24 hours";
        public static final String OPEN_24_HOURS_NO_SPACE = "24hours";
    }

    /**
     * 地址標籤
     */
    public static final class AddressTags {
        private AddressTags() {
        }

        public static final String FULL = "addr:full";
        public static final String HOUSE_NUMBER = "addr:housenumber";
        public static final String STREET = "addr:street";
        public static final String DISTRICT = "addr:district";
        public static final String CITY = "addr:city";
        public static final String STATE = "addr:state";
    }

    /**
     * 預設名稱
     */
    public static final class DefaultNames {
        private DefaultNames() {
        }

        public static final String PUBLIC_TOILET = "公共廁所";
        public static final String TOILET_BUILDING = "廁所建築";
        public static final String TOILET_FACILITY = "廁所設施";

        // 帶類型的名稱後綴
        public static final String RESTAURANT_SUFFIX = " (餐廳)";
        public static final String CAFE_SUFFIX = " (咖啡廳)";
        public static final String CONVENIENCE_STORE_SUFFIX = " (便利商店)";
        public static final String GAS_STATION_SUFFIX = " (加油站)";

        // 無名稱時的預設值
        public static final String RESTAURANT_DEFAULT = "餐廳廁所";
        public static final String CAFE_DEFAULT = "咖啡廳廁所";
        public static final String CONVENIENCE_STORE_DEFAULT = "便利商店廁所";
        public static final String GAS_STATION_DEFAULT = "加油站廁所";
    }
}
