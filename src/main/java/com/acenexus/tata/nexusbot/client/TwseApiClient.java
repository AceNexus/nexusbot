package com.acenexus.tata.nexusbot.client;

import com.acenexus.tata.nexusbot.dto.InstitutionalInvestorsData;
import com.acenexus.tata.nexusbot.dto.TwseApiResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 台灣證交所 API 客戶端
 * 官方免費 API，無需 Token
 * 文件：https://www.twse.com.tw/zh/page/trading/exchange/STOCK_DAY.html
 */
@Slf4j
@Component
public class TwseApiClient {

    private static final String TWSE_BASE_URL = "https://www.twse.com.tw";
    private static final String TWSE_REALTIME_URL = "https://mis.twse.com.tw/stock/api/getStockInfo.jsp";
    private static final int TIMEOUT_MS = 15000;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter ROC_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public TwseApiClient(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);

        this.restTemplate = restTemplateBuilder
                .requestFactory(() -> factory)
                .defaultHeader("User-Agent", USER_AGENT)
                .defaultHeader("Accept", "application/json")
                .build();

        this.objectMapper = objectMapper;
    }

    /**
     * 查詢台股即時報價（單一股票）
     */
    public Optional<TwseApiResponse.TwseStockData> getStockQuote(String symbol) {
        try {
            String url = String.format("%s?ex_ch=tse_%s.tw", TWSE_REALTIME_URL, symbol);
            log.debug("TWSE realtime API request - symbol={}", symbol);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode() != HttpStatus.OK) {
                return Optional.empty();
            }

            TwseApiResponse apiResponse = objectMapper.readValue(response.getBody(), TwseApiResponse.class);
            if (!apiResponse.isSuccess() || apiResponse.getStockDataList().isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(apiResponse.getStockDataList().get(0));
        } catch (Exception e) {
            log.error("TWSE realtime API error - symbol={}, error={}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 批次查詢台股即時報價（多檔股票一次請求）
     * TWSE API 支援用 | 分隔多個股票代號
     */
    public Map<String, TwseApiResponse.TwseStockData> getBatchStockQuotes(List<String> symbols) {
        Map<String, TwseApiResponse.TwseStockData> resultMap = new HashMap<>();

        if (symbols == null || symbols.isEmpty()) {
            return resultMap;
        }

        try {
            // 組合批次查詢參數：tse_2330.tw|tse_3037.tw|tse_8021.tw
            String exCh = symbols.stream()
                    .map(s -> "tse_" + s.trim() + ".tw")
                    .reduce((a, b) -> a + "|" + b)
                    .orElse("");

            String url = String.format("%s?ex_ch=%s", TWSE_REALTIME_URL, exCh);
            log.info("TWSE realtime batch request - symbols={}", symbols.size());

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode() != HttpStatus.OK) {
                log.warn("TWSE batch quote API returned non-OK status");
                return resultMap;
            }

            TwseApiResponse apiResponse = objectMapper.readValue(response.getBody(), TwseApiResponse.class);
            if (!apiResponse.isSuccess()) {
                log.warn("TWSE batch quote API response not successful");
                return resultMap;
            }

            // 將結果轉為 Map<symbol, data>
            for (TwseApiResponse.TwseStockData stockData : apiResponse.getStockDataList()) {
                if (stockData.getSymbol() != null) {
                    resultMap.put(stockData.getSymbol(), stockData);
                }
            }

            log.info("TWSE realtime batch loaded - count={}", resultMap.size());

        } catch (Exception e) {
            log.error("TWSE realtime batch API error - error={}", e.getMessage());
        }

        return resultMap;
    }

    /**
     * 取得股票清單（從本益比表）
     * API: https://www.twse.com.tw/exchangeReport/BWIBBU_ALL
     */
    @Cacheable(value = "twseStockList", key = "'all'")
    public Map<String, String> getTaiwanStockNameToSymbolMap() {
        try {
            String date = LocalDate.now().format(DATE_FORMATTER);
            String url = String.format("%s/exchangeReport/BWIBBU_ALL?response=json&date=%s", TWSE_BASE_URL, date);

            log.info("TWSE stock list request - url={}", url);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                return Map.of();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            if (!"OK".equals(root.path("stat").asText())) {
                log.warn("TWSE stock list API error - stat={}", root.path("stat").asText());
                return Map.of();
            }

            Map<String, String> nameToSymbolMap = new HashMap<>();
            JsonNode dataArray = root.path("data");
            for (JsonNode row : dataArray) {
                String symbol = row.get(0).asText().trim();
                String name = row.get(1).asText().trim();
                nameToSymbolMap.put(name, symbol);
            }

            log.info("TWSE stock list loaded - count={}", nameToSymbolMap.size());
            return nameToSymbolMap;

        } catch (Exception e) {
            log.error("TWSE stock list error - error={}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * 取得上櫃股票清單（從櫃買中心 OpenAPI）
     * API: https://www.tpex.org.tw/openapi/v1/tpex_mainboard_quotes
     */
    @Cacheable(value = "tpexStockList", key = "'all'")
    public Map<String, String> getTpexStockNameToSymbolMap() {
        try {
            String url = "https://www.tpex.org.tw/openapi/v1/tpex_mainboard_quotes";

            log.info("TPEx stock list request (OpenAPI)");
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                return Map.of();
            }

            JsonNode root = objectMapper.readTree(response.getBody());

            if (!root.isArray()) {
                log.warn("TPEx stock list API - unexpected format");
                return Map.of();
            }

            Map<String, String> nameToSymbolMap = new HashMap<>();
            for (JsonNode row : root) {
                // 格式: {"SecuritiesCompanyCode": "8042", "CompanyName": "金山電", ...}
                String symbol = row.path("SecuritiesCompanyCode").asText().trim();
                String name = row.path("CompanyName").asText().trim();
                // 過濾：只取 4-6 位數字的股票代號（排除 ETF、權證等）
                if (symbol.matches("\\d{4,6}") && !symbol.startsWith("00")) {
                    nameToSymbolMap.put(name, symbol);
                }
            }

            log.info("TPEx stock list loaded - count={}", nameToSymbolMap.size());
            return nameToSymbolMap;

        } catch (Exception e) {
            log.error("TPEx stock list error - error={}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * 取得指定日期「全市場」三大法人買賣超
     * 一次 API 請求取得所有股票，快取後供多次查詢使用
     */
    @Cacheable(value = "twseInstitutionalAll", key = "#date.toString()")
    public Map<String, InstitutionalInvestorsData> getAllInstitutionalInvestorsByDate(LocalDate date) {
        Map<String, InstitutionalInvestorsData> resultMap = new HashMap<>();

        try {
            String dateStr = date.format(DATE_FORMATTER);
            String url = String.format("%s/fund/T86?response=json&date=%s&selectType=ALL", TWSE_BASE_URL, dateStr);

            log.info("TWSE institutional ALL request - date={}", dateStr);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.warn("TWSE API returned non-OK status for date={}", dateStr);
                return resultMap;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            String stat = root.path("stat").asText();

            // 處理非交易日情況
            if (!"OK".equals(stat)) {
                log.info("No trading data for date={}, stat={}", dateStr, stat);
                return resultMap;
            }

            JsonNode dataArray = root.path("data");
            for (JsonNode row : dataArray) {
                try {
                    String symbol = row.get(0).asText().trim();
                    String name = row.get(1).asText().trim();

                    // 解析欄位（單位：股 → 張，除以 1000）
                    Long foreignBuy = parseLong(row.get(2).asText()) / 1000;
                    Long foreignSell = parseLong(row.get(3).asText()) / 1000;
                    Long foreignNet = parseLong(row.get(4).asText()) / 1000;
                    Long trustBuy = parseLong(row.get(8).asText()) / 1000;
                    Long trustSell = parseLong(row.get(9).asText()) / 1000;
                    Long trustNet = parseLong(row.get(10).asText()) / 1000;
                    Long dealerNet = parseLong(row.get(11).asText()) / 1000;
                    Long totalNet = parseLong(row.get(18).asText()) / 1000;

                    InstitutionalInvestorsData data = InstitutionalInvestorsData.builder()
                            .symbol(symbol)
                            .name(name)
                            .date(date)
                            .foreignInvestorBuy(foreignBuy)
                            .foreignInvestorSell(foreignSell)
                            .foreignInvestorBuySell(foreignNet)
                            .investmentTrustBuy(trustBuy)
                            .investmentTrustSell(trustSell)
                            .investmentTrustBuySell(trustNet)
                            .dealerBuySell(dealerNet)
                            .totalBuySell(totalNet)
                            .build();

                    resultMap.put(symbol, data);
                } catch (Exception e) {
                    log.warn("Failed to parse row: {}", e.getMessage());
                }
            }

            log.info("TWSE institutional ALL loaded - date={}, count={}", dateStr, resultMap.size());

        } catch (Exception e) {
            log.error("TWSE institutional ALL error - date={}, error={}", date, e.getMessage());
        }

        return resultMap;
    }

    /**
     * 取得最近 N 天的法人進出（優化版）
     * 使用全市場快取，避免重複 API 請求
     */
    public List<InstitutionalInvestorsData> getRecentDaysInstitutionalInvestors(String stockId, int days) {
        List<InstitutionalInvestorsData> result = new ArrayList<>();
        LocalDate now = LocalDate.now();

        // 修復：增加搜尋範圍，確保能跨越週末找到足夠的交易日
        // 最多往前找 days + 10 天（含週末和可能的假日）
        int maxSearchDays = days + 10;
        int tradingDaysFound = 0;

        for (int i = 0; i < maxSearchDays && tradingDaysFound < days; i++) {
            LocalDate targetDate = now.minusDays(i);

            // 跳過週末（週六=6, 週日=7）
            int dayOfWeek = targetDate.getDayOfWeek().getValue();
            if (dayOfWeek >= 6) {
                continue;
            }

            // 使用全市場快取查詢（效能優化）
            Map<String, InstitutionalInvestorsData> allData = getAllInstitutionalInvestorsByDate(targetDate);

            if (allData.isEmpty()) {
                // 該日無資料（可能是假日），繼續往前找
                log.debug("No data for date={}, skipping", targetDate);
                continue;
            }

            // 從快取中取得指定股票
            InstitutionalInvestorsData stockData = allData.get(stockId);
            if (stockData != null) {
                result.add(stockData);
                tradingDaysFound++;
            }
        }

        return result;
    }

    /**
     * 批次取得多檔股票的最新法人進出（用於籌碼看板）
     * 一次 API 取得全市場，再篩選需要的股票
     */
    public Map<String, InstitutionalInvestorsData> getBatchInstitutionalInvestors(List<String> symbols) {
        Map<String, InstitutionalInvestorsData> result = new HashMap<>();
        LocalDate now = LocalDate.now();

        // 往前找最近的交易日
        for (int i = 0; i < 10; i++) {
            LocalDate targetDate = now.minusDays(i);

            if (targetDate.getDayOfWeek().getValue() >= 6) {
                continue;
            }

            Map<String, InstitutionalInvestorsData> allData = getAllInstitutionalInvestorsByDate(targetDate);

            if (!allData.isEmpty()) {
                // 找到交易日，篩選需要的股票
                for (String symbol : symbols) {
                    InstitutionalInvestorsData data = allData.get(symbol);
                    if (data != null) {
                        result.put(symbol, data);
                    }
                }
                break; // 找到資料就結束
            }
        }

        return result;
    }

    // === TPEx (上櫃) 法人進出 ===

    /**
     * 取得上櫃股票全市場法人進出
     * API: https://www.tpex.org.tw/web/stock/3insti/daily_trade/3itrade_hedge_result.php
     */
    @Cacheable(value = "tpexInstitutionalAll", key = "'latest'")
    public Map<String, InstitutionalInvestorsData> getTpexAllInstitutionalInvestors() {
        Map<String, InstitutionalInvestorsData> resultMap = new HashMap<>();

        try {
            String url = "https://www.tpex.org.tw/web/stock/3insti/daily_trade/3itrade_hedge_result.php?l=zh-tw&o=json&se=AL&t=D";

            log.info("TPEx institutional ALL request");
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.warn("TPEx institutional API returned non-OK status");
                return resultMap;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode tables = root.path("tables");
            if (!tables.isArray() || tables.isEmpty()) {
                log.warn("TPEx institutional API - no tables found");
                return resultMap;
            }

            JsonNode dataArray = tables.get(0).path("data");
            String dateStr = tables.get(0).path("date").asText(); // "115/01/16"
            LocalDate dataDate = null;
            if (dateStr != null && dateStr.contains("/")) {
                dataDate = parseRocDate(dateStr);
            }

            for (JsonNode row : dataArray) {
                try {
                    String symbol = row.get(0).asText().trim();
                    String name = row.get(1).asText().trim();

                    // 欄位說明 (單位: 股)
                    // 2-4: 外資(不含自營), 5-7: 外資自營, 8-10: 外資合計
                    // 11-13: 投信, 14-16: 自營商避險, 17-19: 自營商, 20-22: 自營商合計
                    // 23: 三大法人合計
                    Long foreignNet = parseLong(row.get(4).asText()) / 1000;  // 外資買賣超 -> 張
                    Long trustNet = parseLong(row.get(13).asText()) / 1000;   // 投信買賣超 -> 張
                    Long dealerNet = parseLong(row.get(22).asText()) / 1000;  // 自營商合計買賣超 -> 張
                    Long totalNet = parseLong(row.get(23).asText()) / 1000;   // 三大法人合計 -> 張

                    InstitutionalInvestorsData data = InstitutionalInvestorsData.builder()
                            .symbol(symbol)
                            .name(name)
                            .date(dataDate)
                            .foreignInvestorBuySell(foreignNet)
                            .investmentTrustBuySell(trustNet)
                            .dealerBuySell(dealerNet)
                            .totalBuySell(totalNet)
                            .build();

                    resultMap.put(symbol, data);
                } catch (Exception e) {
                    log.warn("Failed to parse TPEx row: {}", e.getMessage());
                }
            }

            log.info("TPEx institutional ALL loaded - count={}", resultMap.size());

        } catch (Exception e) {
            log.error("TPEx institutional ALL error - error={}", e.getMessage());
        }

        return resultMap;
    }

    /**
     * 批次取得上櫃股票法人進出
     */
    public Map<String, InstitutionalInvestorsData> getBatchTpexInstitutionalInvestors(List<String> symbols) {
        Map<String, InstitutionalInvestorsData> result = new HashMap<>();
        Map<String, InstitutionalInvestorsData> allData = getTpexAllInstitutionalInvestors();

        for (String symbol : symbols) {
            InstitutionalInvestorsData data = allData.get(symbol);
            if (data != null) {
                result.put(symbol, data);
            }
        }

        return result;
    }

    /**
     * 取得指定日期「上櫃」全市場三大法人買賣超
     * API: https://www.tpex.org.tw/web/stock/3insti/daily_trade/3itrade_hedge_result.php
     */
    @Cacheable(value = "tpexInstitutionalAll", key = "#date.toString()")
    public Map<String, InstitutionalInvestorsData> getTpexAllInstitutionalInvestorsByDate(LocalDate date) {
        Map<String, InstitutionalInvestorsData> resultMap = new HashMap<>();

        try {
            // 民國年轉換: 2025-01-16 -> 114/01/16
            String rocDate = String.format("%d/%02d/%02d", date.getYear() - 1911, date.getMonthValue(), date.getDayOfMonth());
            String url = String.format("https://www.tpex.org.tw/web/stock/3insti/daily_trade/3itrade_hedge_result.php?l=zh-tw&o=json&se=AL&t=D&d=%s", rocDate);

            log.info("TPEx institutional ALL request - date={}", rocDate);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.warn("TPEx institutional API returned non-OK status for date={}", rocDate);
                return resultMap;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode tables = root.path("tables");
            
            // 檢查是否有資料 (假日或非交易日)
            if (!tables.isArray() || tables.isEmpty()) {
                log.info("No TPEx trading data for date={}, tables empty", rocDate);
                return resultMap;
            }

            JsonNode dataArray = tables.get(0).path("data");
            
            // 雙重檢查：確認回傳日期是否與請求日期一致 (TPEx 有時會回傳最新日期)
            String respDateStr = tables.get(0).path("date").asText(); 
            if (respDateStr != null && respDateStr.contains("/")) {
                 LocalDate respDate = parseRocDate(respDateStr);
                 if (!respDate.equals(date)) {
                     log.warn("TPEx API returned date {} but requested {}, skipping", respDate, date);
                     return resultMap;
                 }
            }

            for (JsonNode row : dataArray) {
                try {
                    String symbol = row.get(0).asText().trim();
                    String name = row.get(1).asText().trim();

                    // 欄位說明 (單位: 股)
                    // 2-4: 外資(不含自營), 5-7: 外資自營, 8-10: 外資合計
                    // 11-13: 投信, 14-16: 自營商避險, 17-19: 自營商, 20-22: 自營商合計
                    // 23: 三大法人合計
                    Long foreignNet = parseLong(row.get(4).asText()) / 1000;
                    Long trustNet = parseLong(row.get(13).asText()) / 1000;
                    Long dealerNet = parseLong(row.get(22).asText()) / 1000;
                    Long totalNet = parseLong(row.get(23).asText()) / 1000;

                    InstitutionalInvestorsData data = InstitutionalInvestorsData.builder()
                            .symbol(symbol)
                            .name(name)
                            .date(date)
                            .foreignInvestorBuySell(foreignNet)
                            .investmentTrustBuySell(trustNet)
                            .dealerBuySell(dealerNet)
                            .totalBuySell(totalNet)
                            .build();

                    resultMap.put(symbol, data);
                } catch (Exception e) {
                    log.warn("Failed to parse TPEx row: {}", e.getMessage());
                }
            }

            log.info("TPEx institutional ALL loaded - date={}, count={}", rocDate, resultMap.size());

        } catch (Exception e) {
            log.error("TPEx institutional ALL error - date={}, error={}", date, e.getMessage());
        }

        return resultMap;
    }

    // === 輔助方法 ===

    private LocalDate parseRocDate(String rocDateStr) {
        // 114/01/02 -> 2025/01/02
        String[] parts = rocDateStr.split("/");
        int year = Integer.parseInt(parts[0]) + 1911;
        int month = Integer.parseInt(parts[1]);
        int day = Integer.parseInt(parts[2]);
        return LocalDate.of(year, month, day);
    }

    private String extractStockName(String title) {
        // "114年01月 2330 台積電 各日成交資訊" -> "台積電"
        if (title == null) return "";
        String[] parts = title.split("\\s+");
        if (parts.length >= 3) {
            return parts[2].trim();
        }
        return "";
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty() || "--".equals(value.trim())) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", "").replace("+", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLong(String value) {
        if (value == null || value.trim().isEmpty() || "--".equals(value.trim())) {
            return 0L;
        }
        try {
            return Long.parseLong(value.replace(",", "").replace("+", "").trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
