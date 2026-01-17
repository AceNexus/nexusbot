package com.acenexus.tata.nexusbot.client;

import com.acenexus.tata.nexusbot.dto.CandlestickData;
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
import java.math.RoundingMode;
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
     * 查詢台股即時報價
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
     * 取得個股月 K 線數據
     * API: https://www.twse.com.tw/exchangeReport/STOCK_DAY
     */
    public List<CandlestickData> getStockMonthlyKLine(String stockId, int year, int month) {
        try {
            String date = String.format("%d%02d01", year, month);
            String url = String.format("%s/exchangeReport/STOCK_DAY?response=json&date=%s&stockNo=%s",
                    TWSE_BASE_URL, date, stockId);

            log.debug("TWSE K-line request - stockId={}, date={}", stockId, date);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            if (!"OK".equals(root.path("stat").asText())) {
                return List.of();
            }

            // 從 title 提取股票名稱
            String title = root.path("title").asText();
            String stockName = extractStockName(title);

            List<CandlestickData> result = new ArrayList<>();
            JsonNode dataArray = root.path("data");

            for (JsonNode row : dataArray) {
                try {
                    // 格式: ["114/01/02","45,045,125","47,883,206,644","1,070.00","1,075.00","1,055.00","1,065.00","-10.00","74,997",""]
                    String rocDateStr = row.get(0).asText(); // 114/01/02
                    LocalDate localDate = parseRocDate(rocDateStr);

                    CandlestickData candle = CandlestickData.builder()
                            .symbol(stockId)
                            .name(stockName)
                            .date(localDate)
                            .volume(parseLong(row.get(1).asText()) / 1000) // 股數轉張數
                            .turnover(parseBigDecimal(row.get(2).asText()))
                            .open(parseBigDecimal(row.get(3).asText()))
                            .high(parseBigDecimal(row.get(4).asText()))
                            .low(parseBigDecimal(row.get(5).asText()))
                            .close(parseBigDecimal(row.get(6).asText()))
                            .change(parseBigDecimal(row.get(7).asText()))
                            .build();

                    // 計算漲跌幅
                    if (candle.getClose() != null && candle.getChange() != null) {
                        BigDecimal prevClose = candle.getClose().subtract(candle.getChange());
                        if (prevClose.compareTo(BigDecimal.ZERO) != 0) {
                            BigDecimal changePercent = candle.getChange()
                                    .divide(prevClose, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100))
                                    .setScale(2, RoundingMode.HALF_UP);
                            candle.setChangePercent(changePercent);
                        }
                    }

                    result.add(candle);
                } catch (Exception e) {
                    log.warn("Failed to parse K-line row: {}", e.getMessage());
                }
            }

            return result;

        } catch (Exception e) {
            log.error("TWSE K-line error - stockId={}, error={}", stockId, e.getMessage());
            return List.of();
        }
    }

    /**
     * 取得三大法人買賣超
     * API: https://www.twse.com.tw/fund/T86
     */
    @Cacheable(value = "twseInstitutional", key = "#stockId + '_' + #date")
    public Optional<InstitutionalInvestorsData> getInstitutionalInvestors(String stockId, LocalDate date) {
        try {
            String dateStr = date.format(DATE_FORMATTER);
            String url = String.format("%s/fund/T86?response=json&date=%s&selectType=ALL", TWSE_BASE_URL, dateStr);

            log.debug("TWSE institutional request - date={}", dateStr);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            if (!"OK".equals(root.path("stat").asText())) {
                return Optional.empty();
            }

            JsonNode dataArray = root.path("data");
            for (JsonNode row : dataArray) {
                String symbol = row.get(0).asText().trim();
                if (symbol.equals(stockId)) {
                    // 欄位: 證券代號,證券名稱,外陸資買進,外陸資賣出,外陸資買賣超,...,三大法人買賣超
                    Long foreignBuy = parseLong(row.get(2).asText()) / 1000;
                    Long foreignSell = parseLong(row.get(3).asText()) / 1000;
                    Long foreignNet = parseLong(row.get(4).asText()) / 1000;
                    Long trustBuy = parseLong(row.get(8).asText()) / 1000;
                    Long trustSell = parseLong(row.get(9).asText()) / 1000;
                    Long trustNet = parseLong(row.get(10).asText()) / 1000;
                    Long dealerNet = parseLong(row.get(11).asText()) / 1000;
                    Long totalNet = parseLong(row.get(18).asText()) / 1000;

                    return Optional.of(InstitutionalInvestorsData.builder()
                            .symbol(stockId)
                            .date(date)
                            .foreignInvestorBuy(foreignBuy)
                            .foreignInvestorSell(foreignSell)
                            .foreignInvestorBuySell(foreignNet)
                            .investmentTrustBuy(trustBuy)
                            .investmentTrustSell(trustSell)
                            .investmentTrustBuySell(trustNet)
                            .dealerBuySell(dealerNet)
                            .totalBuySell(totalNet)
                            .build());
                }
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("TWSE institutional error - stockId={}, error={}", stockId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 取得最近 N 天的法人進出
     */
    public List<InstitutionalInvestorsData> getRecentDaysInstitutionalInvestors(String stockId, int days) {
        List<InstitutionalInvestorsData> result = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (int i = 0; i < days && result.size() < days; i++) {
            LocalDate targetDate = now.minusDays(i);
            // 跳過週末
            if (targetDate.getDayOfWeek().getValue() >= 6) continue;

            Optional<InstitutionalInvestorsData> data = getInstitutionalInvestors(stockId, targetDate);
            data.ifPresent(result::add);

            // 避免請求過快
            if (i < days - 1) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return result;
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
