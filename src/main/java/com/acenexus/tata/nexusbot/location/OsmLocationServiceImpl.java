package com.acenexus.tata.nexusbot.location;

import com.acenexus.tata.nexusbot.config.properties.OsmProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OsmLocationServiceImpl implements LocationService {

    private final OsmProperties osmProperties;
    private final RestTemplateBuilder restTemplateBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private RestTemplate overpassRestTemplate;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory overpassFactory = new SimpleClientHttpRequestFactory();
        overpassFactory.setConnectTimeout(osmProperties.getTimeoutMs());
        overpassFactory.setReadTimeout(osmProperties.getTimeoutMs());

        this.overpassRestTemplate = restTemplateBuilder
                .requestFactory(() -> overpassFactory)
                .defaultHeader("User-Agent", osmProperties.getUserAgent())
                .build();
    }

    @Override
    public CompletableFuture<List<ToiletLocation>> findNearbyToilets(double latitude, double longitude, int radius) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Searching for toilets near ({}, {}) within {}m using OSM", latitude, longitude, radius);

                // 建立 Overpass 查詢
                String overpassQuery = buildOverpassQuery(latitude, longitude, radius);
                log.debug("Overpass query: {}", overpassQuery);

                // 執行查詢
                OverpassResponse response = executeOverpassQuery(overpassQuery);

                if (response == null || !response.isValid()) {
                    log.warn("No toilets found or invalid response from Overpass API");
                    return new ArrayList<>();
                }

                // 轉換為 ToiletLocation 列表
                List<ToiletLocation> toilets = convertToToiletLocations(response.getElements(), latitude, longitude);

                // 按距離排序並限制結果數量
                toilets.sort(Comparator.comparingDouble(ToiletLocation::getDistance));
                int maxResults = Math.min(toilets.size(), osmProperties.getCarouselMaxItems());

                List<ToiletLocation> result = toilets.subList(0, maxResults);
                log.info("Found {} toilets within {}m", result.size(), radius);

                return result;

            } catch (Exception e) {
                log.error("Error searching nearby toilets using OSM: {}", e.getMessage(), e);
                return new ArrayList<>();
            }
        });
    }

    /**
     * 建立 Overpass API 查詢語句
     */
    private String buildOverpassQuery(double latitude, double longitude, int radius) {
        // TODO 將半徑轉換為度數（粗略計算）
        double radiusInDegrees = radius / 111320.0; // 1度約等於111320公尺

        double minLat = latitude - radiusInDegrees;
        double maxLat = latitude + radiusInDegrees;
        double minLon = longitude - radiusInDegrees;
        double maxLon = longitude + radiusInDegrees;

        StringBuilder query = new StringBuilder();
        query.append("[out:json][timeout:25];\n");
        query.append("(\n");

        // 搜尋直接的廁所設施
        for (String tag : osmProperties.getToiletTags()) {
            query.append(String.format("  node[%s](%.6f,%.6f,%.6f,%.6f);\n", tag, minLat, minLon, maxLat, maxLon));
            query.append(String.format("  way[%s](%.6f,%.6f,%.6f,%.6f);\n", tag, minLat, minLon, maxLat, maxLon));
        }

        // 搜尋可能包含廁所的設施
        for (String tag : osmProperties.getFacilityTags()) {
            query.append(String.format("  node[%s][\"toilets\"=\"yes\"](%.6f,%.6f,%.6f,%.6f);\n", tag, minLat, minLon, maxLat, maxLon));
            query.append(String.format("  way[%s][\"toilets\"=\"yes\"](%.6f,%.6f,%.6f,%.6f);\n", tag, minLat, minLon, maxLat, maxLon));
        }

        query.append(");\n");
        query.append("out geom meta;\n");

        return query.toString();
    }

    /**
     * 執行 Overpass 查詢
     */
    private OverpassResponse executeOverpassQuery(String query) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> request = new HttpEntity<>(query, headers);

            ResponseEntity<String> response = overpassRestTemplate.postForEntity(osmProperties.getOverpassBaseUrl(), request, String.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.warn("Overpass API returned status: {}", response.getStatusCode());
                return null;
            }

            return objectMapper.readValue(response.getBody(), OverpassResponse.class);

        } catch (RestClientException e) {
            log.error("HTTP error calling Overpass API: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error parsing Overpass response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 將 OSM 元素轉換為 ToiletLocation
     */
    private List<ToiletLocation> convertToToiletLocations(List<OsmElement> elements, double userLat, double userLon) {
        List<ToiletLocation> toilets = new ArrayList<>();

        for (OsmElement element : elements) {
            Double elementLat = element.getEffectiveLat();
            Double elementLon = element.getEffectiveLon();

            if (elementLat == null || elementLon == null) {
                log.debug("Skipping element {} due to missing coordinates", element.getId());
                continue; // 跳過沒有座標的元素
            }

            ToiletLocation toilet = new ToiletLocation();

            // 基本資訊
            toilet.setName(determineName(element));
            toilet.setLatitude(elementLat);
            toilet.setLongitude(elementLon);
            toilet.setDistance(calculateDistance(userLat, userLon, elementLat, elementLon));

            // 地址資訊（如果有）
            String address = element.getTag("addr:full");
            if (address == null) {
                address = buildAddressFromTags(element);
            }
            toilet.setVicinity(address);

            // 評分資訊（OSM 沒有評分系統，使用其他指標）
            toilet.setRating(determineQualityRating(element));

            // 營業狀態（基於營業時間）
            toilet.setOpen(determineOpenStatus(element));

            toilets.add(toilet);
        }

        return toilets;
    }

    /**
     * 決定廁所名稱
     */
    private String determineName(OsmElement element) {
        String name = element.getName();
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }

        // 根據標籤類型生成名稱
        if (element.hasTagValue("amenity", "toilets")) {
            return "公共廁所";
        }
        if (element.hasTagValue("building", "toilets")) {
            return "廁所建築";
        }
        if (element.hasTagValue("amenity", "restaurant")) {
            return element.getName() != null ? element.getName() + " (餐廳)" : "餐廳廁所";
        }
        if (element.hasTagValue("amenity", "cafe")) {
            return element.getName() != null ? element.getName() + " (咖啡廳)" : "咖啡廳廁所";
        }
        if (element.hasTagValue("shop", "convenience")) {
            return element.getName() != null ? element.getName() + " (便利商店)" : "便利商店廁所";
        }
        if (element.hasTagValue("amenity", "fuel")) {
            return element.getName() != null ? element.getName() + " (加油站)" : "加油站廁所";
        }

        return "廁所設施";
    }

    /**
     * 從標籤建立地址
     */
    private String buildAddressFromTags(OsmElement element) {
        StringBuilder address = new StringBuilder();

        String houseNumber = element.getTag("addr:housenumber");
        String street = element.getTag("addr:street");
        String district = element.getTag("addr:district");
        String city = element.getTag("addr:city");
        String state = element.getTag("addr:state");

        if (city != null) address.append(city);
        if (district != null) {
            if (address.length() > 0) address.append(" ");
            address.append(district);
        }
        if (street != null) {
            if (address.length() > 0) address.append(" ");
            address.append(street);
        }
        if (houseNumber != null) {
            if (address.length() > 0) address.append(" ");
            address.append(houseNumber);
        }

        return address.length() > 0 ? address.toString() : null;
    }

    /**
     * 決定品質評分（基於 OSM 標籤）
     */
    private String determineQualityRating(OsmElement element) {
        double rating = 3.0; // 基礎評分

        // 有名稱加分
        if (element.getName() != null && !element.getName().trim().isEmpty()) {
            rating += 0.5;
        }

        // 無障礙設施加分
        if (element.isWheelchairAccessible()) {
            rating += 0.3;
        }

        // 免費使用加分
        if (element.isFree()) {
            rating += 0.2;
        }

        // 有營業時間資訊加分
        if (element.getOpeningHours() != null) {
            rating += 0.2;
        }

        // 特殊設施類型調整
        if (element.hasTagValue("amenity", "toilets")) {
            rating += 0.3; // 專門的廁所設施
        }

        return String.format("%.1f", Math.min(rating, 5.0));
    }

    /**
     * TODO 決定營業狀態
     */
    private boolean determineOpenStatus(OsmElement element) {
        String openingHours = element.getOpeningHours();

        if (openingHours == null) {
            // 公共廁所假設 24 小時開放
            return element.hasTagValue("amenity", "toilets") || element.hasTagValue("building", "toilets");
        }

        // 24/7 或 24 小時
        if ("24/7".equals(openingHours) || "24 hours".equalsIgnoreCase(openingHours)) {
            return true;
        }

        return true;
    }

    @Override
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // 地球半徑（公尺）

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}