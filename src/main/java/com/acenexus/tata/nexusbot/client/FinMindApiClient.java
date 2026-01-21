package com.acenexus.tata.nexusbot.client;

import com.acenexus.tata.nexusbot.dto.CandlestickData;
import com.acenexus.tata.nexusbot.dto.FinMindInstitutionalInvestorsResponse;
import com.acenexus.tata.nexusbot.dto.FinMindStockPriceResponse;
import com.acenexus.tata.nexusbot.dto.InstitutionalInvestorsData;
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
import java.util.Map;
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

    @org.springframework.beans.factory.annotation.Value("${finmind.api-token:}")
    private String apiToken;

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

                    // 成交量（單位：股）
                    Long volume = parseLong(item.getTradingVolume());

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
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))  // 按日期排序（從舊到新）
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

    /**
     * 取得法人進出數據
     * API 文件：https://finmind.github.io/tutor/TaiwanMarket/Institutional/#taiwanstockinstitutionalinvestorsbuysell
     *
     * @param stockId   股票代號
     * @param startDate 開始日期 (YYYY-MM-DD)
     * @param endDate   結束日期 (YYYY-MM-DD)
     * @return 法人進出數據列表
     */
    @Cacheable(value = "stockFinancialData", key = "#stockId + '_institutional_' + #startDate + '_' + #endDate")
    public List<InstitutionalInvestorsData> getInstitutionalInvestors(String stockId, String startDate, String endDate) {
        try {
            String url = String.format("%s?dataset=TaiwanStockInstitutionalInvestorsBuySell&data_id=%s&start_date=%s",
                    BASE_URL, stockId, startDate);

            if (endDate != null && !endDate.isEmpty()) {
                url += "&end_date=" + endDate;
            }

            log.debug("FinMind Institutional Investors query - stockId={}, startDate={}, endDate={}",
                    stockId, startDate, endDate);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("FinMind Institutional Investors query failed - stockId={}, status={}",
                        stockId, response.getStatusCode());
                return List.of();
            }

            FinMindInstitutionalInvestorsResponse apiResponse = objectMapper.readValue(
                    response.getBody(), FinMindInstitutionalInvestorsResponse.class
            );

            if (apiResponse.getStatus() != 200 || apiResponse.getData() == null) {
                log.warn("FinMind Institutional Investors API error - stockId={}, message={}",
                        stockId, apiResponse.getMessage());
                return List.of();
            }

            List<InstitutionalInvestorsData> result = convertToInstitutionalInvestorsData(apiResponse.getData());
            log.info("FinMind Institutional Investors data retrieved - stockId={}, count={}",
                    stockId, result.size());

            return result;

        } catch (RestClientException e) {
            log.error("FinMind Institutional Investors network error - stockId={}, error={}",
                    stockId, e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("FinMind Institutional Investors parse error - stockId={}, error={}",
                    stockId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 取得最近 N 天的法人進出數據
     *
     * @param stockId 股票代號
     * @param days    天數
     * @return 法人進出數據列表
     */
    public List<InstitutionalInvestorsData> getRecentDaysInstitutionalInvestors(String stockId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        return getInstitutionalInvestors(
                stockId,
                startDate.format(DATE_FORMATTER),
                endDate.format(DATE_FORMATTER)
        );
    }

    /**
     * 轉換 FinMind 法人進出數據為內部格式
     * <p>
     * FinMind 的資料格式：每天會有多筆記錄，每筆記錄代表不同的法人類型
     * 我們需要將同一天的記錄合併成單筆記錄
     */
    private List<InstitutionalInvestorsData> convertToInstitutionalInvestorsData(
            List<FinMindInstitutionalInvestorsResponse.InstitutionalInvestorData> data) {

        // 按日期分組
        Map<String, List<FinMindInstitutionalInvestorsResponse.InstitutionalInvestorData>> groupedByDate =
                data.stream().collect(Collectors.groupingBy(
                        FinMindInstitutionalInvestorsResponse.InstitutionalInvestorData::getDate
                ));

        // 處理每個日期的資料
        return groupedByDate.entrySet().stream()
                .map(entry -> {
                    String date = entry.getKey();
                    List<FinMindInstitutionalInvestorsResponse.InstitutionalInvestorData> dayData = entry.getValue();

                    // 初始化各法人的買進、賣出、買賣超
                    Long foreignBuy = null;
                    Long foreignSell = null;
                    Long trustBuy = null;
                    Long trustSell = null;
                    Long dealerSelfBuy = null;
                    Long dealerSelfSell = null;
                    Long dealerHedgingBuy = null;
                    Long dealerHedgingSell = null;
                    String stockId = null;

                    // 遍歷每筆記錄，根據 name 分類
                    for (FinMindInstitutionalInvestorsResponse.InstitutionalInvestorData item : dayData) {
                        if (stockId == null) {
                            stockId = item.getStockId();
                        }

                        String name = item.getName();
                        Long buy = parseLong(item.getBuy());
                        Long sell = parseLong(item.getSell());

                        switch (name) {
                            case "Foreign_Investor":
                                foreignBuy = buy;
                                foreignSell = sell;
                                break;
                            case "Investment_Trust":
                                trustBuy = buy;
                                trustSell = sell;
                                break;
                            case "Dealer_self":
                                dealerSelfBuy = buy;
                                dealerSelfSell = sell;
                                break;
                            case "Dealer_Hedging":
                                dealerHedgingBuy = buy;
                                dealerHedgingSell = sell;
                                break;
                        }
                    }

                    // 計算外資買賣超
                    Long foreignBuySell = null;
                    if (foreignBuy != null && foreignSell != null) {
                        foreignBuySell = foreignBuy - foreignSell;
                    }

                    // 計算投信買賣超
                    Long trustBuySell = null;
                    if (trustBuy != null && trustSell != null) {
                        trustBuySell = trustBuy - trustSell;
                    }

                    // 合併自營商自營與避險
                    Long dealerBuy = null;
                    if (dealerSelfBuy != null && dealerHedgingBuy != null) {
                        dealerBuy = dealerSelfBuy + dealerHedgingBuy;
                    } else if (dealerSelfBuy != null) {
                        dealerBuy = dealerSelfBuy;
                    } else if (dealerHedgingBuy != null) {
                        dealerBuy = dealerHedgingBuy;
                    }

                    Long dealerSell = null;
                    if (dealerSelfSell != null && dealerHedgingSell != null) {
                        dealerSell = dealerSelfSell + dealerHedgingSell;
                    } else if (dealerSelfSell != null) {
                        dealerSell = dealerSelfSell;
                    } else if (dealerHedgingSell != null) {
                        dealerSell = dealerHedgingSell;
                    }

                    Long dealerBuySell = null;
                    if (dealerBuy != null && dealerSell != null) {
                        dealerBuySell = dealerBuy - dealerSell;
                    }

                    // 計算三大法人合計
                    Long totalBuy = null;
                    if (foreignBuy != null || trustBuy != null || dealerBuy != null) {
                        totalBuy = (foreignBuy != null ? foreignBuy : 0L) +
                                (trustBuy != null ? trustBuy : 0L) +
                                (dealerBuy != null ? dealerBuy : 0L);
                    }

                    Long totalSell = null;
                    if (foreignSell != null || trustSell != null || dealerSell != null) {
                        totalSell = (foreignSell != null ? foreignSell : 0L) +
                                (trustSell != null ? trustSell : 0L) +
                                (dealerSell != null ? dealerSell : 0L);
                    }

                    Long totalBuySell = null;
                    if (totalBuy != null && totalSell != null) {
                        totalBuySell = totalBuy - totalSell;
                    }

                    return InstitutionalInvestorsData.builder()
                            .symbol(stockId)
                            .date(LocalDate.parse(date, DATE_FORMATTER))
                            .foreignInvestorBuy(foreignBuy)
                            .foreignInvestorSell(foreignSell)
                            .foreignInvestorBuySell(foreignBuySell)
                            .investmentTrustBuy(trustBuy)
                            .investmentTrustSell(trustSell)
                            .investmentTrustBuySell(trustBuySell)
                            .dealerBuy(dealerBuy)
                            .dealerSell(dealerSell)
                            .dealerBuySell(dealerBuySell)
                            .totalBuy(totalBuy)
                            .totalSell(totalSell)
                            .totalBuySell(totalBuySell)
                            .build();
                })
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))  // 按日期排序（從新到舊）
                .collect(Collectors.toList());
    }

}