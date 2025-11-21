package com.acenexus.tata.nexusbot.location.query;

import com.acenexus.tata.nexusbot.config.properties.OsmProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.acenexus.tata.nexusbot.location.LocationConstants.Geography;
import static com.acenexus.tata.nexusbot.location.LocationConstants.OverpassQuery;

/**
 * Overpass API 查詢建構器
 * 負責建構 Overpass QL 查詢語句，支援：
 * - 座標範圍計算
 * - 多標籤查詢
 * - Node 和 Way 查詢
 */
@Component
@RequiredArgsConstructor
public class OverpassQueryBuilder {

    private final OsmProperties osmProperties;

    /**
     * 建構 Overpass API 查詢語句
     *
     * @param latitude  中心緯度
     * @param longitude 中心經度
     * @param radius    搜尋半徑（公尺）
     * @return Overpass QL 查詢字串
     */
    public String buildQuery(double latitude, double longitude, int radius) {
        BoundingBox bbox = calculateBoundingBox(latitude, longitude, radius);

        // 建構查詢標頭
        String header = """
                %s[timeout:%d];
                (
                """.formatted(OverpassQuery.OUTPUT_FORMAT, OverpassQuery.TIMEOUT_SECONDS);

        // 建構查詢主體
        StringBuilder queries = new StringBuilder();

        // 搜尋直接的廁所設施
        for (String tag : osmProperties.getToiletTags()) {
            queries.append(buildNodeAndWayQuery(tag, null, bbox));
        }

        // 搜尋可能包含廁所的設施（需額外檢查 toilets=yes）
        for (String tag : osmProperties.getFacilityTags()) {
            queries.append(buildNodeAndWayQuery(tag, "[\"toilets\"=\"yes\"]", bbox));
        }

        // 建構查詢結尾
        String footer = """
                );
                %s""".formatted(OverpassQuery.OUTPUT_DETAILS);

        return header + queries + footer;
    }

    /**
     * 計算搜尋範圍的邊界框
     */
    private BoundingBox calculateBoundingBox(double latitude, double longitude, int radius) {
        // 緯度：1度 ≈ 111,320 公尺（全球固定）
        double latRadiusInDegrees = radius / Geography.METERS_PER_DEGREE_LATITUDE;

        // 經度：1度距離隨緯度變化，使用 cos(lat)
        double lonRadiusInDegrees = radius / (Geography.METERS_PER_DEGREE_LATITUDE * Math.cos(Math.toRadians(latitude)));

        return new BoundingBox(
                latitude - latRadiusInDegrees,  // minLat
                latitude + latRadiusInDegrees,  // maxLat
                longitude - lonRadiusInDegrees, // minLon
                longitude + lonRadiusInDegrees  // maxLon
        );
    }

    /**
     * 建構 Node 和 Way 查詢字串
     *
     * @param tag              主要標籤（例如：amenity=restaurant）
     * @param additionalFilter 額外過濾條件（例如：["toilets"="yes"]），可為 null
     * @param bbox             邊界框
     * @return 查詢字串
     */
    private String buildNodeAndWayQuery(String tag, String additionalFilter, BoundingBox bbox) {
        String filter = additionalFilter != null ? additionalFilter : "";
        return """
                  node[%s]%s%s;
                  way[%s]%s%s;
                """.formatted(tag, filter, bbox, tag, filter, bbox);
    }

    /**
     * 邊界框（Bounding Box）
     */
    private record BoundingBox(double minLat, double maxLat, double minLon, double maxLon) {
        @Override
        public String toString() {
            return String.format("(%.6f,%.6f,%.6f,%.6f)", minLat, minLon, maxLat, maxLon);
        }
    }
}
