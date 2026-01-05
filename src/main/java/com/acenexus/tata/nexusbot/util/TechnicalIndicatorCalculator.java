package com.acenexus.tata.nexusbot.util;

import com.acenexus.tata.nexusbot.dto.CandlestickData;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 技術指標計算工具
 * 提供常用技術分析指標計算功能
 */
public class TechnicalIndicatorCalculator {

    /**
     * KD 指標結果
     */
    @Data
    @AllArgsConstructor
    public static class KDResult {
        private BigDecimal k;  // K 值
        private BigDecimal d;  // D 值
    }

    /**
     * MACD 指標結果
     */
    @Data
    @AllArgsConstructor
    public static class MACDResult {
        private BigDecimal dif;        // DIF (快線)
        private BigDecimal dea;        // DEA (慢線)
        private BigDecimal macd;       // MACD 柱狀圖
    }

    /**
     * 布林帶結果
     */
    @Data
    @AllArgsConstructor
    public static class BollingerBandsResult {
        private BigDecimal upper;      // 上軌
        private BigDecimal middle;     // 中軌 (MA)
        private BigDecimal lower;      // 下軌
    }

    /**
     * 計算 RSI (相對強弱指標)
     *
     * @param data   K線數據
     * @param index  當前索引
     * @param period 週期 (通常為 6, 12, 24)
     * @return RSI 值 (0-100)
     */
    public static BigDecimal calculateRSI(List<CandlestickData> data, int index, int period) {
        if (index < period) {
            return null;
        }

        BigDecimal gainSum = BigDecimal.ZERO;
        BigDecimal lossSum = BigDecimal.ZERO;

        for (int i = index - period + 1; i <= index; i++) {
            BigDecimal change = data.get(i).getChange();
            if (change == null) {
                return null;
            }

            if (change.compareTo(BigDecimal.ZERO) > 0) {
                gainSum = gainSum.add(change);
            } else {
                lossSum = lossSum.add(change.abs());
            }
        }

        if (lossSum.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("100");
        }

        BigDecimal avgGain = gainSum.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
        BigDecimal avgLoss = lossSum.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);

        BigDecimal rs = avgGain.divide(avgLoss, 4, RoundingMode.HALF_UP);
        BigDecimal rsi = new BigDecimal("100").subtract(
                new BigDecimal("100").divide(BigDecimal.ONE.add(rs), 2, RoundingMode.HALF_UP)
        );

        return rsi;
    }

    /**
     * 計算 KD 指標 (隨機指標)
     *
     * @param data      K線數據
     * @param index     當前索引
     * @param period    RSV 週期 (通常為 9)
     * @param kPrevious 前一個 K 值 (首次可為 50)
     * @param dPrevious 前一個 D 值 (首次可為 50)
     * @return KD 結果
     */
    public static KDResult calculateKD(List<CandlestickData> data, int index, int period,
                                       BigDecimal kPrevious, BigDecimal dPrevious) {
        if (index < period - 1) {
            return null;
        }

        // 計算 RSV (未成熟隨機值)
        BigDecimal highest = data.get(index - period + 1).getHigh();
        BigDecimal lowest = data.get(index - period + 1).getLow();

        for (int i = index - period + 2; i <= index; i++) {
            BigDecimal high = data.get(i).getHigh();
            BigDecimal low = data.get(i).getLow();

            if (high != null && high.compareTo(highest) > 0) {
                highest = high;
            }
            if (low != null && low.compareTo(lowest) < 0) {
                lowest = low;
            }
        }

        BigDecimal close = data.get(index).getClose();
        if (close == null || highest.compareTo(lowest) == 0) {
            return null;
        }

        BigDecimal rsv = close.subtract(lowest)
                .divide(highest.subtract(lowest), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        // K = (2/3) * K前值 + (1/3) * RSV
        BigDecimal k = kPrevious.multiply(new BigDecimal("2"))
                .add(rsv)
                .divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP);

        // D = (2/3) * D前值 + (1/3) * K
        BigDecimal d = dPrevious.multiply(new BigDecimal("2"))
                .add(k)
                .divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP);

        return new KDResult(k, d);
    }

    /**
     * 計算 MACD 指標
     *
     * @param data          K線數據
     * @param index         當前索引
     * @param fastPeriod    快速週期 (通常為 12)
     * @param slowPeriod    慢速週期 (通常為 26)
     * @param signalPeriod  訊號週期 (通常為 9)
     * @param ema12Previous 前一個 EMA12 值
     * @param ema26Previous 前一個 EMA26 值
     * @param deaPrevious   前一個 DEA 值
     * @return MACD 結果
     */
    public static MACDResult calculateMACD(List<CandlestickData> data, int index,
                                           int fastPeriod, int slowPeriod, int signalPeriod,
                                           BigDecimal ema12Previous, BigDecimal ema26Previous,
                                           BigDecimal deaPrevious) {
        if (index < 0) {
            return null;
        }

        BigDecimal close = data.get(index).getClose();
        if (close == null) {
            return null;
        }

        // 計算 EMA12 和 EMA26
        BigDecimal multiplier12 = new BigDecimal("2").divide(
                BigDecimal.valueOf(fastPeriod + 1), 4, RoundingMode.HALF_UP
        );
        BigDecimal multiplier26 = new BigDecimal("2").divide(
                BigDecimal.valueOf(slowPeriod + 1), 4, RoundingMode.HALF_UP
        );

        BigDecimal ema12 = ema12Previous == null ? close :
                close.subtract(ema12Previous).multiply(multiplier12).add(ema12Previous);

        BigDecimal ema26 = ema26Previous == null ? close :
                close.subtract(ema26Previous).multiply(multiplier26).add(ema26Previous);

        // DIF = EMA12 - EMA26
        BigDecimal dif = ema12.subtract(ema26);

        // DEA = DIF 的 9 日 EMA
        BigDecimal multiplierSignal = new BigDecimal("2").divide(
                BigDecimal.valueOf(signalPeriod + 1), 4, RoundingMode.HALF_UP
        );
        BigDecimal dea = deaPrevious == null ? dif :
                dif.subtract(deaPrevious).multiply(multiplierSignal).add(deaPrevious);

        // MACD = DIF - DEA (符合業界標準)
        BigDecimal macd = dif.subtract(dea);

        return new MACDResult(
                dif.setScale(2, RoundingMode.HALF_UP),
                dea.setScale(2, RoundingMode.HALF_UP),
                macd.setScale(2, RoundingMode.HALF_UP)
        );
    }

    /**
     * 計算布林帶
     *
     * @param data   K線數據
     * @param index  當前索引
     * @param period MA 週期 (通常為 20)
     * @param stdDev 標準差倍數 (通常為 2)
     * @return 布林帶結果
     */
    public static BollingerBandsResult calculateBollingerBands(List<CandlestickData> data,
                                                               int index, int period,
                                                               double stdDev) {
        if (index < period - 1) {
            return null;
        }

        // 計算中軌 (MA)
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = index - period + 1; i <= index; i++) {
            BigDecimal close = data.get(i).getClose();
            if (close == null) {
                return null;
            }
            sum = sum.add(close);
        }
        BigDecimal middle = sum.divide(BigDecimal.valueOf(period), 2, RoundingMode.HALF_UP);

        // 計算標準差
        BigDecimal variance = BigDecimal.ZERO;
        for (int i = index - period + 1; i <= index; i++) {
            BigDecimal close = data.get(i).getClose();
            BigDecimal diff = close.subtract(middle);
            variance = variance.add(diff.multiply(diff));
        }
        variance = variance.divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);

        double std = Math.sqrt(variance.doubleValue());
        BigDecimal stdBigDecimal = BigDecimal.valueOf(std * stdDev)
                .setScale(2, RoundingMode.HALF_UP);

        // 上軌 = 中軌 + (標準差 * 倍數)
        BigDecimal upper = middle.add(stdBigDecimal);
        // 下軌 = 中軌 - (標準差 * 倍數)
        BigDecimal lower = middle.subtract(stdBigDecimal);

        return new BollingerBandsResult(upper, middle, lower);
    }

    /**
     * 計算全部 KD 指標序列
     */
    public static List<KDResult> calculateKDSeries(List<CandlestickData> data, int period) {
        List<KDResult> results = new ArrayList<>();
        BigDecimal kPrev = new BigDecimal("50");
        BigDecimal dPrev = new BigDecimal("50");

        for (int i = 0; i < data.size(); i++) {
            KDResult kd = calculateKD(data, i, period, kPrev, dPrev);
            results.add(kd);

            if (kd != null) {
                kPrev = kd.getK();
                dPrev = kd.getD();
            }
        }

        return results;
    }

    /**
     * 計算全部 MACD 指標序列
     */
    public static List<MACDResult> calculateMACDSeries(List<CandlestickData> data) {
        List<MACDResult> results = new ArrayList<>();
        BigDecimal ema12Prev = null;
        BigDecimal ema26Prev = null;
        BigDecimal deaPrev = null;

        for (int i = 0; i < data.size(); i++) {
            MACDResult macd = calculateMACD(data, i, 12, 26, 9, ema12Prev, ema26Prev, deaPrev);
            results.add(macd);

            if (macd != null && i > 0) {
                BigDecimal close = data.get(i).getClose();
                BigDecimal mult12 = new BigDecimal("2").divide(new BigDecimal("13"), 4, RoundingMode.HALF_UP);
                BigDecimal mult26 = new BigDecimal("2").divide(new BigDecimal("27"), 4, RoundingMode.HALF_UP);

                ema12Prev = ema12Prev == null ? close :
                        close.subtract(ema12Prev).multiply(mult12).add(ema12Prev);
                ema26Prev = ema26Prev == null ? close :
                        close.subtract(ema26Prev).multiply(mult26).add(ema26Prev);
                deaPrev = macd.getDea();
            }
        }

        return results;
    }

    /**
     * 計算威廉指標 (Williams %R)
     * Williams %R = (最高價 - 收盤價) / (最高價 - 最低價) × (-100)
     * 範圍: -100 到 0
     * -20 以上為超買，-80 以下為超賣
     *
     * @param data   K線數據
     * @param index  當前索引
     * @param period 週期（通常為 14）
     * @return Williams %R 值
     */
    public static BigDecimal calculateWilliamsR(List<CandlestickData> data, int index, int period) {
        if (index < period - 1) {
            return null;
        }

        // 計算期間內最高價和最低價
        BigDecimal highest = data.get(index - period + 1).getHigh();
        BigDecimal lowest = data.get(index - period + 1).getLow();

        for (int i = index - period + 2; i <= index; i++) {
            BigDecimal high = data.get(i).getHigh();
            BigDecimal low = data.get(i).getLow();

            if (high != null && high.compareTo(highest) > 0) {
                highest = high;
            }
            if (low != null && low.compareTo(lowest) < 0) {
                lowest = low;
            }
        }

        BigDecimal close = data.get(index).getClose();
        if (close == null || highest.compareTo(lowest) == 0) {
            return null;
        }

        // Williams %R = (H - C) / (H - L) × (-100)
        BigDecimal wr = highest.subtract(close)
                .divide(highest.subtract(lowest), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("-100"));

        return wr.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 計算乖離率 (BIAS)
     * BIAS = (收盤價 - MA) / MA × 100%
     * 正值表示價格高於均線，負值表示低於均線
     *
     * @param data   K線數據
     * @param index  當前索引
     * @param period MA 週期（通常為 6, 12, 24）
     * @return BIAS 值（百分比）
     */
    public static BigDecimal calculateBIAS(List<CandlestickData> data, int index, int period) {
        BigDecimal close = data.get(index).getClose();
        BigDecimal ma = MovingAverageCalculator.calculateMA(data, index, period);

        if (close == null || ma == null || ma.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        // BIAS = (C - MA) / MA × 100
        BigDecimal bias = close.subtract(ma)
                .divide(ma, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        return bias.setScale(2, RoundingMode.HALF_UP);
    }
}
