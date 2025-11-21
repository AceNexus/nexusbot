package com.acenexus.tata.nexusbot.location.mapper;

import com.acenexus.tata.nexusbot.location.OsmElement;
import com.acenexus.tata.nexusbot.location.ToiletLocation;
import com.acenexus.tata.nexusbot.location.strategy.ToiletNamingStrategy;
import com.acenexus.tata.nexusbot.location.strategy.ToiletRatingCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.acenexus.tata.nexusbot.location.LocationConstants.AddressTags;
import static com.acenexus.tata.nexusbot.location.LocationConstants.Geography;
import static com.acenexus.tata.nexusbot.location.LocationConstants.OsmTags;

/**
 * ToiletLocation 映射器
 * 負責將 OSM 元素轉換為 ToiletLocation 物件
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ToiletLocationMapper {

    private final ToiletNamingStrategy namingStrategy;
    private final ToiletRatingCalculator ratingCalculator;

    /**
     * 將 OSM 元素列表轉換為 ToiletLocation 列表
     *
     * @param elements OSM 元素列表
     * @param userLat  使用者緯度
     * @param userLon  使用者經度
     * @return ToiletLocation 列表
     */
    public List<ToiletLocation> mapToToiletLocations(List<OsmElement> elements, double userLat, double userLon) {
        List<ToiletLocation> toilets = new ArrayList<>();

        for (OsmElement element : elements) {
            ToiletLocation toilet = mapElementToLocation(element, userLat, userLon);
            if (toilet != null) {
                toilets.add(toilet);
            }
        }

        return toilets;
    }

    /**
     * 將單個 OSM 元素轉換為 ToiletLocation
     *
     * @param element OSM 元素
     * @param userLat 使用者緯度
     * @param userLon 使用者經度
     * @return ToiletLocation 或 null（如果元素無效）
     */
    private ToiletLocation mapElementToLocation(OsmElement element, double userLat, double userLon) {
        Double elementLat = element.getEffectiveLat();
        Double elementLon = element.getEffectiveLon();

        if (elementLat == null || elementLon == null) {
            log.debug("Skipping element {} due to missing coordinates", element.getId());
            return null;
        }

        ToiletLocation toilet = new ToiletLocation();

        // 基本資訊
        toilet.setName(namingStrategy.resolveName(element));
        toilet.setLatitude(elementLat);
        toilet.setLongitude(elementLon);
        toilet.setDistance(calculateDistance(userLat, userLon, elementLat, elementLon));

        // 地址資訊
        toilet.setVicinity(extractAddress(element));

        // 評分資訊
        toilet.setRating(ratingCalculator.calculateRating(element));

        // 營業狀態
        toilet.setOpen(evaluateOpenStatus(element));

        // 無障礙設施
        toilet.setHasWheelchairAccess(checkWheelchairAccessibility(element));

        return toilet;
    }

    /**
     * 取得地址資訊（優先使用完整地址，否則從標籤組合）
     */
    private String extractAddress(OsmElement element) {
        String fullAddress = element.getTag(AddressTags.FULL);
        if (fullAddress != null) {
            return fullAddress;
        }
        return assembleAddressFromTags(element);
    }

    /**
     * 從地址標籤組合完整地址字串
     */
    private String assembleAddressFromTags(OsmElement element) {
        StringBuilder address = new StringBuilder();

        String city = element.getTag(AddressTags.CITY);
        String district = element.getTag(AddressTags.DISTRICT);
        String street = element.getTag(AddressTags.STREET);
        String houseNumber = element.getTag(AddressTags.HOUSE_NUMBER);

        if (city != null) {
            address.append(city);
        }
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
     * 評估廁所開放狀態（採保守策略）
     * <p>
     * 邏輯：
     * 1. 無營業時間資訊 -> 公共廁所假設開放，其他假設關閉
     * 2. 明確標示 24/7 -> 開放
     * 3. 有營業時間但無法解析 -> 保守假設關閉（避免誤導使用者）
     */
    private boolean evaluateOpenStatus(OsmElement element) {
        String openingHours = element.getOpeningHours();

        // 1. 沒有營業時間資訊
        if (openingHours == null || openingHours.trim().isEmpty()) {
            // 公共廁所假設 24 小時開放
            return element.hasTagValue(OsmTags.AMENITY, OsmTags.TOILETS) ||
                    element.hasTagValue(OsmTags.BUILDING, OsmTags.TOILETS);
        }

        // 2. 明確標示 24 小時營業
        String hoursLower = openingHours.toLowerCase().trim();
        if (OsmTags.OPEN_24_7.equals(hoursLower) ||
                OsmTags.OPEN_24_HOURS.equals(hoursLower) ||
                OsmTags.OPEN_24_HOURS_NO_SPACE.equals(hoursLower)) {
            return true;
        }

        // 3. 有營業時間但無法準確判斷當前是否營業，保守策略：顯示為關閉
        return false;
    }

    /**
     * 檢查是否具備無障礙設施
     */
    private boolean checkWheelchairAccessibility(OsmElement element) {
        // 檢查 wheelchair 標籤
        String wheelchair = element.getTag(OsmTags.WHEELCHAIR);
        if (OsmTags.YES.equals(wheelchair)) {
            return true;
        }

        // 檢查 toilets:wheelchair 標籤（專門針對廁所）
        String toiletsWheelchair = element.getTag(OsmTags.TOILETS_WHEELCHAIR);
        return OsmTags.YES.equals(toiletsWheelchair);
    }

    /**
     * 計算兩點間距離（Haversine 公式）
     *
     * @param lat1 起點緯度
     * @param lon1 起點經度
     * @param lat2 終點緯度
     * @param lon2 終點經度
     * @return 距離（公尺）
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double haversineA = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double centralAngle = 2 * Math.atan2(Math.sqrt(haversineA), Math.sqrt(1 - haversineA));

        return Geography.EARTH_RADIUS_METERS * centralAngle;
    }
}
