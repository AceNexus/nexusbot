package com.acenexus.tata.nexusbot.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToiletLocation {
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private double distance;
    private String placeId;
    private String vicinity;
    private boolean isOpen;
    private String rating;

    public String getDistanceFormatted() {
        if (distance < 1000) {
            return String.format("%.0f公尺", distance);
        } else {
            return String.format("%.1f公里", distance / 1000);
        }
    }
}