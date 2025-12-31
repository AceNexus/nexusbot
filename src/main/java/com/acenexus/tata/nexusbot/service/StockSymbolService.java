package com.acenexus.tata.nexusbot.service;

import com.acenexus.tata.nexusbot.client.FinMindApiClient;
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
 * 快取策略：第一次查詢時從 FinMind API 載入，快取 1 天後過期
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockSymbolService {

    private final FinMindApiClient finMindApiClient;

    /**
     * 台股名稱 -> 代號快取
     * Key: 股票名稱 (e.g., "台積電")
     * Value: 股票代號 (e.g., "2330")
     * 快取設定：
     * - 1 天後過期 (自動刷新)
     * - 最多 10000 筆記錄
     */
    private final Cache<String, String> taiwanStockCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(10000)
            .build();

    /**
     * 解析輸入的股票代號或名稱，返回股票代號
     * 支援以下輸入格式：
     * - 股票代號（如 "2330"）
     * - 股票中文名稱（如 "台積電"）
     * 快取策略：
     * - 第一次查詢時從 FinMind API 載入所有台股資料並快取到記憶體
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
     * 從 FinMind API 載入台股資料到快取
     */
    private synchronized void loadTaiwanStockCache() {
        // 再次檢查，避免重複載入
        if (taiwanStockCache.estimatedSize() > 0) {
            log.debug("Taiwan stock cache already loaded, skipping");
            return;
        }

        log.info("Loading Taiwan stock data from FinMind API...");
        Map<String, String> nameToSymbolMap = finMindApiClient.getTaiwanStockNameToSymbolMap();

        if (nameToSymbolMap.isEmpty()) {
            log.warn("Failed to load Taiwan stock data from FinMind API");
            return;
        }

        // 批次寫入快取
        taiwanStockCache.putAll(nameToSymbolMap);

        log.info("Taiwan stock cache loaded successfully - count={}", taiwanStockCache.estimatedSize());
    }

    /**
     * 清除快取（用於測試或強制刷新）
     */
    public void clearCache() {
        taiwanStockCache.invalidateAll();
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
        for (Map.Entry<String, String> entry : taiwanStockCache.asMap().entrySet()) {
            result.add(StockSymbolDto.builder()
                    .name(entry.getKey())
                    .symbol(entry.getValue())
                    .build());
        }

        // 根據代號排序，方便前端顯示
        result.sort((a, b) -> a.getSymbol().compareTo(b.getSymbol()));

        return result;
    }
}
