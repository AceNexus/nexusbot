package com.acenexus.tata.nexusbot.location;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OverpassResponse {
    private List<OsmElement> elements;

    /**
     * 檢查響應是否有效
     */
    public boolean isValid() {
        return elements != null && !elements.isEmpty();
    }
}