package com.acenexus.tata.nexusbot.service;

import com.acenexus.tata.nexusbot.client.TwseApiClient;
import com.acenexus.tata.nexusbot.dto.StockSymbolDto;
import com.acenexus.tata.nexusbot.enums.StockMarket;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 股票代號服務
 * 使用 Caffeine 記憶體快取股票名稱與代號的映射
 * 快取策略：第一次查詢時從 TWSE API 載入，快取 1 天後過期
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockSymbolService {

    private final TwseApiClient twseApiClient;

    /**
     * 台股名稱 -> 代號快取
     */
    private final Cache<String, String> taiwanStockCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(10000)
            .build();

    /**
     * 台股代號 -> 市場快取 (上市/上櫃)
     */
    private final Cache<String, String> symbolToMarketCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(10000)
            .build();

    /**
     * 解析輸入的股票代號或名稱，返回股票代號
     * 支援以下輸入格式：
     * - 股票代號（如 "2330"）
     * - 股票中文名稱（如 "台積電"）
     * 快取策略：
     * - 第一次查詢時從 TWSE API 載入所有台股資料並快取到記憶體
     * - 快取 1 天後自動過期並重新載入
     *
     * @param input  使用者輸入的股票代號或名稱
     * @param market 市場類型
     * @return 解析後的股票代號，若找不到則返回原輸入
     */
    public String resolveSymbol(String input, StockMarket market) {
        if (input == null || input.trim().isEmpty()) {
            log.warn("Empty input for symbol resolution");
            return input;
        }

        // 目前只支援台股市場
        if (market != StockMarket.TW) {
            log.debug("Market {} not supported for symbol resolution, returning original input", market);
            return input;
        }

        String trimmedInput = input.trim();
        log.debug("Resolving stock symbol - input={}, market={}", trimmedInput, market);

        // 檢查快取大小，如果快取為空則載入資料
        if (taiwanStockCache.estimatedSize() == 0) {
            loadTaiwanStockCache();
        }

        // 1. 如果輸入是純數字，假設是股票代號，直接返回
        if (trimmedInput.matches("\\d+")) {
            log.debug("Input is numeric, assuming it's a stock symbol - input={}", trimmedInput);
            return trimmedInput;
        }

        // 2. 嘗試從快取中查找名稱對應的代號
        String symbol = taiwanStockCache.getIfPresent(trimmedInput);
        if (symbol != null) {
            log.info("Resolved by exact name match - input={}, symbol={}", trimmedInput, symbol);
            return symbol;
        }

        // 3. 嘗試部分匹配（模糊搜尋）
        for (Map.Entry<String, String> entry : taiwanStockCache.asMap().entrySet()) {
            if (entry.getKey().contains(trimmedInput)) {
                log.info("Resolved by partial name match - input={}, name={}, symbol={}",
                        trimmedInput, entry.getKey(), entry.getValue());
                return entry.getValue();
            }
        }

        // 4. 找不到匹配，返回原輸入（可能本身就是股票代號）
        log.warn("Stock symbol not found in cache - input={}, market={}, returning original input",
                trimmedInput, market);
        return trimmedInput;
    }

    /**
     * 從 TWSE API 載入台股資料到快取
     */
    private synchronized void loadTaiwanStockCache() {
        // 再次檢查，避免重複載入
        if (taiwanStockCache.estimatedSize() > 0 && symbolToMarketCache.estimatedSize() > 0) {
            log.debug("Taiwan stock cache already loaded, skipping");
            return;
        }

        log.info("Loading Taiwan stock data from TWSE API...");
        
        // 載入上市股票
        Map<String, String> twseMap = twseApiClient.getTaiwanStockNameToSymbolMap();
        if (!twseMap.isEmpty()) {
            taiwanStockCache.putAll(twseMap);
            twseMap.values().forEach(symbol -> symbolToMarketCache.put(symbol, "上市"));
        }

        // 載入上櫃股票
        Map<String, String> tpexMap = twseApiClient.getTpexStockNameToSymbolMap();
        if (!tpexMap.isEmpty()) {
            taiwanStockCache.putAll(tpexMap);
            tpexMap.values().forEach(symbol -> symbolToMarketCache.put(symbol, "上櫃"));
        }

        log.info("Taiwan stock cache loaded successfully - total count={}, market count={}", 
                taiwanStockCache.estimatedSize(), symbolToMarketCache.estimatedSize());
    }

    /**
     * 根據代號取得市場類型
     */
    public String getMarketBySymbol(String symbol) {
        if (symbolToMarketCache.estimatedSize() == 0) {
            loadTaiwanStockCache();
        }
        return symbolToMarketCache.getIfPresent(symbol);
    }

    /**
     * 清除快取（用於測試或強制刷新）
     */
    public void clearCache() {
        taiwanStockCache.invalidateAll();
        symbolToMarketCache.invalidateAll();
        log.info("Taiwan stock cache cleared");
    }

    /**
     * 取得快取大小
     *
     * @return 快取中的記錄數
     */
    public long getCacheSize() {
        return taiwanStockCache.estimatedSize();
    }

    /**
     * 取得所有台股代號列表
     *
     * @return 包含代號和名稱的列表
     */
    public List<StockSymbolDto> getAllStocks() {
        if (taiwanStockCache.estimatedSize() == 0) {
            loadTaiwanStockCache();
        }

        List<StockSymbolDto> result = new ArrayList<>();
        // 為了確保不重複且包含市場資訊，從 symbolToMarketCache 遍歷
        Map<String, String> symbolToNameMap = new java.util.HashMap<>();
        taiwanStockCache.asMap().forEach((name, symbol) -> symbolToNameMap.put(symbol, name));

        symbolToMarketCache.asMap().forEach((symbol, market) -> {
            String name = symbolToNameMap.get(symbol);
            if (name != null) {
                result.add(StockSymbolDto.builder()
                        .name(name)
                        .symbol(symbol)
                        .market(market)
                        .build());
            }
        });

        // 根據代號排序，方便前端顯示
        result.sort((a, b) -> a.getSymbol().compareTo(b.getSymbol()));

        return result;
    }
}
