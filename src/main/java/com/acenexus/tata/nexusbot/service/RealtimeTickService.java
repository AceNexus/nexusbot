package com.acenexus.tata.nexusbot.service;

import com.acenexus.tata.nexusbot.client.FugleWebSocketClient;
import com.acenexus.tata.nexusbot.config.FugleConfig;
import com.acenexus.tata.nexusbot.dto.TickData;
import com.acenexus.tata.nexusbot.dto.TickStats;
import com.acenexus.tata.nexusbot.dto.TickType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 即時成交服務
 * 純 Fugle WebSocket 模式
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeTickService {

    private final FugleWebSocketClient fugleClient;
    private final FugleConfig fugleConfig;

    // 當日成交明細快取 (symbol -> ticks)
    private final Map<String, List<TickData>> tickHistory = new ConcurrentHashMap<>();

    // 監控中的股票
    private final Set<String> monitoredSymbols = ConcurrentHashMap.newKeySet();

    // 大單監聽器
    private final List<BigTradeListener> bigTradeListeners = Collections.synchronizedList(new ArrayList<>());

    // Tick 廣播監聽器
    private final List<TickBroadcastListener> tickBroadcastListeners = Collections.synchronizedList(new ArrayList<>());

    // 大單門檻（張）
    private static final int BIG_TRADE_THRESHOLD = 100;

    /**
     * 開始監控股票（即時模式 - Fugle WebSocket）
     *
     * @return 訂閱結果：FUGLE（成功）、NO_API_KEY（未設定）、LIMIT_EXCEEDED（額度已滿）
     */
    public String startMonitor(String symbol) {
        // 檢查 Fugle API Key
        if (!fugleConfig.isEnabled()) {
            log.warn("Fugle API Key 未設定，無法使用即時成交功能");
            return "NO_API_KEY";
        }

        // 檢查是否已訂閱
        if (fugleClient.isSubscribed(symbol)) {
            log.info("{} 已在監控中", symbol);
            return "FUGLE";
        }

        // 檢查額度
        if (fugleClient.getRemainingSlots() <= 0) {
            log.warn("Fugle 訂閱額度已滿 ({}/{})",
                    fugleClient.getSubscribedCount(), fugleConfig.getMaxSubscriptions());
            return "LIMIT_EXCEEDED";
        }

        // 初始化歷史列表
        tickHistory.putIfAbsent(symbol, Collections.synchronizedList(new ArrayList<>()));

        // 訂閱 Fugle WebSocket
        boolean subscribed = fugleClient.subscribe(symbol, tick -> onTickReceived(symbol, tick));

        if (subscribed) {
            monitoredSymbols.add(symbol);
            log.info("即時監控啟動: {} (Fugle WebSocket)", symbol);
            return "FUGLE";
        }

        return "FAILED";
    }

    /**
     * 停止監控股票
     */
    public void stopMonitor(String symbol) {
        if (monitoredSymbols.remove(symbol)) {
            fugleClient.unsubscribe(symbol);
            log.info("停止監控: {}", symbol);
        }
    }

    /**
     * 處理收到的成交資料
     */
    private void onTickReceived(String symbol, TickData tick) {
        // 儲存到歷史
        List<TickData> history = tickHistory.get(symbol);
        if (history != null) {
            history.add(tick);
        }

        // 廣播給所有監聽器
        broadcastTick(symbol, tick);

        // 大單警示
        int lots = tick.getVolumeLots();
        if (lots >= BIG_TRADE_THRESHOLD) {
            notifyBigTrade(symbol, tick, lots);
        }

        log.debug("[{}] {} 成交 {} 張 @ {} ({})",
                tick.getTime() != null ? tick.getTime().toLocalTime() : "-",
                symbol,
                lots,
                tick.getPrice(),
                tick.getTickType().getDisplayName());
    }

    /**
     * 取得當日成交明細
     */
    public List<TickData> getTickHistory(String symbol) {
        return tickHistory.getOrDefault(symbol, Collections.emptyList());
    }

    /**
     * 取得最近 N 筆成交
     */
    public List<TickData> getRecentTicks(String symbol, int limit) {
        List<TickData> ticks = tickHistory.get(symbol);
        if (ticks == null || ticks.isEmpty()) {
            return Collections.emptyList();
        }

        int size = ticks.size();
        int start = Math.max(0, size - limit);
        return new ArrayList<>(ticks.subList(start, size));
    }

    /**
     * 取得即時統計
     */
    public TickStats getRealtimeStats(String symbol) {
        List<TickData> ticks = tickHistory.get(symbol);

        if (ticks == null || ticks.isEmpty()) {
            return TickStats.empty(symbol);
        }

        return calculateStats(symbol, ticks);
    }

    /**
     * 計算統計數據
     */
    private TickStats calculateStats(String symbol, List<TickData> ticks) {
        if (ticks.isEmpty()) {
            return TickStats.empty(symbol);
        }

        // 計算外盤量
        long buyVolume = ticks.stream()
                .filter(t -> t.getTickType() == TickType.BUY)
                .mapToLong(TickData::getVolume)
                .sum();

        // 計算內盤量
        long sellVolume = ticks.stream()
                .filter(t -> t.getTickType() == TickType.SELL)
                .mapToLong(TickData::getVolume)
                .sum();

        // 總成交量
        long totalVolume = ticks.stream()
                .mapToLong(TickData::getVolume)
                .sum();

        // 計算均價
        BigDecimal totalAmount = ticks.stream()
                .map(t -> t.getPrice().multiply(BigDecimal.valueOf(t.getVolume())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgPrice = totalVolume > 0
                ? totalAmount.divide(BigDecimal.valueOf(totalVolume), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 最高/最低價
        BigDecimal highPrice = ticks.stream()
                .map(TickData::getPrice)
                .filter(p -> p != null)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal lowPrice = ticks.stream()
                .map(TickData::getPrice)
                .filter(p -> p != null)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // 外盤比例
        double buyRatio = (buyVolume + sellVolume) > 0
                ? (double) buyVolume / (buyVolume + sellVolume) * 100
                : 50.0;

        TickData lastTick = ticks.get(ticks.size() - 1);
        TickData firstTick = ticks.get(0);

        return TickStats.builder()
                .symbol(symbol)
                .totalTicks(ticks.size())
                .totalVolume(totalVolume)
                .buyVolume(buyVolume)
                .sellVolume(sellVolume)
                .buyRatio(buyRatio)
                .avgPrice(avgPrice)
                .lastPrice(lastTick.getPrice())
                .highPrice(highPrice)
                .lowPrice(lowPrice)
                .openPrice(firstTick.getPrice())
                .updateTime(lastTick.getTime())
                .source("FUGLE")
                .build();
    }

    /**
     * 取得大單列表
     */
    public List<TickData> getBigTrades(String symbol, int thresholdLots) {
        List<TickData> ticks = tickHistory.get(symbol);
        if (ticks == null) {
            return Collections.emptyList();
        }

        int thresholdShares = thresholdLots * 1000;
        return ticks.stream()
                .filter(t -> t.getVolume() >= thresholdShares)
                .collect(Collectors.toList());
    }

    /**
     * 取得監控狀態
     */
    public Map<String, String> getMonitorStatus() {
        Map<String, String> status = new ConcurrentHashMap<>();
        for (String symbol : monitoredSymbols) {
            status.put(symbol, "FUGLE");
        }
        return status;
    }

    /**
     * 取得目前監控中的所有股票代號
     */
    public Set<String> getMonitoredSymbols() {
        return Set.copyOf(monitoredSymbols);
    }

    /**
     * 取得訂閱統計
     */
    public String getSubscriptionSummary() {
        return String.format("Fugle: %d/%d",
                fugleClient.getSubscribedCount(), fugleConfig.getMaxSubscriptions());
    }

    /**
     * 取得已訂閱數量
     */
    public int getSubscribedCount() {
        return fugleClient.getSubscribedCount();
    }

    /**
     * 取得最大訂閱數量
     */
    public int getMaxSubscriptions() {
        return fugleConfig.getMaxSubscriptions();
    }

    /**
     * 檢查是否已訂閱
     */
    public boolean isSubscribed(String symbol) {
        return fugleClient.isSubscribed(symbol);
    }

    /**
     * 檢查 Fugle 是否啟用
     */
    public boolean isFugleEnabled() {
        return fugleConfig.isEnabled();
    }

    /**
     * 檢查 Fugle 是否已連線且通過驗證
     */
    public boolean isFugleReady() {
        return fugleConfig.isEnabled() && fugleClient.isReady();
    }

    /**
     * 每日開盤前清除資料（08:30）
     */
    @Scheduled(cron = "0 30 8 * * MON-FRI")
    public void clearDailyData() {
        tickHistory.clear();
        log.info("已清除當日成交明細快取");
    }

    /**
     * 註冊大單監聽器
     */
    public void addBigTradeListener(BigTradeListener listener) {
        bigTradeListeners.add(listener);
    }

    /**
     * 移除大單監聽器
     */
    public void removeBigTradeListener(BigTradeListener listener) {
        bigTradeListeners.remove(listener);
    }

    /**
     * 通知大單成交
     */
    private void notifyBigTrade(String symbol, TickData tick, int lots) {
        log.info("大單警示: {} {} {} 張 @ {}",
                symbol,
                tick.getTickType().getDisplayName(),
                lots,
                tick.getPrice());

        for (BigTradeListener listener : bigTradeListeners) {
            try {
                listener.onBigTrade(symbol, tick, lots);
            } catch (Exception e) {
                log.error("大單警示回調失敗", e);
            }
        }
    }

    /**
     * 註冊 Tick 廣播監聽器
     */
    public void addTickBroadcastListener(TickBroadcastListener listener) {
        tickBroadcastListeners.add(listener);
    }

    /**
     * 移除 Tick 廣播監聽器
     */
    public void removeTickBroadcastListener(TickBroadcastListener listener) {
        tickBroadcastListeners.remove(listener);
    }

    /**
     * 廣播 Tick 給所有監聽器
     */
    private void broadcastTick(String symbol, TickData tick) {
        for (TickBroadcastListener listener : tickBroadcastListeners) {
            try {
                listener.onTick(symbol, tick);
            } catch (Exception e) {
                log.error("Tick 廣播回調失敗", e);
            }
        }
    }

    /**
     * 大單監聽器介面
     */
    @FunctionalInterface
    public interface BigTradeListener {
        void onBigTrade(String symbol, TickData tick, int lots);
    }

    /**
     * Tick 廣播監聽器介面
     */
    @FunctionalInterface
    public interface TickBroadcastListener {
        void onTick(String symbol, TickData tick);
    }
}
