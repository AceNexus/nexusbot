package com.acenexus.tata.nexusbot.location.strategy;

import com.acenexus.tata.nexusbot.location.OsmElement;
import org.springframework.stereotype.Component;

import static com.acenexus.tata.nexusbot.location.LocationConstants.DefaultNames;
import static com.acenexus.tata.nexusbot.location.LocationConstants.OsmTags;

/**
 * 廁所命名策略
 * 根據 OSM 元素的標籤決定廁所設施的顯示名稱
 */
@Component
public class ToiletNamingStrategy {

    /**
     * 根據 OSM 元素標籤決定顯示名稱
     *
     * @param element OSM 元素
     * @return 廁所名稱
     */
    public String resolveName(OsmElement element) {
        String existingName = element.getName();
        if (existingName != null && !existingName.trim().isEmpty()) {
            return existingName;
        }

        if (element.hasTagValue(OsmTags.AMENITY, OsmTags.TOILETS)) {
            return DefaultNames.PUBLIC_TOILET;
        }
        if (element.hasTagValue(OsmTags.BUILDING, OsmTags.TOILETS)) {
            return DefaultNames.TOILET_BUILDING;
        }

        if (element.hasTagValue(OsmTags.AMENITY, OsmTags.RESTAURANT)) {
            return formatFacilityName(element, DefaultNames.RESTAURANT_SUFFIX, DefaultNames.RESTAURANT_DEFAULT);
        }
        if (element.hasTagValue(OsmTags.AMENITY, OsmTags.CAFE)) {
            return formatFacilityName(element, DefaultNames.CAFE_SUFFIX, DefaultNames.CAFE_DEFAULT);
        }
        if (element.hasTagValue(OsmTags.SHOP, OsmTags.CONVENIENCE)) {
            return formatFacilityName(element, DefaultNames.CONVENIENCE_STORE_SUFFIX, DefaultNames.CONVENIENCE_STORE_DEFAULT);
        }
        if (element.hasTagValue(OsmTags.AMENITY, OsmTags.FUEL)) {
            return formatFacilityName(element, DefaultNames.GAS_STATION_SUFFIX, DefaultNames.GAS_STATION_DEFAULT);
        }

        return DefaultNames.TOILET_FACILITY;
    }

    /**
     * 格式化設施名稱（附加類型後綴或使用預設名稱）
     *
     * @param element     OSM 元素
     * @param typeSuffix  類型後綴（例如：" (餐廳)"）
     * @param defaultName 預設名稱（當無名稱時使用）
     * @return 格式化後的名稱
     */
    private String formatFacilityName(OsmElement element, String typeSuffix, String defaultName) {
        String name = element.getName();
        if (name != null && !name.trim().isEmpty()) {
            return name + typeSuffix;
        }
        return defaultName;
    }
}
