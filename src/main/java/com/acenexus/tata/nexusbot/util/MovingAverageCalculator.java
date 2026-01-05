package com.acenexus.tata.nexusbot.util;

import com.acenexus.tata.nexusbot.dto.CandlestickData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 移動平均線計算工具
 */
public class MovingAverageCalculator {

    /**
     * 計算簡單移動平均線（SMA）
     *
     * @param data   K線數據列表（必須按日期排序）
     * @param period 週期（天數）
     * @return 移動平均線數值（與輸入數據對應，數據不足時返回 null）
     */
    public static BigDecimal calculateMA(List<CandlestickData> data, int index, int period) {
        // 檢查數據是否足夠
        if (index < period - 1) {
            return null;
        }

        // 計算平均值
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = index - period + 1; i <= index; i++) {
            BigDecimal close = data.get(i).getClose();
            if (close == null) {
                return null;  // 如果有任何收盤價為 null，則無法計算
            }
            sum = sum.add(close);
        }

        return sum.divide(BigDecimal.valueOf(period), 2, RoundingMode.HALF_UP);
    }

    /**
     * 計算 MA5（5日均線）
     */
    public static BigDecimal calculateMA5(List<CandlestickData> data, int index) {
        return calculateMA(data, index, 5);
    }

    /**
     * 計算 MA10（10日均線）
     */
    public static BigDecimal calculateMA10(List<CandlestickData> data, int index) {
        return calculateMA(data, index, 10);
    }

    /**
     * 計算 MA20（20日均線）
     */
    public static BigDecimal calculateMA20(List<CandlestickData> data, int index) {
        return calculateMA(data, index, 20);
    }

    /**
     * 計算 MA60（60日均線）
     */
    public static BigDecimal calculateMA60(List<CandlestickData> data, int index) {
        return calculateMA(data, index, 60);
    }

    /**
     * 計算指數移動平均線（EMA）
     * EMA = Price(t) × α + EMA(t-1) × (1-α)
     * 其中 α = 2 / (period + 1)
     *
     * @param data        K線數據列表
     * @param index       當前索引
     * @param period      週期（天數）
     * @param previousEMA 前一個 EMA 值（首次計算傳入 null）
     * @return EMA 值（數據不足時返回 null）
     */
    public static BigDecimal calculateEMA(List<CandlestickData> data, int index,
                                          int period, BigDecimal previousEMA) {
        if (index < 0) {
            return null;
        }

        BigDecimal close = data.get(index).getClose();
        if (close == null) {
            return null;
        }

        // 首次計算使用 SMA 作為初始值
        if (previousEMA == null) {
            if (index < period - 1) {
                return null;
            }
            return calculateMA(data, index, period);
        }

        // α = 2 / (period + 1)
        BigDecimal alpha = new BigDecimal("2")
                .divide(BigDecimal.valueOf(period + 1), 4, RoundingMode.HALF_UP);

        // EMA = close × α + previousEMA × (1-α)
        BigDecimal ema = close.multiply(alpha)
                .add(previousEMA.multiply(BigDecimal.ONE.subtract(alpha)));

        return ema.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 批量計算 EMA 序列
     * 自動處理前值傳遞，返回完整的 EMA 序列
     *
     * @param data   K線數據列表
     * @param period 週期（天數）
     * @return EMA 序列（與輸入數據長度相同，數據不足部分為 null）
     */
    public static List<BigDecimal> calculateEMASeries(List<CandlestickData> data, int period) {
        List<BigDecimal> results = new ArrayList<>();
        BigDecimal previousEMA = null;

        for (int i = 0; i < data.size(); i++) {
            BigDecimal ema = calculateEMA(data, i, period, previousEMA);
            results.add(ema);
            if (ema != null) {
                previousEMA = ema;
            }
        }

        return results;
    }
}
