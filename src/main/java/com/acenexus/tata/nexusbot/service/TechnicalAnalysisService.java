package com.acenexus.tata.nexusbot.service;

import com.acenexus.tata.nexusbot.dto.CandlestickData;
import com.acenexus.tata.nexusbot.dto.TechnicalAnalysisResult;
import com.acenexus.tata.nexusbot.util.MovingAverageCalculator;
import com.acenexus.tata.nexusbot.util.TechnicalIndicatorCalculator;
import com.acenexus.tata.nexusbot.util.TechnicalIndicatorCalculator.BollingerBandsResult;
import com.acenexus.tata.nexusbot.util.TechnicalIndicatorCalculator.KDResult;
import com.acenexus.tata.nexusbot.util.TechnicalIndicatorCalculator.MACDResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 技術分析服務
 * 提供股票技術分析功能
 */
@Slf4j
@Service
public class TechnicalAnalysisService {

    /**
     * 執行完整技術分析
     *
     * @param symbol 股票代號
     * @param data   K線數據（至少需要 60 天以上）
     * @return 技術分析結果
     */
    public TechnicalAnalysisResult analyze(String symbol, List<CandlestickData> data) {
        if (data == null || data.size() < 60) {
            throw new IllegalArgumentException("數據不足，至少需要 60 筆K線數據");
        }

        int lastIndex = data.size() - 1;
        CandlestickData latestCandle = data.get(lastIndex);

        // 計算技術指標
        TechnicalAnalysisResult.TechnicalIndicators indicators = calculateIndicators(data, lastIndex);

        // 分析買賣訊號
        List<TechnicalAnalysisResult.TradingSignal> signals = analyzeSignals(data, lastIndex, indicators);

        // 計算支撐壓力位
        TechnicalAnalysisResult.SupportResistance sr = calculateSupportResistance(data, lastIndex, indicators);

        // 生成綜合建議
        String recommendation = generateRecommendation(signals, indicators, latestCandle.getClose());
        Integer confidence = calculateConfidence(signals);
        String analysis = generateAnalysisText(signals, indicators, recommendation);

        return TechnicalAnalysisResult.builder()
                .symbol(symbol)
                .name(latestCandle.getName())
                .analysisDate(latestCandle.getDate())
                .currentPrice(latestCandle.getClose())
                .recommendation(recommendation)
                .confidence(confidence)
                .indicators(indicators)
                .signals(signals)
                .supportResistance(sr)
                .analysis(analysis)
                .build();
    }

    /**
     * 計算所有技術指標
     */
    private TechnicalAnalysisResult.TechnicalIndicators calculateIndicators(
            List<CandlestickData> data, int index) {

        // RSI
        BigDecimal rsi6 = TechnicalIndicatorCalculator.calculateRSI(data, index, 6);
        BigDecimal rsi12 = TechnicalIndicatorCalculator.calculateRSI(data, index, 12);

        // KD
        List<KDResult> kdSeries = TechnicalIndicatorCalculator.calculateKDSeries(data, 9);
        KDResult kd = kdSeries.get(index);

        // MACD
        List<MACDResult> macdSeries = TechnicalIndicatorCalculator.calculateMACDSeries(data);
        MACDResult macd = macdSeries.get(index);

        // 布林帶
        BollingerBandsResult bbands = TechnicalIndicatorCalculator.calculateBollingerBands(
                data, index, 20, 2.0
        );

        // 均線
        BigDecimal ma5 = MovingAverageCalculator.calculateMA5(data, index);
        BigDecimal ma10 = MovingAverageCalculator.calculateMA10(data, index);
        BigDecimal ma20 = MovingAverageCalculator.calculateMA20(data, index);
        BigDecimal ma60 = MovingAverageCalculator.calculateMA60(data, index);

        return TechnicalAnalysisResult.TechnicalIndicators.builder()
                .rsi6(rsi6)
                .rsi12(rsi12)
                .kdK(kd != null ? kd.getK() : null)
                .kdD(kd != null ? kd.getD() : null)
                .macdDif(macd != null ? macd.getDif() : null)
                .macdDea(macd != null ? macd.getDea() : null)
                .macdHistogram(macd != null ? macd.getMacd() : null)
                .bbandsUpper(bbands != null ? bbands.getUpper() : null)
                .bbandsMiddle(bbands != null ? bbands.getMiddle() : null)
                .bbandsLower(bbands != null ? bbands.getLower() : null)
                .ma5(ma5)
                .ma10(ma10)
                .ma20(ma20)
                .ma60(ma60)
                .build();
    }

    /**
     * 分析買賣訊號
     */
    private List<TechnicalAnalysisResult.TradingSignal> analyzeSignals(
            List<CandlestickData> data, int index,
            TechnicalAnalysisResult.TechnicalIndicators indicators) {

        List<TechnicalAnalysisResult.TradingSignal> signals = new ArrayList<>();
        CandlestickData current = data.get(index);

        // 1. 均線訊號
        if (index > 0) {
            BigDecimal prevMa5 = MovingAverageCalculator.calculateMA5(data, index - 1);
            BigDecimal prevMa10 = MovingAverageCalculator.calculateMA10(data, index - 1);
            BigDecimal prevMa20 = MovingAverageCalculator.calculateMA20(data, index - 1);

            // 黃金交叉 (MA5 向上穿越 MA10)
            if (indicators.getMa5() != null && indicators.getMa10() != null &&
                    prevMa5 != null && prevMa10 != null) {
                if (prevMa5.compareTo(prevMa10) <= 0 && indicators.getMa5().compareTo(indicators.getMa10()) > 0) {
                    signals.add(createSignal("黃金交叉", "買進", 4,
                            "MA5 向上穿越 MA10，形成黃金交叉，短期轉強訊號", current.getDate()));
                }
            }

            // 死亡交叉 (MA5 向下穿越 MA10)
            if (indicators.getMa5() != null && indicators.getMa10() != null &&
                    prevMa5 != null && prevMa10 != null) {
                if (prevMa5.compareTo(prevMa10) >= 0 && indicators.getMa5().compareTo(indicators.getMa10()) < 0) {
                    signals.add(createSignal("死亡交叉", "賣出", 4,
                            "MA5 向下穿越 MA10，形成死亡交叉，短期轉弱訊號", current.getDate()));
                }
            }

            // 多頭排列 (MA5 > MA10 > MA20 > MA60)
            if (indicators.getMa5() != null && indicators.getMa10() != null &&
                    indicators.getMa20() != null && indicators.getMa60() != null) {
                if (indicators.getMa5().compareTo(indicators.getMa10()) > 0 &&
                        indicators.getMa10().compareTo(indicators.getMa20()) > 0 &&
                        indicators.getMa20().compareTo(indicators.getMa60()) > 0) {
                    signals.add(createSignal("多頭排列", "買進", 3,
                            "均線呈現多頭排列，趨勢向上", current.getDate()));
                }
            }

            // 空頭排列 (MA5 < MA10 < MA20 < MA60)
            if (indicators.getMa5() != null && indicators.getMa10() != null &&
                    indicators.getMa20() != null && indicators.getMa60() != null) {
                if (indicators.getMa5().compareTo(indicators.getMa10()) < 0 &&
                        indicators.getMa10().compareTo(indicators.getMa20()) < 0 &&
                        indicators.getMa20().compareTo(indicators.getMa60()) < 0) {
                    signals.add(createSignal("空頭排列", "賣出", 3,
                            "均線呈現空頭排列，趨勢向下", current.getDate()));
                }
            }
        }

        // 2. KD 訊號
        if (indicators.getKdK() != null && indicators.getKdD() != null) {
            BigDecimal k = indicators.getKdK();
            BigDecimal d = indicators.getKdD();

            // KD 超買 (K > 80, D > 80)
            if (k.compareTo(new BigDecimal("80")) > 0 && d.compareTo(new BigDecimal("80")) > 0) {
                signals.add(createSignal("KD超買", "賣出", 3,
                        String.format("KD值進入超買區 (K=%.2f, D=%.2f)，小心回檔", k, d), current.getDate()));
            }

            // KD 超賣 (K < 20, D < 20)
            if (k.compareTo(new BigDecimal("20")) < 0 && d.compareTo(new BigDecimal("20")) < 0) {
                signals.add(createSignal("KD超賣", "買進", 3,
                        String.format("KD值進入超賣區 (K=%.2f, D=%.2f)，可能反彈", k, d), current.getDate()));
            }

            // KD 黃金交叉 (K 向上穿越 D)
            if (index > 0) {
                List<KDResult> kdSeries = TechnicalIndicatorCalculator.calculateKDSeries(data, 9);
                KDResult prevKd = kdSeries.get(index - 1);
                if (prevKd != null && prevKd.getK().compareTo(prevKd.getD()) <= 0 &&
                        k.compareTo(d) > 0) {
                    signals.add(createSignal("KD黃金交叉", "買進", 3,
                            "K線向上穿越D線，短期買進訊號", current.getDate()));
                }

                // KD 死亡交叉 (K 向下穿越 D)
                if (prevKd != null && prevKd.getK().compareTo(prevKd.getD()) >= 0 &&
                        k.compareTo(d) < 0) {
                    signals.add(createSignal("KD死亡交叉", "賣出", 3,
                            "K線向下穿越D線，短期賣出訊號", current.getDate()));
                }
            }
        }

        // 3. RSI 訊號
        if (indicators.getRsi12() != null) {
            BigDecimal rsi = indicators.getRsi12();

            // RSI 超買 (RSI > 70)
            if (rsi.compareTo(new BigDecimal("70")) > 0) {
                signals.add(createSignal("RSI超買", "賣出", 2,
                        String.format("RSI進入超買區 (%.2f)，注意獲利了結", rsi), current.getDate()));
            }

            // RSI 超賣 (RSI < 30)
            if (rsi.compareTo(new BigDecimal("30")) < 0) {
                signals.add(createSignal("RSI超賣", "買進", 2,
                        String.format("RSI進入超賣區 (%.2f)，可能出現反彈", rsi), current.getDate()));
            }
        }

        // 4. MACD 訊號
        if (indicators.getMacdDif() != null && indicators.getMacdDea() != null) {
            // MACD 黃金交叉 (DIF 向上穿越 DEA)
            if (index > 0) {
                List<MACDResult> macdSeries = TechnicalIndicatorCalculator.calculateMACDSeries(data);
                MACDResult prevMacd = macdSeries.get(index - 1);
                if (prevMacd != null &&
                        prevMacd.getDif().compareTo(prevMacd.getDea()) <= 0 &&
                        indicators.getMacdDif().compareTo(indicators.getMacdDea()) > 0) {
                    signals.add(createSignal("MACD黃金交叉", "買進", 4,
                            "DIF向上穿越DEA，中期買進訊號", current.getDate()));
                }

                // MACD 死亡交叉 (DIF 向下穿越 DEA)
                if (prevMacd != null &&
                        prevMacd.getDif().compareTo(prevMacd.getDea()) >= 0 &&
                        indicators.getMacdDif().compareTo(indicators.getMacdDea()) < 0) {
                    signals.add(createSignal("MACD死亡交叉", "賣出", 4,
                            "DIF向下穿越DEA，中期賣出訊號", current.getDate()));
                }
            }

            // MACD 柱狀圖由負轉正
            if (indicators.getMacdHistogram().compareTo(BigDecimal.ZERO) > 0) {
                if (index > 0) {
                    List<MACDResult> macdSeries = TechnicalIndicatorCalculator.calculateMACDSeries(data);
                    MACDResult prevMacd = macdSeries.get(index - 1);
                    if (prevMacd != null && prevMacd.getMacd().compareTo(BigDecimal.ZERO) <= 0) {
                        signals.add(createSignal("MACD柱狀圖轉正", "買進", 3,
                                "MACD柱狀圖由負轉正，動能轉強", current.getDate()));
                    }
                }
            }
        }

        // 5. 布林帶訊號
        if (indicators.getBbandsUpper() != null && indicators.getBbandsLower() != null) {
            BigDecimal close = current.getClose();

            // 價格觸及下軌
            if (close.compareTo(indicators.getBbandsLower()) <= 0) {
                signals.add(createSignal("觸及布林下軌", "買進", 2,
                        "股價觸及布林帶下軌，可能反彈", current.getDate()));
            }

            // 價格觸及上軌
            if (close.compareTo(indicators.getBbandsUpper()) >= 0) {
                signals.add(createSignal("觸及布林上軌", "賣出", 2,
                        "股價觸及布林帶上軌，可能回檔", current.getDate()));
            }
        }

        return signals;
    }

    /**
     * 計算支撐壓力位
     */
    private TechnicalAnalysisResult.SupportResistance calculateSupportResistance(
            List<CandlestickData> data, int index,
            TechnicalAnalysisResult.TechnicalIndicators indicators) {

        // 第一支撐：MA20
        BigDecimal support1 = indicators.getMa20();

        // 第二支撐：MA60
        BigDecimal support2 = indicators.getMa60();

        // 第一壓力：布林帶上軌
        BigDecimal resistance1 = indicators.getBbandsUpper();

        // 第二壓力：近期高點
        BigDecimal resistance2 = null;
        if (index >= 20) {
            BigDecimal maxHigh = data.get(index - 19).getHigh();
            for (int i = index - 19; i <= index; i++) {
                if (data.get(i).getHigh().compareTo(maxHigh) > 0) {
                    maxHigh = data.get(i).getHigh();
                }
            }
            resistance2 = maxHigh;
        }

        return TechnicalAnalysisResult.SupportResistance.builder()
                .support1(support1)
                .support2(support2)
                .resistance1(resistance1)
                .resistance2(resistance2)
                .build();
    }

    /**
     * 生成綜合建議（基於今日K線分析明天操作）
     */
    private String generateRecommendation(List<TechnicalAnalysisResult.TradingSignal> signals,
                                          TechnicalAnalysisResult.TechnicalIndicators indicators,
                                          BigDecimal currentPrice) {
        int buySignals = 0;
        int sellSignals = 0;
        int buyStrength = 0;
        int sellStrength = 0;

        for (TechnicalAnalysisResult.TradingSignal signal : signals) {
            if ("買進".equals(signal.getDirection())) {
                buySignals++;
                buyStrength += signal.getStrength();
            } else if ("賣出".equals(signal.getDirection())) {
                sellSignals++;
                sellStrength += signal.getStrength();
            }
        }

        // 根據訊號數量和強度判斷明天操作
        // 買入條件：買進訊號明顯強於賣出訊號
        if (buySignals >= 2 && buyStrength > sellStrength + 5) {
            return "買入";
        }
        // 賣出條件：賣出訊號明顯強於買進訊號
        else if (sellSignals >= 2 && sellStrength > buyStrength + 5) {
            return "賣出";
        }
        // 觀望條件：訊號不明確、混合或不足以做出明確判斷
        else {
            return "觀望";
        }
    }

    /**
     * 計算信心指數
     */
    private Integer calculateConfidence(List<TechnicalAnalysisResult.TradingSignal> signals) {
        if (signals.isEmpty()) {
            return 0;
        }

        int totalStrength = 0;
        for (TechnicalAnalysisResult.TradingSignal signal : signals) {
            totalStrength += signal.getStrength();
        }

        // 信心指數 = (總強度 / 訊號數量) * 20，最高 100
        int confidence = Math.min(100, (totalStrength * 20) / signals.size());
        return confidence;
    }

    /**
     * 生成分析文字
     */
    private String generateAnalysisText(List<TechnicalAnalysisResult.TradingSignal> signals,
                                        TechnicalAnalysisResult.TechnicalIndicators indicators,
                                        String recommendation) {
        StringBuilder analysis = new StringBuilder();

        analysis.append("【基於今日K線的技術面分析】\n\n");
        analysis.append(String.format("明日操作建議：%s\n\n", recommendation));

        if (!signals.isEmpty()) {
            analysis.append("主要訊號：\n");
            for (TechnicalAnalysisResult.TradingSignal signal : signals) {
                analysis.append(String.format("• %s - %s\n", signal.getType(), signal.getDescription()));
            }
            analysis.append("\n");
        }

        analysis.append("技術指標狀況：\n");
        if (indicators.getRsi12() != null) {
            analysis.append(String.format("• RSI(12): %.2f ", indicators.getRsi12()));
            if (indicators.getRsi12().compareTo(new BigDecimal("70")) > 0) {
                analysis.append("(超買)\n");
            } else if (indicators.getRsi12().compareTo(new BigDecimal("30")) < 0) {
                analysis.append("(超賣)\n");
            } else {
                analysis.append("(中性)\n");
            }
        }

        if (indicators.getKdK() != null && indicators.getKdD() != null) {
            analysis.append(String.format("• KD: K=%.2f, D=%.2f\n", indicators.getKdK(), indicators.getKdD()));
        }

        if (indicators.getMacdDif() != null && indicators.getMacdDea() != null) {
            analysis.append(String.format("• MACD: DIF=%.2f, DEA=%.2f\n",
                    indicators.getMacdDif(), indicators.getMacdDea()));
        }

        // 添加操作說明
        analysis.append("\n【操作說明】\n");
        switch (recommendation) {
            case "買入":
                analysis.append("技術面出現明顯買進訊號，建議明日可考慮買入建倉。\n");
                break;
            case "賣出":
                analysis.append("技術面出現明顯賣出訊號，建議明日可考慮賣出持股。\n");
                break;
            case "觀望":
                analysis.append("訊號不明確或訊號混合，建議明日暫時觀望，不進行操作。\n");
                break;
        }

        analysis.append("\n投資有風險，以上分析僅供參考，請審慎評估後決策。");

        return analysis.toString();
    }

    /**
     * 創建交易訊號
     */
    private TechnicalAnalysisResult.TradingSignal createSignal(String type, String direction,
                                                               Integer strength, String description,
                                                               java.time.LocalDate date) {
        return TechnicalAnalysisResult.TradingSignal.builder()
                .type(type)
                .direction(direction)
                .strength(strength)
                .description(description)
                .date(date)
                .build();
    }
}
