package com.acenexus.tata.nexusbot.service;

import com.acenexus.tata.nexusbot.dto.CandlestickData;
import com.acenexus.tata.nexusbot.dto.TechnicalIndicatorData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 技術分析服務
 * 提供各種技術指標的計算功能
 */
@Slf4j
@Service
public class TechnicalAnalysisService {

    private static final int RSI_PERIOD = 14;
    private static final int SCALE = 2;

    /**
     * 計算簡單移動平均線 (SMA)
     *
     * @param data   K線數據（需依日期由舊到新排序）
     * @param period 週期
     * @return 每日的 SMA 值列表（前 period-1 筆為 null）
     */
    public List<BigDecimal> calculateSMA(List<CandlestickData> data, int period) {
        List<BigDecimal> smaList = new ArrayList<>();

        if (data == null || data.isEmpty() || period <= 0) {
            return smaList;
        }

        for (int i = 0; i < data.size(); i++) {
            if (i < period - 1) {
                smaList.add(null);
            } else {
                BigDecimal sum = BigDecimal.ZERO;
                for (int j = i - period + 1; j <= i; j++) {
                    BigDecimal close = data.get(j).getClose();
                    if (close != null) {
                        sum = sum.add(close);
                    }
                }
                BigDecimal sma = sum.divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
                smaList.add(sma);
            }
        }

        return smaList;
    }

    /**
     * 計算相對強弱指標 (RSI)
     * 使用 Wilder's Smoothing Method
     *
     * @param data   K線數據（需依日期由舊到新排序）
     * @param period RSI 週期（預設 14）
     * @return 最新的 RSI 值
     */
    public BigDecimal calculateRSI(List<CandlestickData> data, int period) {
        if (data == null || data.size() < period + 1) {
            return null;
        }

        List<BigDecimal> gains = new ArrayList<>();
        List<BigDecimal> losses = new ArrayList<>();

        // 計算每日漲跌
        for (int i = 1; i < data.size(); i++) {
            BigDecimal prevClose = data.get(i - 1).getClose();
            BigDecimal currClose = data.get(i).getClose();

            if (prevClose == null || currClose == null) {
                gains.add(BigDecimal.ZERO);
                losses.add(BigDecimal.ZERO);
                continue;
            }

            BigDecimal change = currClose.subtract(prevClose);
            if (change.compareTo(BigDecimal.ZERO) > 0) {
                gains.add(change);
                losses.add(BigDecimal.ZERO);
            } else {
                gains.add(BigDecimal.ZERO);
                losses.add(change.abs());
            }
        }

        if (gains.size() < period) {
            return null;
        }

        // 計算初始平均漲跌
        BigDecimal avgGain = BigDecimal.ZERO;
        BigDecimal avgLoss = BigDecimal.ZERO;

        for (int i = 0; i < period; i++) {
            avgGain = avgGain.add(gains.get(i));
            avgLoss = avgLoss.add(losses.get(i));
        }

        avgGain = avgGain.divide(BigDecimal.valueOf(period), 10, RoundingMode.HALF_UP);
        avgLoss = avgLoss.divide(BigDecimal.valueOf(period), 10, RoundingMode.HALF_UP);

        // 使用 Wilder's Smoothing 計算後續平均
        for (int i = period; i < gains.size(); i++) {
            avgGain = avgGain.multiply(BigDecimal.valueOf(period - 1))
                    .add(gains.get(i))
                    .divide(BigDecimal.valueOf(period), 10, RoundingMode.HALF_UP);
            avgLoss = avgLoss.multiply(BigDecimal.valueOf(period - 1))
                    .add(losses.get(i))
                    .divide(BigDecimal.valueOf(period), 10, RoundingMode.HALF_UP);
        }

        // 計算 RSI
        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }

        BigDecimal rs = avgGain.divide(avgLoss, 10, RoundingMode.HALF_UP);
        BigDecimal rsi = BigDecimal.valueOf(100).subtract(
                BigDecimal.valueOf(100).divide(
                        BigDecimal.ONE.add(rs), 10, RoundingMode.HALF_UP
                )
        );

        return rsi.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 計算月線乖離率 (MA Bias)
     * 公式：(收盤價 - MA) / MA * 100%
     *
     * @param data     K線數據
     * @param maPeriod MA 週期
     * @return 乖離率（百分比）
     */
    public BigDecimal calculateMABias(List<CandlestickData> data, int maPeriod) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        List<BigDecimal> maList = calculateSMA(data, maPeriod);
        if (maList.isEmpty()) {
            return null;
        }

        BigDecimal latestMA = maList.get(maList.size() - 1);
        BigDecimal latestClose = data.get(data.size() - 1).getClose();

        if (latestMA == null || latestClose == null || latestMA.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return latestClose.subtract(latestMA)
                .divide(latestMA, 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 判斷是否站上均線
     *
     * @param data     K線數據
     * @param maPeriod MA 週期
     * @return 是否站上均線
     */
    public Boolean isAboveMA(List<CandlestickData> data, int maPeriod) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        List<BigDecimal> maList = calculateSMA(data, maPeriod);
        if (maList.isEmpty()) {
            return null;
        }

        BigDecimal latestMA = maList.get(maList.size() - 1);
        BigDecimal latestClose = data.get(data.size() - 1).getClose();

        if (latestMA == null || latestClose == null) {
            return null;
        }

        return latestClose.compareTo(latestMA) >= 0;
    }

    /**
     * 計算完整的技術指標資料列表
     *
     * @param data K線數據（需依日期由舊到新排序）
     * @return 技術指標資料列表
     */
    public List<TechnicalIndicatorData> calculateAllIndicators(List<CandlestickData> data) {
        List<TechnicalIndicatorData> indicators = new ArrayList<>();

        if (data == null || data.isEmpty()) {
            return indicators;
        }

        // 計算各週期 SMA
        List<BigDecimal> ma5List = calculateSMA(data, 5);
        List<BigDecimal> ma20List = calculateSMA(data, 20);
        List<BigDecimal> ma60List = calculateSMA(data, 60);

        // 組裝每日的技術指標
        for (int i = 0; i < data.size(); i++) {
            CandlestickData candle = data.get(i);

            // 計算該日的 RSI（需要之前的數據）
            BigDecimal rsi = null;
            if (i >= RSI_PERIOD) {
                rsi = calculateRSI(data.subList(0, i + 1), RSI_PERIOD);
            }

            // 計算該日的乖離率
            BigDecimal ma20Bias = null;
            BigDecimal ma20 = i < ma20List.size() ? ma20List.get(i) : null;
            BigDecimal close = candle.getClose();
            if (ma20 != null && close != null && ma20.compareTo(BigDecimal.ZERO) != 0) {
                ma20Bias = close.subtract(ma20)
                        .divide(ma20, 10, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(SCALE, RoundingMode.HALF_UP);
            }

            // 判斷是否站上月線
            Boolean aboveMA20 = null;
            if (ma20 != null && close != null) {
                aboveMA20 = close.compareTo(ma20) >= 0;
            }

            indicators.add(TechnicalIndicatorData.builder()
                    .date(candle.getDate())
                    .ma5(i < ma5List.size() ? ma5List.get(i) : null)
                    .ma20(ma20)
                    .ma60(i < ma60List.size() ? ma60List.get(i) : null)
                    .rsi(rsi)
                    .ma20Bias(ma20Bias)
                    .aboveMA20(aboveMA20)
                    .build());
        }

        return indicators;
    }
}
