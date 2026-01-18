package com.acenexus.tata.nexusbot.controller;

import com.acenexus.tata.nexusbot.client.FinMindApiClient;
import com.acenexus.tata.nexusbot.client.TwseApiClient;
import com.acenexus.tata.nexusbot.dto.InstitutionalInvestorsData;
import com.acenexus.tata.nexusbot.dto.StockSymbolDto;
import com.acenexus.tata.nexusbot.enums.StockMarket;
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

    @GetMapping("/list")
    public ResponseEntity<List<StockSymbolDto>> getStockList() {
        // 從穩定、免 Token 的 TWSE 獲取
        Map<String, String> twseMap = twseApiClient.getTaiwanStockNameToSymbolMap();
        if (!twseMap.isEmpty()) {
            return ResponseEntity.ok(twseMap.entrySet().stream()
                    .map(e -> new StockSymbolDto(e.getValue(), e.getKey())) // Map is <Name, Symbol>, DTO is (symbol, name)
                    .collect(Collectors.toList()));
        }
        return ResponseEntity.ok(stockSymbolService.getAllStocks());
    }

    @GetMapping("/{symbol}/institutional-investors")
    public ResponseEntity<List<InstitutionalInvestorsData>> getInstitutionalInvestors(
            @PathVariable String symbol, @RequestParam(defaultValue = "10") int days) {
        String resolvedSymbol = stockSymbolService.resolveSymbol(symbol, StockMarket.TW);
        try {
            List<InstitutionalInvestorsData> data = twseApiClient.getRecentDaysInstitutionalInvestors(resolvedSymbol, days);
            if (data.isEmpty()) {
                data = finMindApiClient.getRecentDaysInstitutionalInvestors(resolvedSymbol, days);
            }
            return ResponseEntity.ok(data);
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/batch-institutional-investors")
    public ResponseEntity<List<Map<String, Object>>> getBatchInstitutionalInvestors(@RequestParam String symbols,
                                                                                    @RequestParam(defaultValue = "1") int days) {
        List<String> symbolList = java.util.Arrays.stream(symbols.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (symbolList.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<Map<String, Object>> resultList = new java.util.ArrayList<>();

        if (days == 1) {
            // 單日查詢：使用批次方法優化（一次 API 請求）
            Map<String, InstitutionalInvestorsData> batchData = twseApiClient.getBatchInstitutionalInvestors(symbolList);
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
            // 多日查詢：逐一查詢並累加
            for (String symbol : symbolList) {
                try {
                    List<InstitutionalInvestorsData> data = twseApiClient.getRecentDaysInstitutionalInvestors(symbol, days);
                    if (data.isEmpty()) data = finMindApiClient.getRecentDaysInstitutionalInvestors(symbol, days);
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
    public ResponseEntity<Map<String, Object>> getStockMonitorData(@RequestParam String symbols) {
        List<String> symbolList = java.util.Arrays.stream(symbols.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (symbolList.isEmpty()) {
            Map<String, Object> emptyResult = new java.util.HashMap<>();
            emptyResult.put("dataDate", null);
            emptyResult.put("stocks", List.of());
            return ResponseEntity.ok(emptyResult);
        }

        // 批次查詢優化：即時報價 + 法人進出各一次 API 請求
        Map<String, com.acenexus.tata.nexusbot.dto.TwseApiResponse.TwseStockData> batchQuotes =
                twseApiClient.getBatchStockQuotes(symbolList);
        Map<String, InstitutionalInvestorsData> batchChipsData =
                twseApiClient.getBatchInstitutionalInvestors(symbolList);

        List<Map<String, Object>> resultList = new java.util.ArrayList<>();
        java.time.LocalDate dataDate = null;

        for (String cleanSymbol : symbolList) {
            try {
                String name = cleanSymbol;
                BigDecimal price = null;

                // 從批次報價結果取得
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
                    // 取得資料日期（取第一筆有日期的資料）
                    if (dataDate == null && chipsData.getDate() != null) {
                        dataDate = chipsData.getDate();
                    }
                }

                Map<String, Object> map = new java.util.HashMap<>();
                map.put("code", cleanSymbol);
                map.put("name", name);
                map.put("price", price);
                map.put("foreignBuy", foreign);
                map.put("investmentTrustBuy", trust);
                map.put("dealerBuy", dealer);

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
