package com.acenexus.tata.nexusbot.location;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface LocationService {

    /**
     * 搜尋附近的廁所
     *
     * @param latitude  緯度
     * @param longitude 經度
     * @param radius    搜尋半徑（公尺）
     * @return 附近廁所列表
     */
    CompletableFuture<List<ToiletLocation>> findNearbyToilets(double latitude, double longitude, int radius);

    /**
     * 計算兩點間距離
     *
     * @param lat1 起點緯度
     * @param lon1 起點經度
     * @param lat2 終點緯度
     * @param lon2 終點經度
     * @return 距離（公尺）
     */
    double calculateDistance(double lat1, double lon1, double lat2, double lon2);
}