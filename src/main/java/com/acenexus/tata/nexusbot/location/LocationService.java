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
}