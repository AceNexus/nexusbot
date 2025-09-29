package com.acenexus.tata.nexusbot.location;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OverpassResponse {
    private String version;
    private String generator;
    private Osm3s osm3s;
    private List<OsmElement> elements;

    @Data
    public static class Osm3s {
        private String timestamp_osm_base;
        private String copyright;
    }

    /**
     * 檢查響應是否有效
     */
    public boolean isValid() {
        return elements != null && !elements.isEmpty();
    }

    /**
     * 獲取元素數量
     */
    public int getElementCount() {
        return elements != null ? elements.size() : 0;
    }
}