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
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<List<Map<String, Object>>> getBatchInstitutionalInvestors(
            @RequestParam String symbols, @RequestParam(defaultValue = "1") int days) {
        String[] symbolArray = symbols.split(",");
        List<Map<String, Object>> resultList = new java.util.ArrayList<>();
        for (String symbol : symbolArray) {
            String cleanSymbol = symbol.trim();
            try {
                List<InstitutionalInvestorsData> data = twseApiClient.getRecentDaysInstitutionalInvestors(cleanSymbol, days);
                if (data.isEmpty()) data = finMindApiClient.getRecentDaysInstitutionalInvestors(cleanSymbol, days);
                if (!data.isEmpty()) {
                    long total = data.stream().mapToLong(d -> d.getTotalBuySell() != null ? d.getTotalBuySell() : 0).sum();
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("symbol", cleanSymbol);
                    map.put("total", total);
                    map.put("foreign", data.get(0).getForeignInvestorBuySell());
                    map.put("trust", data.get(0).getInvestmentTrustBuySell());
                    map.put("dealer", data.get(0).getDealerBuySell());
                    resultList.add(map);
                }
            } catch (Exception e) {}
        }
        return ResponseEntity.ok(resultList);
    }

    @GetMapping("/monitor")
    public ResponseEntity<List<Map<String, Object>>> getStockMonitorData(@RequestParam String symbols) {
        String[] symbolArray = symbols.split(",");
        List<Map<String, Object>> resultList = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        java.util.Arrays.stream(symbolArray).parallel().forEach(symbol -> {
             String cleanSymbol = symbol.trim();
             if (cleanSymbol.isEmpty()) return;

             try {
                 String name = "";
                 BigDecimal price = null;
                 java.util.Optional<com.acenexus.tata.nexusbot.dto.TwseApiResponse.TwseStockData> quoteOpt = twseApiClient.getStockQuote(cleanSymbol);

                 if (quoteOpt.isPresent()) {
                     com.acenexus.tata.nexusbot.dto.TwseApiResponse.TwseStockData quote = quoteOpt.get();
                     name = quote.getName();
                     try {
                        String z = quote.getCurrentPrice();
                        if (z != null && !z.equals("-")) {
                             price = new BigDecimal(z.replace(",", ""));
                        }
                     } catch (Exception e) {}
                 }

                 if (name == null || name.isEmpty()) {
                      name = cleanSymbol;
                 }

                 List<InstitutionalInvestorsData> chips = twseApiClient.getRecentDaysInstitutionalInvestors(cleanSymbol, 1);
                 if (chips.isEmpty()) chips = finMindApiClient.getRecentDaysInstitutionalInvestors(cleanSymbol, 1);

                 long foreign = 0;
                 long trust = 0;
                 long dealer = 0;

                 if (!chips.isEmpty()) {
                     InstitutionalInvestorsData dayData = chips.get(0);
                     foreign = dayData.getForeignInvestorBuySell() != null ? dayData.getForeignInvestorBuySell() : 0;
                     trust = dayData.getInvestmentTrustBuySell() != null ? dayData.getInvestmentTrustBuySell() : 0;
                     dealer = dayData.getDealerBuySell() != null ? dayData.getDealerBuySell() : 0;
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
                 log.error("Error fetching monitor data for {}", cleanSymbol, e);
             }
        });

        return ResponseEntity.ok(resultList);
    }
}
