package com.acenexus.tata.nexusbot.controller;

import com.acenexus.tata.nexusbot.client.FinMindApiClient;
import com.acenexus.tata.nexusbot.client.TwseApiClient;
import com.acenexus.tata.nexusbot.dto.CandlestickData;
import com.acenexus.tata.nexusbot.dto.InstitutionalInvestorsData;
import com.acenexus.tata.nexusbot.dto.StockSymbolDto;
import com.acenexus.tata.nexusbot.dto.TechnicalIndicatorData;
import com.acenexus.tata.nexusbot.dto.TickData;
import com.acenexus.tata.nexusbot.dto.TickStats;
import com.acenexus.tata.nexusbot.enums.StockMarket;
import com.acenexus.tata.nexusbot.service.RealtimeTickService;
import com.acenexus.tata.nexusbot.service.StockChipService;
import com.acenexus.tata.nexusbot.service.StockSymbolService;
import com.acenexus.tata.nexusbot.service.TechnicalAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final FinMindApiClient finMindApiClient;
    private final TwseApiClient twseApiClient;
    private final StockSymbolService stockSymbolService;
    private final StockChipService stockChipService;
    private final TechnicalAnalysisService technicalAnalysisService;
    private final RealtimeTickService realtimeTickService;

    @GetMapping("/list")
    public ResponseEntity<List<StockSymbolDto>> getStockList() {
        List<StockSymbolDto> result = new java.util.ArrayList<>();

        // 上市股票（TWSE）
        Map<String, String> twseMap = twseApiClient.getTaiwanStockNameToSymbolMap();
        twseMap.forEach((name, symbol) -> result.add(StockSymbolDto.builder().symbol(symbol).name(name).market("上市").build()));

        // 上櫃股票（TPEx）
        Map<String, String> tpexMap = twseApiClient.getTpexStockNameToSymbolMap();
        tpexMap.forEach((name, symbol) -> result.add(StockSymbolDto.builder().symbol(symbol).name(name).market("上櫃").build()));

        // 如果都沒資料，fallback 到 stockSymbolService
        if (result.isEmpty()) {
            return ResponseEntity.ok(stockSymbolService.getAllStocks());
        }

        // 依代號排序
        result.sort((a, b) -> a.getSymbol().compareTo(b.getSymbol()));

        log.info("Stock list loaded - 上市: {}, 上櫃: {}, total: {}", twseMap.size(), tpexMap.size(), result.size());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{symbol}/institutional-investors")
    public ResponseEntity<List<InstitutionalInvestorsData>> getInstitutionalInvestors(@PathVariable String symbol, @RequestParam(defaultValue = "10") int days) {
        String resolvedSymbol = stockSymbolService.resolveSymbol(symbol, StockMarket.TW);
        try {
            // 使用 StockChipService (DB First)
            List<InstitutionalInvestorsData> data = stockChipService.getRecentDaysInstitutionalInvestors(resolvedSymbol, days);

            // Fallback to FinMind only if still empty (e.g., very old historical data)
            if (data.isEmpty()) {
                data = finMindApiClient.getRecentDaysInstitutionalInvestors(resolvedSymbol, days);
            }
            return ResponseEntity.ok(data);
        } catch (Exception ex) {
            log.error("Error getting institutional investors for {}", symbol, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{symbol}/institutional-investors/range")
    public ResponseEntity<Map<String, Object>> getInstitutionalInvestorsRange(@PathVariable String symbol, @RequestParam String startDate, @RequestParam String endDate) {
        try {
            String resolvedSymbol = stockSymbolService.resolveSymbol(symbol, StockMarket.TW);
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            // 使用 StockChipService 從資料庫查詢，若缺資料會自動補齊
            List<InstitutionalInvestorsData> dailyData = stockChipService.getInstitutionalInvestorsRange(resolvedSymbol, start, end);

            if (dailyData.isEmpty()) {
                Map<String, Object> emptySummary = new java.util.HashMap<>();
                emptySummary.put("symbol", symbol);
                emptySummary.put("name", symbol);
                emptySummary.put("foreign", 0);
                emptySummary.put("trust", 0);
                emptySummary.put("dealer", 0);
                emptySummary.put("total", 0);
                emptySummary.put("days", 0);
                return ResponseEntity.ok(Map.of("summary", emptySummary, "daily", List.of()));
            }

            // 累計數據
            long totalForeign = 0;
            long totalTrust = 0;
            long totalDealer = 0;
            long totalAll = 0;
            String name = dailyData.get(0).getName();

            for (InstitutionalInvestorsData data : dailyData) {
                totalForeign += (data.getForeignInvestorBuySell() != null ? data.getForeignInvestorBuySell() : 0);
                totalTrust += (data.getInvestmentTrustBuySell() != null ? data.getInvestmentTrustBuySell() : 0);
                totalDealer += (data.getDealerBuySell() != null ? data.getDealerBuySell() : 0);
                totalAll += (data.getTotalBuySell() != null ? data.getTotalBuySell() : 0);
            }

            Map<String, Object> summary = new java.util.HashMap<>();
            summary.put("symbol", symbol);
            summary.put("name", name);
            summary.put("foreign", totalForeign);
            summary.put("trust", totalTrust);
            summary.put("dealer", totalDealer);
            summary.put("total", totalAll);
            summary.put("days", dailyData.size());

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("summary", summary);
            response.put("daily", dailyData);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting institutional investors range for {}", symbol, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/batch-institutional-investors")
    public ResponseEntity<List<Map<String, Object>>> getBatchInstitutionalInvestors(@RequestParam String symbols, @RequestParam(defaultValue = "1") int days) {
        List<String> symbolList = java.util.Arrays.stream(symbols.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());

        if (symbolList.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<Map<String, Object>> resultList = new java.util.ArrayList<>();

        if (days == 1) {
            // 使用 StockChipService 批次取得最新數據
            Map<String, InstitutionalInvestorsData> batchData = stockChipService.getLatestInstitutionalInvestors(symbolList);

            for (String symbol : symbolList) {
                InstitutionalInvestorsData data = batchData.get(symbol);
                if (data != null) {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("symbol", symbol);
                    map.put("total", data.getTotalBuySell() != null ? data.getTotalBuySell() : 0);
                    map.put("foreign", data.getForeignInvestorBuySell());
                    map.put("trust", data.getInvestmentTrustBuySell());
                    map.put("dealer", data.getDealerBuySell());
                    resultList.add(map);
                }
            }
        } else {
            // 多日查詢：使用 StockChipService 獲取各檔股票多日數據
            for (String symbol : symbolList) {
                try {
                    List<InstitutionalInvestorsData> data = stockChipService.getRecentDaysInstitutionalInvestors(symbol, days);
                    if (!data.isEmpty()) {
                        long total = data.stream().mapToLong(d -> d.getTotalBuySell() != null ? d.getTotalBuySell() : 0).sum();
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("symbol", symbol);
                        map.put("total", total);
                        map.put("foreign", data.get(0).getForeignInvestorBuySell());
                        map.put("trust", data.get(0).getInvestmentTrustBuySell());
                        map.put("dealer", data.get(0).getDealerBuySell());
                        resultList.add(map);
                    }
                } catch (Exception e) {
                    log.warn("Failed to get institutional data for {}: {}", symbol, e.getMessage());
                }
            }
        }
        return ResponseEntity.ok(resultList);
    }

    @GetMapping("/monitor")
    public ResponseEntity<Map<String, Object>> getStockMonitorData(@RequestParam String symbols, @RequestParam(required = false) String date) {
        List<String> symbolList = java.util.Arrays.stream(symbols.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());

        if (symbolList.isEmpty()) {
            Map<String, Object> emptyResult = new java.util.HashMap<>();
            emptyResult.put("dataDate", null);
            emptyResult.put("stocks", List.of());
            return ResponseEntity.ok(emptyResult);
        }

        // 解析查詢日期，若未指定則查最新
        LocalDate queryDate = null;
        if (date != null && !date.isEmpty()) {
            try {
                queryDate = LocalDate.parse(date);
            } catch (Exception e) {
                log.warn("Invalid date format: {}, falling back to latest", date);
            }
        }

        // 1. 即時報價 (必須呼叫 API) - 僅查詢今天時才取即時報價
        Map<String, com.acenexus.tata.nexusbot.dto.TwseApiResponse.TwseStockData> batchQuotes;
        if (queryDate == null || queryDate.equals(LocalDate.now())) {
            batchQuotes = twseApiClient.getBatchStockQuotes(symbolList);
        } else {
            batchQuotes = Map.of(); // 歷史日期不取即時報價
        }

        // 2. 籌碼數據 (DB First)
        Map<String, InstitutionalInvestorsData> batchChipsData;
        if (queryDate != null) {
            batchChipsData = stockChipService.getInstitutionalInvestorsByDate(symbolList, queryDate);
        } else {
            batchChipsData = stockChipService.getLatestInstitutionalInvestors(symbolList);
        }

        List<Map<String, Object>> resultList = new java.util.ArrayList<>();
        java.time.LocalDate dataDate = null;

        for (String cleanSymbol : symbolList) {
            try {
                String name = cleanSymbol;
                BigDecimal price = null;

                // 從批次報價結果取得
                BigDecimal previousClose = null;
                com.acenexus.tata.nexusbot.dto.TwseApiResponse.TwseStockData quote = batchQuotes.get(cleanSymbol);
                if (quote != null) {
                    if (quote.getName() != null && !quote.getName().isEmpty()) {
                        name = quote.getName();
                    }
                    try {
                        String z = quote.getCurrentPrice();
                        if (z != null && !z.equals("-")) {
                            price = new BigDecimal(z.replace(",", ""));
                        }
                        // 取得昨收價
                        String y = quote.getPreviousClose();
                        if (y != null && !y.equals("-")) {
                            previousClose = new BigDecimal(y.replace(",", ""));
                        }
                    } catch (Exception e) {
                        // ignore parse error
                    }
                }

                // 從批次法人資料結果取得
                long foreign = 0;
                long trust = 0;
                long dealer = 0;

                InstitutionalInvestorsData chipsData = batchChipsData.get(cleanSymbol);
                if (chipsData != null) {
                    foreign = chipsData.getForeignInvestorBuySell() != null ? chipsData.getForeignInvestorBuySell() : 0;
                    trust = chipsData.getInvestmentTrustBuySell() != null ? chipsData.getInvestmentTrustBuySell() : 0;
                    dealer = chipsData.getDealerBuySell() != null ? chipsData.getDealerBuySell() : 0;

                    if (name.equals(cleanSymbol) && chipsData.getName() != null && !chipsData.getName().isEmpty()) {
                        name = chipsData.getName();
                    }
                    if (dataDate == null && chipsData.getDate() != null) {
                        dataDate = chipsData.getDate();
                    }
                }

                // 計算漲跌與漲跌幅
                BigDecimal change = null;
                BigDecimal changePercent = null;
                if (price != null && previousClose != null && previousClose.compareTo(BigDecimal.ZERO) != 0) {
                    change = price.subtract(previousClose);
                    changePercent = change.multiply(new BigDecimal("100")).divide(previousClose, 2, java.math.RoundingMode.HALF_UP);
                }

                Map<String, Object> map = new java.util.HashMap<>();
                map.put("code", cleanSymbol);
                map.put("name", name);
                map.put("price", price);
                map.put("change", change);
                map.put("changePercent", changePercent);
                map.put("foreignBuy", foreign);
                map.put("investmentTrustBuy", trust);
                map.put("dealerBuy", dealer);
                map.put("market", stockSymbolService.getMarketBySymbol(cleanSymbol));

                resultList.add(map);
            } catch (Exception e) {
                log.error("Error processing monitor data for {}", cleanSymbol, e);
            }
        }

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("dataDate", dataDate);
        response.put("stocks", resultList);
        return ResponseEntity.ok(response);
    }

    /**
     * 取得股票 K 線數據與技術指標
     *
     * @param symbol 股票代號
     * @param days   天數（預設 120，約半年）
     * @return K線數據與技術指標
     */
    @GetMapping("/{symbol}/kline")
    public ResponseEntity<Map<String, Object>> getKlineData(@PathVariable String symbol,
                                                            @RequestParam(defaultValue = "120") int days) {

        try {
            String resolvedSymbol = stockSymbolService.resolveSymbol(symbol, StockMarket.TW);

            // 計算查詢日期範圍
            LocalDate endDate = LocalDate.now();
            // 為了計算 MA60，需要多取一些數據
            LocalDate startDate = endDate.minusDays(days + 60);

            // 取得 K 線數據
            List<CandlestickData> candlesticks = finMindApiClient.getStockPriceHistory(
                    resolvedSymbol,
                    startDate.toString(),
                    endDate.toString()
            );

            if (candlesticks.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "symbol", symbol,
                        "name", stockSymbolService.getNameBySymbol(resolvedSymbol),
                        "candles", List.of(),
                        "indicators", Map.of()
                ));
            }

            // 計算技術指標
            List<TechnicalIndicatorData> allIndicators = technicalAnalysisService.calculateAllIndicators(candlesticks);

            // 只回傳最近 N 天的數據
            int dataSize = candlesticks.size();
            int returnSize = Math.min(days, dataSize);
            List<CandlestickData> returnCandles = candlesticks.subList(dataSize - returnSize, dataSize);
            List<TechnicalIndicatorData> returnIndicators = allIndicators.subList(dataSize - returnSize, dataSize);

            // 取得最新的技術指標摘要
            TechnicalIndicatorData latestIndicator = allIndicators.get(allIndicators.size() - 1);

            // 組裝回傳格式
            Map<String, Object> indicatorsSummary = new java.util.HashMap<>();
            indicatorsSummary.put("ma5", extractMAValues(returnIndicators, "ma5"));
            indicatorsSummary.put("ma20", extractMAValues(returnIndicators, "ma20"));
            indicatorsSummary.put("ma60", extractMAValues(returnIndicators, "ma60"));
            indicatorsSummary.put("latestRSI", latestIndicator.getRsi());
            indicatorsSummary.put("ma20Bias", latestIndicator.getMa20Bias());
            indicatorsSummary.put("aboveMA20", latestIndicator.getAboveMA20());

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("symbol", resolvedSymbol);
            response.put("name", stockSymbolService.getNameBySymbol(resolvedSymbol));
            response.put("candles", returnCandles);
            response.put("indicators", indicatorsSummary);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting kline data for {}", symbol, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 提取均線數據用於圖表繪製
     */
    private List<Map<String, Object>> extractMAValues(List<TechnicalIndicatorData> indicators, String maType) {
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (TechnicalIndicatorData indicator : indicators) {
            BigDecimal value = null;
            switch (maType) {
                case "ma5":
                    value = indicator.getMa5();
                    break;
                case "ma20":
                    value = indicator.getMa20();
                    break;
                case "ma60":
                    value = indicator.getMa60();
                    break;
            }
            if (value != null) {
                Map<String, Object> point = new java.util.HashMap<>();
                point.put("time", indicator.getDate().toString());
                point.put("value", value);
                result.add(point);
            }
        }
        return result;
    }

    /**
     * 開始監控股票即時成交
     * POST /api/stock/{symbol}/tick-monitor
     */
    @PostMapping("/{symbol}/tick-monitor")
    public ResponseEntity<Map<String, Object>> startTickMonitor(@PathVariable String symbol) {
        try {
            String resolvedSymbol = stockSymbolService.resolveSymbol(symbol, StockMarket.TW);
            String source = realtimeTickService.startMonitor(resolvedSymbol);

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("symbol", resolvedSymbol);
            response.put("source", source);
            response.put("status", "monitoring");
            response.put("summary", realtimeTickService.getSubscriptionSummary());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error starting tick monitor for {}", symbol, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 停止監控股票即時成交
     * DELETE /api/stock/{symbol}/tick-monitor
     */
    @DeleteMapping("/{symbol}/tick-monitor")
    public ResponseEntity<Map<String, Object>> stopTickMonitor(@PathVariable String symbol) {
        try {
            String resolvedSymbol = stockSymbolService.resolveSymbol(symbol, StockMarket.TW);
            realtimeTickService.stopMonitor(resolvedSymbol);

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("symbol", resolvedSymbol);
            response.put("status", "stopped");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error stopping tick monitor for {}", symbol, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 取得即時成交統計
     * GET /api/stock/{symbol}/ticks/stats
     */
    @GetMapping("/{symbol}/ticks/stats")
    public ResponseEntity<TickStats> getTickStats(@PathVariable String symbol) {
        try {
            String resolvedSymbol = stockSymbolService.resolveSymbol(symbol, StockMarket.TW);
            TickStats stats = realtimeTickService.getRealtimeStats(resolvedSymbol);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting tick stats for {}", symbol, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 取得最近成交明細
     * GET /api/stock/{symbol}/ticks?limit=50
     */
    @GetMapping("/{symbol}/ticks")
    public ResponseEntity<List<TickData>> getRecentTicks(@PathVariable String symbol,
                                                         @RequestParam(defaultValue = "50") int limit) {
        try {
            String resolvedSymbol = stockSymbolService.resolveSymbol(symbol, StockMarket.TW);
            List<TickData> ticks = realtimeTickService.getRecentTicks(resolvedSymbol, limit);
            return ResponseEntity.ok(ticks);
        } catch (Exception e) {
            log.error("Error getting ticks for {}", symbol, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 取得大單成交
     * GET /api/stock/{symbol}/ticks/big?threshold=100
     */
    @GetMapping("/{symbol}/ticks/big")
    public ResponseEntity<List<TickData>> getBigTrades(@PathVariable String symbol,
                                                       @RequestParam(defaultValue = "100") int threshold) {
        try {
            String resolvedSymbol = stockSymbolService.resolveSymbol(symbol, StockMarket.TW);
            List<TickData> bigTrades = realtimeTickService.getBigTrades(resolvedSymbol, threshold);
            return ResponseEntity.ok(bigTrades);
        } catch (Exception e) {
            log.error("Error getting big trades for {}", symbol, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 取得監控狀態
     * GET /api/stock/tick-monitor/status
     */
    @GetMapping("/tick-monitor/status")
    public ResponseEntity<Map<String, Object>> getTickMonitorStatus() {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("summary", realtimeTickService.getSubscriptionSummary());
        response.put("monitors", realtimeTickService.getMonitorStatus());
        return ResponseEntity.ok(response);
    }
}
