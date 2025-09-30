package com.acenexus.tata.nexusbot.location;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsmElement {
    private String type; // "node", "way", "relation"
    private Long id;
    private Double lat;
    private Double lon;
    private Map<String, String> tags;

    // way 元素的幾何資訊
    private List<GeometryNode> geometry;

    // 額外實用資訊
    private String access;        // 存取權限：yes, permissive, private 等
    private String description;   // 描述資訊

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeometryNode {
        private Double lat;
        private Double lon;
    }

    /**
     * 獲取標籤值
     */
    public String getTag(String key) {
        return tags != null ? tags.get(key) : null;
    }

    /**
     * 檢查標籤是否匹配指定值
     */
    public boolean hasTagValue(String key, String value) {
        return value.equals(getTag(key));
    }

    /**
     * 獲取名稱（多語言支援）
     */
    public String getName() {
        if (tags == null) return null;

        // 優先順序：name:zh-tw -> name:zh -> name
        String name = tags.get("name:zh-tw");
        if (name != null && !name.trim().isEmpty()) return name;

        name = tags.get("name:zh");
        if (name != null && !name.trim().isEmpty()) return name;

        return tags.get("name");
    }

    /**
     * 是否需要付費
     */
    public boolean isFree() {
        String fee = getTag("fee");
        return !"yes".equals(fee);
    }

    /**
     * 是否有無障礙設施
     */
    public boolean isWheelchairAccessible() {
        String wheelchair = getTag("wheelchair");
        return "yes".equals(wheelchair);
    }

    /**
     * 獲取營業時間
     */
    public String getOpeningHours() {
        return getTag("opening_hours");
    }

    /**
     * 獲取存取權限
     */
    public String getAccess() {
        return getTag("access");
    }

    /**
     * 獲取有效的緯度（處理 way 元素）
     */
    public Double getEffectiveLat() {
        if (lat != null) {
            return lat;
        }

        // 對於 way 元素，計算幾何中心
        if (geometry != null && !geometry.isEmpty()) {
            double sum = 0.0;
            int count = 0;
            for (GeometryNode node : geometry) {
                if (node.getLat() != null) {
                    sum += node.getLat();
                    count++;
                }
            }
            return count > 0 ? sum / count : null;
        }

        return null;
    }

    /**
     * 獲取有效的經度（處理 way 元素）
     */
    public Double getEffectiveLon() {
        if (lon != null) {
            return lon;
        }

        // 對於 way 元素，計算幾何中心
        if (geometry != null && !geometry.isEmpty()) {
            double sum = 0.0;
            int count = 0;
            for (GeometryNode node : geometry) {
                if (node.getLon() != null) {
                    sum += node.getLon();
                    count++;
                }
            }
            return count > 0 ? sum / count : null;
        }

        return null;
    }
}