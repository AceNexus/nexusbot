package com.acenexus.tata.nexusbot.util;

import com.acenexus.tata.nexusbot.dto.CandlestickData;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
}
