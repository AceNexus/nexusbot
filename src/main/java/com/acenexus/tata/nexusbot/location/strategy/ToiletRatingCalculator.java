package com.acenexus.tata.nexusbot.location.strategy;

import com.acenexus.tata.nexusbot.location.OsmElement;
import org.springframework.stereotype.Component;

import static com.acenexus.tata.nexusbot.location.LocationConstants.OsmTags;
import static com.acenexus.tata.nexusbot.location.LocationConstants.Rating;

/**
 * 廁所評分計算器
 * 根據 OSM 元素的標籤計算廁所品質評分
 */
@Component
public class ToiletRatingCalculator {

    /**
     * 根據 OSM 標籤計算品質評分
     *
     * @param element OSM 元素
     * @return 評分字串（格式化為 1 位小數）
     */
    public String calculateRating(OsmElement element) {
        double rating = Rating.BASE_RATING;

        if (hasNonEmptyName(element)) {
            rating += Rating.BONUS_HAS_NAME;
        }

        if (element.isWheelchairAccessible()) {
            rating += Rating.BONUS_WHEELCHAIR_ACCESSIBLE;
        }

        if (element.isFree()) {
            rating += Rating.BONUS_FREE;
        }

        if (hasOpeningHoursInfo(element)) {
            rating += Rating.BONUS_HAS_OPENING_HOURS;
        }

        if (isDedicatedToiletFacility(element)) {
            rating += Rating.BONUS_DEDICATED_TOILET;
        }

        return String.format("%.1f", Math.min(rating, Rating.MAX_RATING));
    }

    /**
     * 檢查是否具有非空名稱
     */
    private boolean hasNonEmptyName(OsmElement element) {
        String name = element.getName();
        return name != null && !name.trim().isEmpty();
    }

    /**
     * 檢查是否有營業時間資訊
     */
    private boolean hasOpeningHoursInfo(OsmElement element) {
        return element.getOpeningHours() != null;
    }

    /**
     * 檢查是否為專門的公共廁所設施
     */
    private boolean isDedicatedToiletFacility(OsmElement element) {
        return element.hasTagValue(OsmTags.AMENITY, OsmTags.TOILETS);
    }
}
