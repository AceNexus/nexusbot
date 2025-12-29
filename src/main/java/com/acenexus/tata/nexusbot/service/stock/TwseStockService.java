package com.acenexus.tata.nexusbot.service.stock;

import com.acenexus.tata.nexusbot.client.TwseApiClient;
import com.acenexus.tata.nexusbot.dto.StockInfo;
import com.acenexus.tata.nexusbot.dto.TwseApiResponse;
import com.acenexus.tata.nexusbot.enums.StockMarket;
import com.acenexus.tata.nexusbot.util.MdcTaskDecorator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 台灣證交所股票服務實作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TwseStockService implements StockService {

    private final TwseApiClient twseApiClient;

    @Override
    public CompletableFuture<Optional<StockInfo>> getStockInfo(String symbol, StockMarket market) {
        if (!supports(market)) {
            log.warn("TWSE service - unsupported market={}", market);
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return CompletableFuture.supplyAsync(MdcTaskDecorator.wrapSupplier(() -> {
            try {
                log.debug("TWSE query started - symbol={}", symbol);

                Optional<TwseApiResponse.TwseStockData> stockDataOpt = twseApiClient.getStockQuote(symbol);

                if (stockDataOpt.isEmpty()) {
                    log.info("TWSE stock not found - symbol={}", symbol);
                    return Optional.empty();
                }

                TwseApiResponse.TwseStockData stockData = stockDataOpt.get();
                StockInfo stockInfo = convertToStockInfo(stockData);

                log.info("TWSE stock retrieved - name={}, symbol={}, price={}, volume={}", stockInfo.getName(), symbol, stockInfo.getCurrentPrice(), stockInfo.getVolume() != null ? stockInfo.getVolume() / 1000 : 0);
                return Optional.of(stockInfo);

            } catch (Exception e) {
                log.error("TWSE query failed - symbol={}, error={}", symbol, e.getMessage(), e);
                return Optional.empty();
            }
        }));
    }

    @Override
    public boolean supports(StockMarket market) {
        return market == StockMarket.TW;
    }

    /**
     * 將 TWSE API 資料轉換為通用 StockInfo
     */
    private StockInfo convertToStockInfo(TwseApiResponse.TwseStockData data) {
        BigDecimal currentPrice = parseBigDecimal(data.getCurrentPrice());
        BigDecimal previousClose = parseBigDecimal(data.getPreviousClose());

        // 計算漲跌金額
        BigDecimal change = null;
        if (currentPrice != null && previousClose != null && previousClose.compareTo(BigDecimal.ZERO) > 0) {
            change = currentPrice.subtract(previousClose);
        }

        // 計算漲跌幅 (%)
        BigDecimal changePercent = null;
        if (change != null && previousClose != null && previousClose.compareTo(BigDecimal.ZERO) > 0) {
            changePercent = change.divide(previousClose, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        // 解析更新時間
        LocalDateTime updateTime = parseDateTime(data.getDate(), data.getTime());

        // 成交量從張轉換為股（1張 = 1000股）
        Long volumeInShares = parseVolume(data.getVolume());

        return StockInfo.builder()
                .symbol(data.getSymbol())
                .name(data.getName())
                .currentPrice(currentPrice)
                .openPrice(parseBigDecimal(data.getOpenPrice()))
                .highPrice(parseBigDecimal(data.getHighPrice()))
                .lowPrice(parseBigDecimal(data.getLowPrice()))
                .previousClose(previousClose)
                .change(change)
                .changePercent(changePercent)
                .volume(volumeInShares)
                .updateTime(updateTime)
                .limitUp(parseBigDecimal(data.getLimitUp()))
                .limitDown(parseBigDecimal(data.getLimitDown()))
                .bestBidPrice(parseBigDecimal(data.getBestBidPrice()))
                .bestAskPrice(parseBigDecimal(data.getBestAskPrice()))
                .turnover(parseBigDecimal(data.getTurnover()))
                .build();
    }

    /**
     * 安全解析 BigDecimal
     */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty() || "-".equals(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            log.debug("Failed to parse BigDecimal from value: {}", value);
            return null;
        }
    }

    /**
     * 解析成交量（從張轉換為股）
     */
    private Long parseVolume(String value) {
        if (value == null || value.trim().isEmpty() || "-".equals(value)) {
            return null;
        }
        try {
            // 成交量單位是「張」，1張 = 1000股
            long volumeInLots = Long.parseLong(value.replace(",", ""));
            return volumeInLots * 1000;
        } catch (NumberFormatException e) {
            log.debug("Failed to parse volume from value: {}", value);
            return null;
        }
    }

    /**
     * 解析日期時間
     *
     * @param date 日期字串 (yyyyMMdd)
     * @param time 時間字串 (HH:mm:ss)
     * @return LocalDateTime
     */
    private LocalDateTime parseDateTime(String date, String time) {
        try {
            if (date == null || time == null) {
                return LocalDateTime.now();
            }

            LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
            LocalTime localTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm:ss"));

            return LocalDateTime.of(localDate, localTime);
        } catch (Exception e) {
            log.debug("Failed to parse datetime from date: {}, time: {}", date, time);
            return LocalDateTime.now();
        }
    }
}
