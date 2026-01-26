package com.acenexus.tata.nexusbot.controller;

import com.acenexus.tata.nexusbot.client.FinMindApiClient;
import com.acenexus.tata.nexusbot.client.TwseApiClient;
import com.acenexus.tata.nexusbot.dto.InstitutionalInvestorsData;
import com.acenexus.tata.nexusbot.dto.StockSymbolDto;
import com.acenexus.tata.nexusbot.enums.StockMarket;
import com.acenexus.tata.nexusbot.service.StockChipService;
import com.acenexus.tata.nexusbot.service.StockSymbolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}
