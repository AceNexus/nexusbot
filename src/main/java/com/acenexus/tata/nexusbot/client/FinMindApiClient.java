package com.acenexus.tata.nexusbot.client;

import com.acenexus.tata.nexusbot.dto.CandlestickData;
import com.acenexus.tata.nexusbot.dto.FinMindStockPriceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FinMind API 客戶端
 * 台灣開源金融資料庫
 * 文件：https://finmind.github.io/
 */
@Slf4j
@Component
public class FinMindApiClient {

    private static final String BASE_URL = "https://api.finmindtrade.com/api/v4/data";
    private static final int TIMEOUT_MS = 10000;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public FinMindApiClient(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);

        this.restTemplate = restTemplateBuilder
                .requestFactory(() -> factory)
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * 取得股票K線數據
     *
     * @param stockId   股票代號
     * @param startDate 開始日期 (YYYY-MM-DD)
     * @param endDate   結束日期 (YYYY-MM-DD)，可為 null 表示至今
     * @return K線數據列表
     */
    @Cacheable(value = "stockFinancialData", key = "#stockId + '_kline_' + #startDate + '_' + #endDate")
    public List<CandlestickData> getStockPriceHistory(String stockId, String startDate, String endDate) {
        try {
            String url = String.format("%s?dataset=TaiwanStockPrice&data_id=%s&start_date=%s", BASE_URL, stockId, startDate);

            if (endDate != null && !endDate.isEmpty()) {
                url += "&end_date=" + endDate;
            }

            log.debug("FinMind K-line query - stockId={}, startDate={}, endDate={}", stockId, startDate, endDate);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("FinMind K-line query failed - stockId={}, status={}", stockId, response.getStatusCode());
                return List.of();
            }

            FinMindStockPriceResponse apiResponse = objectMapper.readValue(
                    response.getBody(), FinMindStockPriceResponse.class
            );

            if (apiResponse.getStatus() != 200 || apiResponse.getData() == null) {
                log.warn("FinMind K-line API error - stockId={}, message={}", stockId, apiResponse.getMessage());
                return List.of();
            }

            List<CandlestickData> candlesticks = convertToCandlestickData(apiResponse.getData());
            log.info("FinMind K-line data retrieved - stockId={}, count={}", stockId, candlesticks.size());

            return candlesticks;

        } catch (RestClientException e) {
            log.error("FinMind K-line network error - stockId={}, error={}", stockId, e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("FinMind K-line parse error - stockId={}, error={}", stockId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 取得最近 N 個月的K線數據
     *
     * @param stockId 股票代號
     * @param months  月數
     * @return K線數據列表
     */
    public List<CandlestickData> getRecentMonthsKLine(String stockId, int months) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months);

        return getStockPriceHistory(
                stockId,
                startDate.format(DATE_FORMATTER),
                endDate.format(DATE_FORMATTER)
        );
    }

    /**
     * 轉換 FinMind 數據為 K線數據格式
     */
    private List<CandlestickData> convertToCandlestickData(List<FinMindStockPriceResponse.StockPriceData> data) {
        return data.stream()
                .map(item -> {
                    BigDecimal open = parseBigDecimal(item.getOpen());
                    BigDecimal high = parseBigDecimal(item.getHigh());
                    BigDecimal low = parseBigDecimal(item.getLow());
                    BigDecimal close = parseBigDecimal(item.getClose());
                    BigDecimal spread = parseBigDecimal(item.getSpread());

                    // 計算漲跌幅
                    BigDecimal changePercent = null;
                    if (close != null && spread != null && close.subtract(spread).compareTo(BigDecimal.ZERO) != 0) {
                        BigDecimal previousClose = close.subtract(spread);
                        changePercent = spread.divide(previousClose, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2, RoundingMode.HALF_UP);
                    }

                    // 成交量（股數轉張數：除以1000）
                    Long volume = parseLong(item.getTradingVolume());
                    if (volume != null) {
                        volume = volume / 1000;  // 轉換為張數
                    }

                    // 成交金額（元轉千元）
                    BigDecimal turnover = parseBigDecimal(item.getTradingMoney());
                    if (turnover != null) {
                        turnover = turnover.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
                    }

                    return CandlestickData.builder()
                            .symbol(item.getStockId())
                            .date(LocalDate.parse(item.getDate(), DATE_FORMATTER))
                            .open(open)
                            .high(high)
                            .low(low)
                            .close(close)
                            .volume(volume)
                            .turnover(turnover)
                            .change(spread)
                            .changePercent(changePercent)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 解析 BigDecimal
     */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse decimal: {}", value);
            return null;
        }
    }

    /**
     * 解析 Long
     */
    private Long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse long: {}", value);
            return null;
        }
    }
}
