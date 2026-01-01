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

        // K線型態識別（先執行，供後續價格計算使用）
        List<String> patterns = identifyCandlestickPatterns(data);

        // 趨勢描述
        String trendDescription = generateTrendDescription(data, lastIndex, indicators);

        // 生成綜合建議
        String recommendation = generateRecommendation(signals, indicators, latestCandle.getClose());
        Integer confidence = calculateConfidence(signals);

        // 量價關係分析（需要在 generateAnalysisText 之前）
        String priceVolumeAnalysis = analyzePriceVolume(data, lastIndex);

        // 計算進出場時機（整合型態資訊）
        TechnicalAnalysisResult.EntryExitTiming entryExitTiming = calculateEntryExitTiming(
                recommendation, latestCandle.getClose(), indicators, sr, signals, patterns, trendDescription);

        // 生成結構化分析文字（包含所有分析項目）
        String analysis = generateAnalysisText(
                signals,
                indicators,
                recommendation,
                entryExitTiming,
                trendDescription,
                sr,
                patterns,
                priceVolumeAnalysis,
                latestCandle.getClose()
        );

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
                .entryExitTiming(entryExitTiming)
                .candlestickPatterns(patterns)
                .trendDescription(trendDescription)
                .priceVolumeAnalysis(priceVolumeAnalysis)
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
     * 計算信心指數（改進版，更貼近實務）
     *
     * 考慮因素：
     * 1. 訊號一致性（買賣訊號衝突會降低信心）
     * 2. 訊號強度加權
     * 3. 訊號數量（過多或過少都會降低信心）
     * 4. 設定實務上限（最高75，避免過度自信）
     */
    private Integer calculateConfidence(List<TechnicalAnalysisResult.TradingSignal> signals) {
        if (signals.isEmpty()) {
            return 0;
        }

        // 1. 計算訊號方向一致性
        long buySignals = signals.stream().filter(s -> "買進".equals(s.getDirection())).count();
        long sellSignals = signals.stream().filter(s -> "賣出".equals(s.getDirection())).count();

        // 訊號衝突程度（0-1，0表示完全一致，1表示完全衝突）
        double conflictRatio = Math.min(buySignals, sellSignals) / (double) Math.max(buySignals, sellSignals);

        // 一致性分數（衝突越少分數越高）
        double consistencyScore = 1.0 - (conflictRatio * 0.5);  // 完全衝突時降至50%

        // 2. 計算加權強度分數
        int totalStrength = 0;
        int maxPossibleStrength = signals.size() * 5;  // 假設最高強度為5

        for (TechnicalAnalysisResult.TradingSignal signal : signals) {
            totalStrength += signal.getStrength();
        }

        double strengthScore = (double) totalStrength / maxPossibleStrength;

        // 3. 訊號數量因子（3-6個訊號最理想，太少或太多都扣分）
        double quantityFactor = 1.0;
        if (signals.size() < 3) {
            quantityFactor = 0.7;  // 訊號太少降低30%
        } else if (signals.size() > 7) {
            quantityFactor = 0.85;  // 訊號太多可能相互矛盾，降低15%
        }

        // 4. 綜合計算（最高75分，避免過度自信）
        // 基礎分數 = 強度分數(40%) + 一致性分數(60%)
        double baseScore = (strengthScore * 0.4 + consistencyScore * 0.6) * 75;

        // 套用數量因子
        int finalConfidence = (int) Math.round(baseScore * quantityFactor);

        // 確保在合理範圍內（10-75）
        return Math.max(10, Math.min(75, finalConfidence));
    }

    /**
     * 計算進出場時機
     */
    private TechnicalAnalysisResult.EntryExitTiming calculateEntryExitTiming(
            String recommendation,
            BigDecimal currentPrice,
            TechnicalAnalysisResult.TechnicalIndicators indicators,
            TechnicalAnalysisResult.SupportResistance sr,
            List<TechnicalAnalysisResult.TradingSignal> signals,
            List<String> patterns,
            String trendDescription) {

        String action;
        BigDecimal entryPrice = null;
        BigDecimal entryPriceMin = null;
        BigDecimal entryPriceMax = null;
        BigDecimal exitPrice = null;
        BigDecimal exitPriceMin = null;
        BigDecimal exitPriceMax = null;
        BigDecimal stopLossPrice = null;
        BigDecimal takeProfit1 = null;
        BigDecimal takeProfit2 = null;
        String riskRewardRatio = null;
        String analysisDetail;
        Integer positionSize = null;

        switch (recommendation) {
            case "買入":
                action = "進場";

                // 使用智能買入價計算（整合型態和趨勢）
                BigDecimal[] entryPrices = calculateSmartEntryPrice(currentPrice, indicators, sr, signals, patterns, trendDescription);
                entryPrice = entryPrices[0];      // 建議價
                entryPriceMin = entryPrices[1];   // 最佳價（保守）
                entryPriceMax = entryPrices[2];   // 可接受價（積極）

                // 停損價位：第二支撐位下方 2-3%
                if (sr.getSupport2() != null) {
                    stopLossPrice = sr.getSupport2().multiply(new BigDecimal("0.97"));
                } else if (sr.getSupport1() != null) {
                    stopLossPrice = sr.getSupport1().multiply(new BigDecimal("0.97"));
                } else if (indicators.getBbandsLower() != null) {
                    stopLossPrice = indicators.getBbandsLower().multiply(new BigDecimal("0.98"));
                } else {
                    stopLossPrice = currentPrice.multiply(new BigDecimal("0.95"));
                }

                // 第一停利價位：第一壓力位
                takeProfit1 = sr.getResistance1();

                // 第二停利價位：第二壓力位
                takeProfit2 = sr.getResistance2();

                // 計算風險報酬比
                if (stopLossPrice != null && takeProfit1 != null) {
                    BigDecimal risk = entryPrice.subtract(stopLossPrice);
                    BigDecimal reward = takeProfit1.subtract(entryPrice);
                    if (risk.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal ratio = reward.divide(risk, 2, BigDecimal.ROUND_HALF_UP);
                        riskRewardRatio = String.format("1:%.2f", ratio);
                    }
                }

                // 建議部位大小
                // 根據訊號強度決定部位大小
                int buySignals = (int) signals.stream()
                        .filter(s -> "買進".equals(s.getDirection()))
                        .count();
                if (buySignals >= 4) {
                    positionSize = 50;  // 強烈買進訊號
                } else if (buySignals >= 3) {
                    positionSize = 40;
                } else if (buySignals >= 2) {
                    positionSize = 30;
                } else {
                    positionSize = 20;
                }

                // 不生成詳細分析內容
                analysisDetail = null;
                break;

            case "賣出":
                action = "出場";

                // 使用智能賣出價計算（整合型態和趨勢）
                BigDecimal[] exitPrices = calculateSmartExitPrice(currentPrice, indicators, sr, signals, patterns, trendDescription);
                exitPrice = exitPrices[0];      // 建議價
                exitPriceMin = exitPrices[1];   // 可接受價（保守）
                exitPriceMax = exitPrices[2];   // 最佳價（積極）

                // 對於持有者，停損價位在當前價格上方（以防反彈）
                if (sr.getResistance1() != null && sr.getResistance1().compareTo(currentPrice) > 0) {
                    stopLossPrice = sr.getResistance1();
                } else {
                    stopLossPrice = currentPrice.multiply(new BigDecimal("1.03"));
                }

                // 不生成詳細分析內容
                int sellSignals = (int) signals.stream()
                        .filter(s -> "賣出".equals(s.getDirection()))
                        .count();

                if (sellSignals >= 3) {
                    positionSize = 100;  // 建議全部出場
                } else {
                    positionSize = 50;  // 建議減碼 50%
                }

                analysisDetail = null;
                break;

            case "觀望":
            default:
                action = "觀望";

                // 觀望時給予進場和出場的參考價位及區間
                // 使用智能計算提供觀望時的參考價格區間（整合型態和趨勢）
                BigDecimal[] observeEntryPrices = calculateSmartEntryPrice(currentPrice, indicators, sr, signals, patterns, trendDescription);
                entryPrice = observeEntryPrices[0];
                entryPriceMin = observeEntryPrices[1];
                entryPriceMax = observeEntryPrices[2];

                BigDecimal[] observeExitPrices = calculateSmartExitPrice(currentPrice, indicators, sr, signals, patterns, trendDescription);
                exitPrice = observeExitPrices[0];
                exitPriceMin = observeExitPrices[1];
                exitPriceMax = observeExitPrices[2];

                // 不生成詳細分析內容
                log.info("觀望 case - Setting analysisDetail to NULL");
                analysisDetail = null;
                positionSize = 0;  // 不建議操作
                break;
        }

        return TechnicalAnalysisResult.EntryExitTiming.builder()
                .action(action)
                .entryPrice(entryPrice)
                .entryPriceMin(entryPriceMin)
                .entryPriceMax(entryPriceMax)
                .exitPrice(exitPrice)
                .exitPriceMin(exitPriceMin)
                .exitPriceMax(exitPriceMax)
                .stopLossPrice(stopLossPrice)
                .takeProfit1(takeProfit1)
                .takeProfit2(takeProfit2)
                .riskRewardRatio(riskRewardRatio)
                .analysisDetail(analysisDetail)
                .positionSize(positionSize)
                .build();
    }

    /**
     * 生成結構化分析文字（白話文，條列式）
     */
    private String generateAnalysisText(
            List<TechnicalAnalysisResult.TradingSignal> signals,
            TechnicalAnalysisResult.TechnicalIndicators indicators,
            String recommendation,
            TechnicalAnalysisResult.EntryExitTiming entryExitTiming,
            String trendDescription,
            TechnicalAnalysisResult.SupportResistance sr,
            List<String> candlestickPatterns,
            String priceVolumeAnalysis,
            BigDecimal currentPrice) {

        StringBuilder analysis = new StringBuilder();

        // 1. 趨勢判斷
        analysis.append("【趨勢判斷】\n");
        if (trendDescription != null) {
            if (trendDescription.contains("強勢上升") || trendDescription.contains("多頭格局")) {
                analysis.append("• 目前走勢：多頭趨勢\n");
                analysis.append("• 說明：股價持續向上，均線呈多頭排列，短期強勢\n");
            } else if (trendDescription.contains("弱勢下降") || trendDescription.contains("空頭格局")) {
                analysis.append("• 目前走勢：空頭趨勢\n");
                analysis.append("• 說明：股價持續下跌，均線呈空頭排列，賣壓較重\n");
            } else if (trendDescription.contains("盤整")) {
                analysis.append("• 目前走勢：盤整格局\n");
                analysis.append("• 說明：股價在一定區間內震盪，等待方向突破\n");
            } else {
                analysis.append("• 目前走勢：").append(trendDescription).append("\n");
            }
        }

        // 2. 支撐壓力位
        analysis.append("\n【關鍵價格區間】\n");
        if (sr != null) {
            if (sr.getResistance1() != null) {
                analysis.append("• 壓力位：").append(sr.getResistance1()).append(" 元");
                if (sr.getResistance2() != null) {
                    analysis.append(" / ").append(sr.getResistance2()).append(" 元");
                }
                analysis.append("\n  → 股價漲到這附近可能遇到賣壓\n");
            }
            if (sr.getSupport1() != null) {
                analysis.append("• 支撐位：").append(sr.getSupport1()).append(" 元");
                if (sr.getSupport2() != null) {
                    analysis.append(" / ").append(sr.getSupport2()).append(" 元");
                }
                analysis.append("\n  → 股價跌到這附近可能獲得支撐\n");
            }
        }

        // 3. K線型態
        analysis.append("\n【K線型態判讀】\n");
        if (candlestickPatterns != null && !candlestickPatterns.isEmpty()) {
            for (String pattern : candlestickPatterns) {
                analysis.append("• ").append(pattern);

                // 白話解釋
                if (pattern.contains("看漲吞沒")) {
                    analysis.append("\n  → 紅K包住前一根黑K，多方力量變強\n");
                } else if (pattern.contains("看跌吞沒")) {
                    analysis.append("\n  → 黑K包住前一根紅K，空方力量變強\n");
                } else if (pattern.contains("錘子線")) {
                    analysis.append("\n  → 長下影線，下方有買盤撐住\n");
                } else if (pattern.contains("流星線")) {
                    analysis.append("\n  → 長上影線，上方賣壓重\n");
                } else if (pattern.contains("十字線")) {
                    analysis.append("\n  → 開收盤價相近，多空陷入拉鋸\n");
                } else if (pattern.contains("早晨之星")) {
                    analysis.append("\n  → 底部反轉訊號，可能止跌回升\n");
                } else if (pattern.contains("黃昏之星")) {
                    analysis.append("\n  → 頭部反轉訊號，可能見高回落\n");
                } else {
                    analysis.append("\n");
                }
            }
        } else {
            analysis.append("• 近期無明顯特殊K線型態\n");
        }

        // 4. 型態結構（根據趨勢和布林帶判斷）
        analysis.append("\n【型態結構】\n");
        if (indicators.getBbandsUpper() != null && indicators.getBbandsLower() != null) {
            BigDecimal range = indicators.getBbandsUpper().subtract(indicators.getBbandsLower());
            BigDecimal rangePercent = range.divide(currentPrice, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"));

            if (rangePercent.compareTo(new BigDecimal("5")) < 0) {
                analysis.append("• 三角收斂格局\n");
                analysis.append("  → 價格波動縮小，醞釀方向突破\n");
            } else if (trendDescription != null && trendDescription.contains("盤整")) {
                analysis.append("• 箱型整理格局\n");
                analysis.append("  → 在支撐壓力間來回震盪\n");
            } else {
                analysis.append("• 趨勢發展中\n");
                analysis.append("  → 沿著均線方向移動\n");
            }
        }

        // 5. 量價配合
        analysis.append("\n【成交量配合】\n");
        if (priceVolumeAnalysis != null) {
            analysis.append("• ").append(priceVolumeAnalysis).append("\n");

            if (priceVolumeAnalysis.contains("價漲量增")) {
                analysis.append("  → 上漲有量配合，較為健康\n");
            } else if (priceVolumeAnalysis.contains("價漲量縮")) {
                analysis.append("  → 上漲量能不足，需注意動能衰退\n");
            } else if (priceVolumeAnalysis.contains("價跌量增")) {
                analysis.append("  → 下跌有賣壓，弱勢明顯\n");
            } else if (priceVolumeAnalysis.contains("價跌量縮")) {
                analysis.append("  → 賣壓趨緩，可能接近止跌\n");
            }
        }

        // 6. 風險與可能走勢
        analysis.append("\n【風險與可能走勢】\n");

        // 根據指標判斷風險
        boolean isOverbought = (indicators.getRsi12() != null &&
                indicators.getRsi12().compareTo(new BigDecimal("70")) > 0) ||
                (indicators.getKdK() != null &&
                indicators.getKdK().compareTo(new BigDecimal("80")) > 0);

        boolean isOversold = (indicators.getRsi12() != null &&
                indicators.getRsi12().compareTo(new BigDecimal("30")) < 0) ||
                (indicators.getKdK() != null &&
                indicators.getKdK().compareTo(new BigDecimal("20")) < 0);

        if (isOverbought) {
            analysis.append("• 風險：目前技術指標顯示過熱，短線有回檔壓力\n");
            analysis.append("• 可能走勢：可能先拉回整理後再決定方向\n");
        } else if (isOversold) {
            analysis.append("• 風險：目前跌深，但可能出現反彈\n");
            analysis.append("• 可能走勢：等待超賣反彈，或持續築底\n");
        } else {
            analysis.append("• 風險：處於相對健康區間\n");
            if (trendDescription != null && trendDescription.contains("上升")) {
                analysis.append("• 可能走勢：順勢而為，關注能否持續創高\n");
            } else if (trendDescription != null && trendDescription.contains("下降")) {
                analysis.append("• 可能走勢：弱勢格局，留意能否止跌\n");
            } else {
                analysis.append("• 可能走勢：等待方向明朗\n");
            }
        }

        // 結論
        analysis.append("\n【結論】\n");

        // 計算偏多/偏空/觀望
        long buySignals = signals.stream().filter(s -> "買進".equals(s.getDirection())).count();
        long sellSignals = signals.stream().filter(s -> "賣出".equals(s.getDirection())).count();

        String conclusion;
        if (buySignals >= sellSignals + 2) {
            conclusion = "偏多";
            analysis.append("• 技術面：偏多\n");
            analysis.append("• 說明：多項指標顯示多方占優，但仍需注意風險控管\n");
        } else if (sellSignals >= buySignals + 2) {
            conclusion = "偏空";
            analysis.append("• 技術面：偏空\n");
            analysis.append("• 說明：多項指標顯示空方占優，不宜貿然進場\n");
        } else {
            conclusion = "觀望";
            analysis.append("• 技術面：觀望為宜\n");
            analysis.append("• 說明：多空訊號混雜，建議等待方向明確\n");
        }

        analysis.append("\n⚠️ 以上為技術分析，不構成買賣建議，投資有風險，請自行審慎評估。");

        return analysis.toString();
    }

    /**
     * 計算智能賣出價（整合K線型態和趨勢分析）- 更實際的價格計算
     */
    private BigDecimal[] calculateSmartExitPrice(
            BigDecimal currentPrice,
            TechnicalAnalysisResult.TechnicalIndicators indicators,
            TechnicalAnalysisResult.SupportResistance sr,
            List<TechnicalAnalysisResult.TradingSignal> signals,
            List<String> patterns,
            String trendDescription) {

        // 返回 [建議價, 可接受價(保守), 最佳價(積極)]
        BigDecimal[] result = new BigDecimal[3];

        // 分析K線型態影響
        boolean hasBearishPattern = patterns.stream()
                .anyMatch(p -> p.contains("看跌") || p.contains("黃昏") ||
                              p.contains("流星") || p.contains("上吊") ||
                              p.contains("上漲結束"));

        boolean hasBullishPattern = patterns.stream()
                .anyMatch(p -> p.contains("看漲") || p.contains("早晨") ||
                              p.contains("錘子") || p.contains("倒錘") ||
                              p.contains("反轉向上"));

        // 分析趨勢強度
        boolean isStrongDowntrend = trendDescription != null &&
                (trendDescription.contains("弱勢下降") || trendDescription.contains("空頭格局"));

        boolean isStrongUptrend = trendDescription != null &&
                (trendDescription.contains("強勢上升") || trendDescription.contains("多頭格局"));

        // 1. 檢查是否超賣
        boolean isOversold =
                (indicators.getRsi12() != null && indicators.getRsi12().compareTo(new BigDecimal("30")) < 0) ||
                        (indicators.getKdK() != null && indicators.getKdK().compareTo(new BigDecimal("20")) < 0);

        // 2. 計算價格相對布林帶位置 (0=下軌, 0.5=中軌, 1=上軌)
        BigDecimal pricePosition = null;
        if (indicators.getBbandsUpper() != null && indicators.getBbandsLower() != null) {
            BigDecimal range = indicators.getBbandsUpper().subtract(indicators.getBbandsLower());
            if (range.compareTo(BigDecimal.ZERO) > 0) {
                pricePosition = currentPrice.subtract(indicators.getBbandsLower())
                        .divide(range, 4, BigDecimal.ROUND_HALF_UP);
            }
        }

        // 3. 檢查賣出訊號強度
        long sellSignalCount = signals.stream()
                .filter(s -> "賣出".equals(s.getDirection()))
                .count();

        // === 新的價格計算邏輯：更實際，以當前價為基準 ===

        // 基礎反彈幅度設定
        BigDecimal conservativeDiscount = new BigDecimal("0.99");   // 保守: -1% (快速出場)
        BigDecimal recommendedPremium = new BigDecimal("1.005");   // 建議: +0.5% (小幅反彈)
        BigDecimal aggressivePremium = new BigDecimal("1.015");    // 積極: +1.5% (等待高點)

        // 情況 1: 超賣狀態 → 等待反彈，但不要等太高
        if (isOversold) {
            // 基礎價格：以當前價為基準
            BigDecimal conservativePrice = currentPrice;                                 // 當前價 (避免損失擴大)
            BigDecimal recommendedPrice = currentPrice.multiply(recommendedPremium);     // +0.5%
            BigDecimal aggressivePrice = currentPrice.multiply(new BigDecimal("1.02")); // +2%

            // 型態調整
            if (hasBearishPattern && isStrongDowntrend) {
                // 看跌型態：快速出場
                conservativePrice = currentPrice.multiply(new BigDecimal("0.995")); // -0.5%
                recommendedPrice = currentPrice;                                     // 當前價
                aggressivePrice = currentPrice.multiply(new BigDecimal("1.005"));   // +0.5%
            } else if (hasBullishPattern) {
                // 看漲型態：可等待更高反彈
                recommendedPrice = currentPrice.multiply(new BigDecimal("1.01")); // +1%
                aggressivePrice = currentPrice.multiply(new BigDecimal("1.025")); // +2.5%
            }

            result[0] = recommendedPrice;
            result[1] = conservativePrice;
            result[2] = aggressivePrice;
            return result;
        }

        // 情況 2: 價格在布林帶下半部 (< 0.4) → 等待反彈
        if (pricePosition != null && pricePosition.compareTo(new BigDecimal("0.4")) < 0) {
            BigDecimal conservativePrice = currentPrice;                                  // 當前價
            BigDecimal recommendedPrice = currentPrice.multiply(new BigDecimal("1.01")); // +1%
            BigDecimal aggressivePrice = currentPrice.multiply(new BigDecimal("1.02"));  // +2%

            // 型態調整
            if (hasBearishPattern) {
                conservativePrice = currentPrice.multiply(new BigDecimal("0.995")); // -0.5%
                recommendedPrice = currentPrice.multiply(new BigDecimal("1.005"));  // +0.5%
                aggressivePrice = currentPrice.multiply(new BigDecimal("1.015"));   // +1.5%
            } else if (hasBullishPattern && isStrongUptrend) {
                recommendedPrice = currentPrice.multiply(new BigDecimal("1.015")); // +1.5%
                aggressivePrice = currentPrice.multiply(new BigDecimal("1.025"));  // +2.5%
            }

            result[0] = recommendedPrice;
            result[1] = conservativePrice;
            result[2] = aggressivePrice;
            return result;
        }

        // 情況 3: 價格在布林帶上半部 (> 0.6) 或已突破上軌 → 立即賣出或等小幅反彈
        if (pricePosition != null && pricePosition.compareTo(new BigDecimal("0.6")) > 0) {
            // 檢查是否突破上軌
            boolean aboveUpperBand = indicators.getBbandsUpper() != null &&
                    currentPrice.compareTo(indicators.getBbandsUpper()) > 0;

            BigDecimal conservativePrice = currentPrice.multiply(conservativeDiscount);  // -1%
            BigDecimal recommendedPrice = currentPrice;                                   // 當前價
            BigDecimal aggressivePrice = currentPrice.multiply(new BigDecimal("1.01")); // +1%

            if (aboveUpperBand) {
                // 已突破上軌：建議立即出場或等小幅反彈
                conservativePrice = currentPrice.multiply(new BigDecimal("0.995")); // -0.5%
                recommendedPrice = currentPrice;                                     // 當前價
                aggressivePrice = currentPrice.multiply(new BigDecimal("1.005"));   // +0.5%
            }

            // 型態調整
            if (hasBearishPattern) {
                conservativePrice = currentPrice.multiply(new BigDecimal("0.985")); // -1.5%
                recommendedPrice = currentPrice.multiply(new BigDecimal("0.995"));  // -0.5%
                aggressivePrice = currentPrice;                                      // 當前價
            } else if (hasBullishPattern && isStrongUptrend) {
                recommendedPrice = currentPrice.multiply(new BigDecimal("1.005")); // +0.5%
                aggressivePrice = currentPrice.multiply(new BigDecimal("1.015"));  // +1.5%
            }

            result[0] = recommendedPrice;
            result[1] = conservativePrice;
            result[2] = aggressivePrice;
            return result;
        }

        // 情況 4: 強烈賣出訊號（3+ 個）→ 立即出場
        if (sellSignalCount >= 3) {
            BigDecimal conservativePrice = currentPrice.multiply(new BigDecimal("0.985")); // -1.5%
            BigDecimal recommendedPrice = currentPrice;                                     // 當前價
            BigDecimal aggressivePrice = currentPrice.multiply(new BigDecimal("1.005"));   // +0.5%

            // 型態調整
            if (hasBearishPattern) {
                conservativePrice = currentPrice.multiply(new BigDecimal("0.98"));  // -2%
                recommendedPrice = currentPrice.multiply(new BigDecimal("0.995")); // -0.5%
                aggressivePrice = currentPrice;                                     // 當前價
            } else if (hasBullishPattern) {
                recommendedPrice = currentPrice.multiply(new BigDecimal("1.005")); // +0.5%
                aggressivePrice = currentPrice.multiply(new BigDecimal("1.01"));   // +1%
            }

            result[0] = recommendedPrice;
            result[1] = conservativePrice;
            result[2] = aggressivePrice;
            return result;
        }

        // 情況 5: 一般情況或訊號不明確 → 等待小幅反彈
        BigDecimal conservativePrice = currentPrice.multiply(new BigDecimal("0.995")); // -0.5%
        BigDecimal recommendedPrice = currentPrice.multiply(new BigDecimal("1.005"));  // +0.5%
        BigDecimal aggressivePrice = currentPrice.multiply(new BigDecimal("1.015"));   // +1.5%

        // 型態調整
        if (hasBearishPattern) {
            conservativePrice = currentPrice.multiply(new BigDecimal("0.985")); // -1.5%
            recommendedPrice = currentPrice;                                     // 當前價
            aggressivePrice = currentPrice.multiply(new BigDecimal("1.005"));   // +0.5%
        } else if (hasBullishPattern) {
            recommendedPrice = currentPrice.multiply(new BigDecimal("1.01")); // +1%
            aggressivePrice = currentPrice.multiply(new BigDecimal("1.02"));  // +2%
        }

        result[0] = recommendedPrice;
        result[1] = conservativePrice;
        result[2] = aggressivePrice;
        return result;
    }

    /**
     * 計算智能買入價（整合K線型態和趨勢分析）- 更實際的價格計算
     */
    private BigDecimal[] calculateSmartEntryPrice(
            BigDecimal currentPrice,
            TechnicalAnalysisResult.TechnicalIndicators indicators,
            TechnicalAnalysisResult.SupportResistance sr,
            List<TechnicalAnalysisResult.TradingSignal> signals,
            List<String> patterns,
            String trendDescription) {

        // 返回 [建議價, 最佳價(保守), 可接受價(積極)]
        BigDecimal[] result = new BigDecimal[3];

        // 分析K線型態影響
        boolean hasBullishPattern = patterns.stream()
                .anyMatch(p -> p.contains("看漲") || p.contains("早晨") ||
                              p.contains("錘子") || p.contains("倒錘") ||
                              p.contains("反轉向上"));

        boolean hasBearishPattern = patterns.stream()
                .anyMatch(p -> p.contains("看跌") || p.contains("黃昏") ||
                              p.contains("流星") || p.contains("上吊") ||
                              p.contains("上漲結束"));

        // 分析趨勢強度
        boolean isStrongUptrend = trendDescription != null &&
                (trendDescription.contains("強勢上升") || trendDescription.contains("多頭格局"));

        boolean isStrongDowntrend = trendDescription != null &&
                (trendDescription.contains("弱勢下降") || trendDescription.contains("空頭格局"));

        // 1. 檢查是否超買
        boolean isOverbought =
                (indicators.getRsi12() != null && indicators.getRsi12().compareTo(new BigDecimal("70")) > 0) ||
                        (indicators.getKdK() != null && indicators.getKdK().compareTo(new BigDecimal("80")) > 0);

        // 2. 計算價格相對布林帶位置 (0=下軌, 0.5=中軌, 1=上軌)
        BigDecimal pricePosition = null;
        if (indicators.getBbandsUpper() != null && indicators.getBbandsLower() != null) {
            BigDecimal range = indicators.getBbandsUpper().subtract(indicators.getBbandsLower());
            if (range.compareTo(BigDecimal.ZERO) > 0) {
                pricePosition = currentPrice.subtract(indicators.getBbandsLower())
                        .divide(range, 4, BigDecimal.ROUND_HALF_UP);
            }
        }

        // 3. 計算買進訊號數量
        long buySignalCount = signals.stream()
                .filter(s -> "買進".equals(s.getDirection()))
                .count();

        // === 新的價格計算邏輯：更實際，以當前價為基準 ===

        // 基礎回調幅度設定
        BigDecimal conservativeDiscount = new BigDecimal("0.97");  // 保守: -3%
        BigDecimal recommendedDiscount = new BigDecimal("0.985"); // 建議: -1.5%
        BigDecimal aggressiveDiscount = new BigDecimal("0.995");  // 積極: -0.5%

        // 情況 1: 超買狀態 → 等待回調，但不要離當前價太遠
        if (isOverbought) {
            // 基礎價格：以當前價為基準計算回調
            BigDecimal conservativePrice = currentPrice.multiply(conservativeDiscount); // -3%
            BigDecimal recommendedPrice = currentPrice.multiply(recommendedDiscount);   // -1.5%
            BigDecimal aggressivePrice = currentPrice.multiply(aggressiveDiscount);     // -0.5%

            // 參考技術支撐位，但限制最大回調幅度
            if (sr.getSupport1() != null) {
                BigDecimal maxDiscount = currentPrice.multiply(new BigDecimal("0.95")); // 最多-5%
                if (sr.getSupport1().compareTo(maxDiscount) > 0) {
                    // 支撐位在合理範圍內，調整保守價
                    conservativePrice = sr.getSupport1();
                    recommendedPrice = conservativePrice.add(currentPrice).divide(new BigDecimal("2"), 2, BigDecimal.ROUND_HALF_UP);
                }
            }

            // 型態調整
            if (hasBullishPattern && isStrongUptrend) {
                // 看漲型態：可以接受更高價格
                aggressivePrice = currentPrice.multiply(new BigDecimal("1.005")); // 可追高0.5%
                recommendedPrice = currentPrice.multiply(new BigDecimal("0.995")); // 只回調0.5%
            } else if (hasBearishPattern) {
                // 看跌型態：等待更深回調
                aggressivePrice = currentPrice.multiply(new BigDecimal("0.98"));  // -2%
                recommendedPrice = currentPrice.multiply(new BigDecimal("0.975")); // -2.5%
            }

            result[0] = recommendedPrice;
            result[1] = conservativePrice;
            result[2] = aggressivePrice;
            return result;
        }

        // 情況 2: 價格在布林帶上半部 (> 0.6) → 等待小幅回調
        if (pricePosition != null && pricePosition.compareTo(new BigDecimal("0.6")) > 0) {
            BigDecimal conservativePrice = currentPrice.multiply(new BigDecimal("0.98")); // -2%
            BigDecimal recommendedPrice = currentPrice.multiply(new BigDecimal("0.99"));  // -1%
            BigDecimal aggressivePrice = currentPrice;                                    // 當前價

            // 型態調整
            if (hasBullishPattern && isStrongUptrend) {
                aggressivePrice = currentPrice.multiply(new BigDecimal("1.01")); // 可追高1%
                recommendedPrice = currentPrice.multiply(new BigDecimal("1.005")); // 可追高0.5%
            } else if (hasBearishPattern) {
                aggressivePrice = currentPrice.multiply(new BigDecimal("0.985")); // -1.5%
                recommendedPrice = currentPrice.multiply(new BigDecimal("0.975")); // -2.5%
            }

            result[0] = recommendedPrice;
            result[1] = conservativePrice;
            result[2] = aggressivePrice;
            return result;
        }

        // 情況 3: 價格在布林帶下半部 (< 0.4) → 可以立即買入或追高
        if (pricePosition != null && pricePosition.compareTo(new BigDecimal("0.4")) < 0) {
            BigDecimal conservativePrice = currentPrice.multiply(new BigDecimal("0.99"));  // -1%
            BigDecimal recommendedPrice = currentPrice;                                     // 當前價
            BigDecimal aggressivePrice = currentPrice.multiply(new BigDecimal("1.01"));    // +1%

            // 型態調整
            if (hasBullishPattern) {
                aggressivePrice = currentPrice.multiply(new BigDecimal("1.015")); // 可追高1.5%
                recommendedPrice = currentPrice.multiply(new BigDecimal("1.005")); // 稍高買入
            } else if (hasBearishPattern && isStrongDowntrend) {
                aggressivePrice = currentPrice.multiply(new BigDecimal("1.005")); // 追高降低
                recommendedPrice = currentPrice.multiply(new BigDecimal("0.995")); // 等待更低
            }

            result[0] = recommendedPrice;
            result[1] = conservativePrice;
            result[2] = aggressivePrice;
            return result;
        }

        // 情況 4: 強烈買進訊號（3+ 個買進訊號）→ 可以追高
        if (buySignalCount >= 3) {
            BigDecimal conservativePrice = currentPrice.multiply(new BigDecimal("0.985")); // -1.5%
            BigDecimal recommendedPrice = currentPrice;                                     // 當前價
            BigDecimal aggressivePrice = currentPrice.multiply(new BigDecimal("1.015"));   // +1.5%

            // 型態調整
            if (hasBullishPattern && isStrongUptrend) {
                aggressivePrice = currentPrice.multiply(new BigDecimal("1.02")); // 可追高2%
                recommendedPrice = currentPrice.multiply(new BigDecimal("1.01")); // 稍高買入
            } else if (hasBearishPattern) {
                aggressivePrice = currentPrice.multiply(new BigDecimal("1.005")); // 降至0.5%
                recommendedPrice = currentPrice.multiply(new BigDecimal("0.995")); // 稍等回調
            }

            result[0] = recommendedPrice;
            result[1] = conservativePrice;
            result[2] = aggressivePrice;
            return result;
        }

        // 情況 5: 一般情況或訊號不明確 → 等待小幅回調
        BigDecimal conservativePrice = currentPrice.multiply(new BigDecimal("0.98"));  // -2%
        BigDecimal recommendedPrice = currentPrice.multiply(new BigDecimal("0.99"));   // -1%
        BigDecimal aggressivePrice = currentPrice;                                      // 當前價

        // 型態調整
        if (hasBullishPattern) {
            aggressivePrice = currentPrice.multiply(new BigDecimal("1.005")); // 可追高0.5%
        } else if (hasBearishPattern) {
            aggressivePrice = currentPrice.multiply(new BigDecimal("0.985")); // -1.5%
            recommendedPrice = currentPrice.multiply(new BigDecimal("0.975")); // -2.5%
        }

        result[0] = recommendedPrice;
        result[1] = conservativePrice;
        result[2] = aggressivePrice;
        return result;
    }

    /**
     * 生成買入價格區間說明
     */
    private String generateEntryPriceExplanation(
            BigDecimal currentPrice,
            TechnicalAnalysisResult.TechnicalIndicators indicators,
            TechnicalAnalysisResult.SupportResistance sr,
            List<TechnicalAnalysisResult.TradingSignal> signals,
            BigDecimal[] prices) {

        StringBuilder explanation = new StringBuilder();

        // 檢查超買
        boolean isOverbought =
                (indicators.getRsi12() != null && indicators.getRsi12().compareTo(new BigDecimal("70")) > 0) ||
                (indicators.getKdK() != null && indicators.getKdK().compareTo(new BigDecimal("80")) > 0);

        // 計算布林帶位置
        BigDecimal pricePosition = null;
        if (indicators.getBbandsUpper() != null && indicators.getBbandsLower() != null) {
            BigDecimal range = indicators.getBbandsUpper().subtract(indicators.getBbandsLower());
            if (range.compareTo(BigDecimal.ZERO) > 0) {
                pricePosition = currentPrice.subtract(indicators.getBbandsLower())
                        .divide(range, 4, BigDecimal.ROUND_HALF_UP);
            }
        }

        // 統計買進訊號數量
        long buySignalCount = signals.stream()
                .filter(s -> "買進".equals(s.getDirection()))
                .count();

        // 根據情況生成說明
        if (isOverbought) {
            explanation.append("目前處於超買區（RSI>70 或 KD>80），建議等待股價回檔至支撐位再進場。");
            explanation.append(String.format("\n最佳買入價 %.2f（支撐位），可接受價 %.2f（中軌）。", prices[1], prices[2]));
        } else if (pricePosition != null && pricePosition.compareTo(new BigDecimal("0.6")) > 0) {
            explanation.append("股價位於布林帶上緣，建議等待回測布林中軌或 MA20。");
            explanation.append(String.format("\n建議買入價 %.2f（中軌），最佳價 %.2f（MA20）。", prices[0], prices[1]));
        } else if (pricePosition != null && pricePosition.compareTo(new BigDecimal("0.4")) < 0) {
            explanation.append("股價位於布林帶下緣，接近超賣區，可立即進場。");
            explanation.append(String.format("\n建議立即買入 %.2f，更佳價位 %.2f（下軌）。", prices[0], prices[1]));
        } else if (buySignalCount >= 3) {
            explanation.append(String.format("出現 %d 個強烈買進訊號，建議積極進場。", buySignalCount));
            explanation.append(String.format("\n建議立即買入 %.2f，可等待回測 %.2f（MA10）。", prices[0], prices[1]));
        } else {
            explanation.append("一般買進訊號，建議等待回測 MA10 或 MA20 再進場。");
            explanation.append(String.format("\n建議買入價 %.2f（MA10），最佳價 %.2f（MA20）。", prices[0], prices[1]));
        }

        return explanation.toString();
    }

    /**
     * 生成賣出價格區間說明
     */
    private String generateExitPriceExplanation(
            BigDecimal currentPrice,
            TechnicalAnalysisResult.TechnicalIndicators indicators,
            TechnicalAnalysisResult.SupportResistance sr,
            List<TechnicalAnalysisResult.TradingSignal> signals,
            BigDecimal[] prices) {

        StringBuilder explanation = new StringBuilder();

        // 檢查超賣
        boolean isOversold =
                (indicators.getRsi12() != null && indicators.getRsi12().compareTo(new BigDecimal("30")) < 0) ||
                (indicators.getKdK() != null && indicators.getKdK().compareTo(new BigDecimal("20")) < 0);

        // 計算布林帶位置
        BigDecimal pricePosition = null;
        if (indicators.getBbandsUpper() != null && indicators.getBbandsLower() != null) {
            BigDecimal range = indicators.getBbandsUpper().subtract(indicators.getBbandsLower());
            if (range.compareTo(BigDecimal.ZERO) > 0) {
                pricePosition = currentPrice.subtract(indicators.getBbandsLower())
                        .divide(range, 4, BigDecimal.ROUND_HALF_UP);
            }
        }

        // 統計賣出訊號數量
        long sellSignalCount = signals.stream()
                .filter(s -> "賣出".equals(s.getDirection()))
                .count();

        // 根據情況生成說明
        if (isOversold) {
            explanation.append("目前處於超賣區（RSI<30 或 KD<20），建議等待反彈至 MA5 或中軌再賣出。");
            explanation.append(String.format("\n建議賣出價 %.2f（MA5），最佳價 %.2f（中軌）。", prices[0], prices[2]));
        } else if (pricePosition != null && pricePosition.compareTo(new BigDecimal("0.4")) < 0) {
            explanation.append("股價位於布林帶下緣，建議等待反彈至下軌或 MA10 再出場。");
            explanation.append(String.format("\n建議賣出價 %.2f（MA10），最佳價 %.2f（下軌）。", prices[0], prices[2]));
        } else if (pricePosition != null && pricePosition.compareTo(new BigDecimal("0.6")) > 0) {
            explanation.append("股價位於布林帶上緣，接近壓力位，建議立即出場。");
            explanation.append(String.format("\n建議立即賣出 %.2f，保守價 %.2f（壓力位）。", prices[0], prices[1]));
        } else if (sellSignalCount >= 3) {
            explanation.append(String.format("出現 %d 個強烈賣出訊號，建議立即出場。", sellSignalCount));
            explanation.append(String.format("\n建議立即賣出 %.2f，可等待反彈至 %.2f（MA5）。", prices[0], prices[2]));
        } else {
            explanation.append("一般賣出訊號，建議等待反彈至 MA5 或中軌再出場。");
            explanation.append(String.format("\n建議賣出價 %.2f（MA5），最佳價 %.2f（中軌）。", prices[0], prices[2]));
        }

        return explanation.toString();
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

    // ==================== K線型態識別相關方法 ====================

    /**
     * 辨識 K 線型態
     */
    private List<String> identifyCandlestickPatterns(List<CandlestickData> candles) {
        List<String> patterns = new ArrayList<>();

        if (candles.size() < 3) {
            return patterns;
        }

        // 取最近3-5根K線
        int lastIdx = candles.size() - 1;
        CandlestickData today = candles.get(lastIdx);
        CandlestickData yesterday = candles.get(lastIdx - 1);
        CandlestickData dayBefore = candles.get(lastIdx - 2);

        // === 經典型態（嚴格條件）===

        // 1. 十字星
        if (isDoji(today)) {
            patterns.add("十字星");
        }

        // 2. 看漲吞沒
        if (isBullishEngulfing(yesterday, today)) {
            patterns.add("看漲吞沒");
        }

        // 3. 看跌吞沒
        if (isBearishEngulfing(yesterday, today)) {
            patterns.add("看跌吞沒");
        }

        // 4. 早晨之星
        if (isMorningStar(dayBefore, yesterday, today)) {
            patterns.add("早晨之星");
        }

        // 5. 黃昏之星
        if (isEveningStar(dayBefore, yesterday, today)) {
            patterns.add("黃昏之星");
        }

        // 6. 錘子線（放寬條件：不要求前一根必須是黑K）
        if (isHammer(today)) {
            // 判斷是在下跌趨勢中還是上漲趨勢中
            boolean inDowntrend = yesterday.getClose().compareTo(yesterday.getOpen()) < 0;
            if (inDowntrend) {
                patterns.add("錘子線");
            } else {
                patterns.add("上吊線");
            }
        }

        // 7. 流星線（放寬條件）
        if (isShootingStar(today)) {
            // 判斷是在上漲趨勢中還是下跌趨勢中
            boolean inUptrend = yesterday.getClose().compareTo(yesterday.getOpen()) > 0;
            if (inUptrend) {
                patterns.add("流星線");
            } else {
                patterns.add("倒錘線");
            }
        }

        // === 新增：常見型態（放寬條件）===

        // 8. 大陽線（實體 > 2%）
        BigDecimal bodyPercent = today.getClose().subtract(today.getOpen())
                .divide(today.getOpen(), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100")).abs();

        if (today.getClose().compareTo(today.getOpen()) > 0 &&
            bodyPercent.compareTo(new BigDecimal("2")) > 0) {
            patterns.add("長紅K");
        }

        // 9. 大陰線（實體 > 2%）
        if (today.getClose().compareTo(today.getOpen()) < 0 &&
            bodyPercent.compareTo(new BigDecimal("2")) > 0) {
            patterns.add("長黑K");
        }

        // 10. 連續上漲
        if (candles.size() >= 3) {
            boolean threeRising = true;
            for (int i = 0; i < 3; i++) {
                CandlestickData candle = candles.get(lastIdx - i);
                if (candle.getClose().compareTo(candle.getOpen()) <= 0) {
                    threeRising = false;
                    break;
                }
            }
            if (threeRising) {
                patterns.add("三連陽");
            }
        }

        // 11. 連續下跌
        if (candles.size() >= 3) {
            boolean threeFalling = true;
            for (int i = 0; i < 3; i++) {
                CandlestickData candle = candles.get(lastIdx - i);
                if (candle.getClose().compareTo(candle.getOpen()) >= 0) {
                    threeFalling = false;
                    break;
                }
            }
            if (threeFalling) {
                patterns.add("三連陰");
            }
        }

        // 12. 長上影線（上影線 > 實體1.5倍，不一定要是流星）
        BigDecimal todayBody = today.getClose().subtract(today.getOpen()).abs();
        BigDecimal todayUpperShadow = today.getHigh().subtract(today.getOpen().max(today.getClose()));
        if (todayBody.compareTo(BigDecimal.ZERO) > 0 &&
            todayUpperShadow.compareTo(todayBody.multiply(new BigDecimal("1.5"))) > 0) {
            if (!patterns.contains("流星線")) {
                patterns.add("長上影線");
            }
        }

        // 13. 長下影線（下影線 > 實體1.5倍，不一定要是錘子）
        BigDecimal todayLowerShadow = today.getOpen().min(today.getClose()).subtract(today.getLow());
        if (todayBody.compareTo(BigDecimal.ZERO) > 0 &&
            todayLowerShadow.compareTo(todayBody.multiply(new BigDecimal("1.5"))) > 0) {
            if (!patterns.contains("錘子線") && !patterns.contains("上吊線")) {
                patterns.add("長下影線");
            }
        }

        // 如果沒有任何型態，至少給出當前K線的基本描述
        if (patterns.isEmpty()) {
            if (today.getClose().compareTo(today.getOpen()) > 0) {
                patterns.add("小陽線");
            } else if (today.getClose().compareTo(today.getOpen()) < 0) {
                patterns.add("小陰線");
            } else {
                patterns.add("平盤");
            }
        }

        return patterns;
    }

    /**
     * 判斷是否為十字星
     */
    private boolean isDoji(CandlestickData candle) {
        BigDecimal bodySize = candle.getClose().subtract(candle.getOpen()).abs();
        BigDecimal range = candle.getHigh().subtract(candle.getLow());
        if (range.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        BigDecimal ratio = bodySize.divide(range, 4, BigDecimal.ROUND_HALF_UP);
        return ratio.compareTo(new BigDecimal("0.1")) < 0;
    }

    /**
     * 判斷是否為看漲吞沒
     */
    private boolean isBullishEngulfing(CandlestickData prev, CandlestickData curr) {
        // 前一根是黑K
        boolean prevBearish = prev.getClose().compareTo(prev.getOpen()) < 0;
        // 當前是紅K
        boolean currBullish = curr.getClose().compareTo(curr.getOpen()) > 0;
        // 當前K線完全吞沒前一根
        boolean engulfs = curr.getOpen().compareTo(prev.getClose()) <= 0 &&
                curr.getClose().compareTo(prev.getOpen()) >= 0;

        return prevBearish && currBullish && engulfs;
    }

    /**
     * 判斷是否為看跌吞沒
     */
    private boolean isBearishEngulfing(CandlestickData prev, CandlestickData curr) {
        // 前一根是紅K
        boolean prevBullish = prev.getClose().compareTo(prev.getOpen()) > 0;
        // 當前是黑K
        boolean currBearish = curr.getClose().compareTo(curr.getOpen()) < 0;
        // 當前K線完全吞沒前一根
        boolean engulfs = curr.getOpen().compareTo(prev.getClose()) >= 0 &&
                curr.getClose().compareTo(prev.getOpen()) <= 0;

        return prevBullish && currBearish && engulfs;
    }

    /**
     * 判斷是否為早晨之星
     */
    private boolean isMorningStar(CandlestickData first, CandlestickData second, CandlestickData third) {
        // 第一根是黑K
        boolean firstBearish = first.getClose().compareTo(first.getOpen()) < 0;
        // 第二根是小實體（十字星或紡錘線）
        boolean secondSmall = isDoji(second) || isSpinningTop(second);
        // 第三根是紅K
        boolean thirdBullish = third.getClose().compareTo(third.getOpen()) > 0;
        // 第三根收盤價深入第一根實體
        boolean penetrates = third.getClose().compareTo(first.getOpen().add(first.getClose()).divide(new BigDecimal("2"), 4, BigDecimal.ROUND_HALF_UP)) > 0;

        return firstBearish && secondSmall && thirdBullish && penetrates;
    }

    /**
     * 判斷是否為黃昏之星
     */
    private boolean isEveningStar(CandlestickData first, CandlestickData second, CandlestickData third) {
        // 第一根是紅K
        boolean firstBullish = first.getClose().compareTo(first.getOpen()) > 0;
        // 第二根是小實體
        boolean secondSmall = isDoji(second) || isSpinningTop(second);
        // 第三根是黑K
        boolean thirdBearish = third.getClose().compareTo(third.getOpen()) < 0;
        // 第三根收盤價深入第一根實體
        boolean penetrates = third.getClose().compareTo(first.getOpen().add(first.getClose()).divide(new BigDecimal("2"), 4, BigDecimal.ROUND_HALF_UP)) < 0;

        return firstBullish && secondSmall && thirdBearish && penetrates;
    }

    /**
     * 判斷是否為紡錘線
     */
    private boolean isSpinningTop(CandlestickData candle) {
        BigDecimal bodySize = candle.getClose().subtract(candle.getOpen()).abs();
        BigDecimal range = candle.getHigh().subtract(candle.getLow());
        if (range.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        BigDecimal ratio = bodySize.divide(range, 4, BigDecimal.ROUND_HALF_UP);
        return ratio.compareTo(new BigDecimal("0.3")) < 0 && ratio.compareTo(new BigDecimal("0.1")) > 0;
    }

    /**
     * 判斷是否為錘子線
     */
    private boolean isHammer(CandlestickData candle) {
        BigDecimal bodySize = candle.getClose().subtract(candle.getOpen()).abs();
        BigDecimal range = candle.getHigh().subtract(candle.getLow());
        BigDecimal lowerShadow = candle.getOpen().min(candle.getClose()).subtract(candle.getLow());
        BigDecimal upperShadow = candle.getHigh().subtract(candle.getOpen().max(candle.getClose()));

        if (range.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }

        // 小實體
        boolean smallBody = bodySize.divide(range, 4, BigDecimal.ROUND_HALF_UP).compareTo(new BigDecimal("0.3")) < 0;
        // 長下影線（至少是實體的2倍）
        boolean longLowerShadow = lowerShadow.compareTo(bodySize.multiply(new BigDecimal("2"))) > 0;
        // 短上影線或無上影線
        boolean shortUpperShadow = upperShadow.compareTo(bodySize) < 0;

        return smallBody && longLowerShadow && shortUpperShadow;
    }

    /**
     * 判斷是否為上吊線（形狀類似錘子線，但出現在上升趨勢）
     */
    private boolean isHangingMan(CandlestickData candle) {
        return isHammer(candle);  // 形狀相同，只是位置不同
    }

    /**
     * 判斷是否為流星線
     */
    private boolean isShootingStar(CandlestickData candle) {
        BigDecimal bodySize = candle.getClose().subtract(candle.getOpen()).abs();
        BigDecimal range = candle.getHigh().subtract(candle.getLow());
        BigDecimal upperShadow = candle.getHigh().subtract(candle.getOpen().max(candle.getClose()));
        BigDecimal lowerShadow = candle.getOpen().min(candle.getClose()).subtract(candle.getLow());

        if (range.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }

        // 小實體
        boolean smallBody = bodySize.divide(range, 4, BigDecimal.ROUND_HALF_UP).compareTo(new BigDecimal("0.3")) < 0;
        // 長上影線（至少是實體的2倍）
        boolean longUpperShadow = upperShadow.compareTo(bodySize.multiply(new BigDecimal("2"))) > 0;
        // 短下影線或無下影線
        boolean shortLowerShadow = lowerShadow.compareTo(bodySize) < 0;

        return smallBody && longUpperShadow && shortLowerShadow;
    }

    /**
     * 判斷是否為倒錘線
     */
    private boolean isInvertedHammer(CandlestickData candle) {
        return isShootingStar(candle);  // 形狀相同，只是位置不同
    }

    /**
     * 生成趨勢描述
     */
    private String generateTrendDescription(List<CandlestickData> data, int index,
                                           TechnicalAnalysisResult.TechnicalIndicators indicators) {
        if (data.size() < 20) {
            return "數據不足，無法判斷趨勢";
        }

        CandlestickData current = data.get(index);
        BigDecimal currentPrice = current.getClose();

        // 檢查均線排列
        boolean bullishAlignment = false;
        boolean bearishAlignment = false;

        if (indicators.getMa5() != null && indicators.getMa10() != null &&
                indicators.getMa20() != null && indicators.getMa60() != null) {

            // 多頭排列: MA5 > MA10 > MA20 > MA60
            bullishAlignment = indicators.getMa5().compareTo(indicators.getMa10()) > 0 &&
                    indicators.getMa10().compareTo(indicators.getMa20()) > 0 &&
                    indicators.getMa20().compareTo(indicators.getMa60()) > 0;

            // 空頭排列: MA5 < MA10 < MA20 < MA60
            bearishAlignment = indicators.getMa5().compareTo(indicators.getMa10()) < 0 &&
                    indicators.getMa10().compareTo(indicators.getMa20()) < 0 &&
                    indicators.getMa20().compareTo(indicators.getMa60()) < 0;
        }

        // 計算價格相對於均線的位置
        int aboveCount = 0;
        if (indicators.getMa5() != null && currentPrice.compareTo(indicators.getMa5()) > 0) aboveCount++;
        if (indicators.getMa10() != null && currentPrice.compareTo(indicators.getMa10()) > 0) aboveCount++;
        if (indicators.getMa20() != null && currentPrice.compareTo(indicators.getMa20()) > 0) aboveCount++;
        if (indicators.getMa60() != null && currentPrice.compareTo(indicators.getMa60()) > 0) aboveCount++;

        // 計算近期漲跌幅 (5日)
        BigDecimal priceChange = BigDecimal.ZERO;
        if (index >= 5) {
            BigDecimal oldPrice = data.get(index - 5).getClose();
            priceChange = currentPrice.subtract(oldPrice)
                    .divide(oldPrice, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        // 生成趨勢描述
        StringBuilder trend = new StringBuilder();

        if (bullishAlignment && aboveCount >= 3) {
            trend.append("強勢上升趨勢");
            if (priceChange.compareTo(new BigDecimal("5")) > 0) {
                trend.append("，短期漲幅較大");
            }
        } else if (bearishAlignment && aboveCount <= 1) {
            trend.append("弱勢下降趨勢");
            if (priceChange.compareTo(new BigDecimal("-5")) < 0) {
                trend.append("，短期跌幅較大");
            }
        } else if (aboveCount >= 3) {
            trend.append("多頭格局");
            if (priceChange.compareTo(BigDecimal.ZERO) > 0) {
                trend.append("，價格站穩均線之上");
            } else {
                trend.append("，但短期有回檔壓力");
            }
        } else if (aboveCount <= 1) {
            trend.append("空頭格局");
            if (priceChange.compareTo(BigDecimal.ZERO) < 0) {
                trend.append("，價格受均線壓制");
            } else {
                trend.append("，但短期有反彈跡象");
            }
        } else {
            trend.append("盤整格局");
            if (indicators.getBbandsUpper() != null && indicators.getBbandsLower() != null) {
                BigDecimal bandWidth = indicators.getBbandsUpper().subtract(indicators.getBbandsLower());
                BigDecimal bandWidthRatio = bandWidth.divide(indicators.getBbandsMiddle(), 4, BigDecimal.ROUND_HALF_UP);
                if (bandWidthRatio.compareTo(new BigDecimal("0.05")) < 0) {
                    trend.append("，波動收斂，可能醞釀突破");
                } else {
                    trend.append("，等待方向選擇");
                }
            }
        }

        return trend.toString();
    }

    /**
     * 分析量價關係
     */
    private String analyzePriceVolume(List<CandlestickData> data, int index) {
        if (data.size() < 6 || index < 5) {
            return "數據不足，無法分析量價關係";
        }

        CandlestickData today = data.get(index);
        CandlestickData yesterday = data.get(index - 1);

        // 計算平均成交量 (5日)
        long avgVolumeLong = 0;
        for (int i = index - 5; i < index; i++) {
            avgVolumeLong += data.get(i).getVolume();
        }
        avgVolumeLong = avgVolumeLong / 5;

        // 今日成交量
        long todayVolume = today.getVolume();

        // 今日漲跌
        BigDecimal priceChange = today.getClose().subtract(today.getOpen());
        boolean isPriceUp = priceChange.compareTo(BigDecimal.ZERO) > 0;
        boolean isPriceDown = priceChange.compareTo(BigDecimal.ZERO) < 0;

        // 量能變化
        boolean isVolumeIncreased = todayVolume > (avgVolumeLong * 1.2);
        boolean isVolumeDecreased = todayVolume < (avgVolumeLong * 0.8);

        // 分析量價配合
        StringBuilder analysis = new StringBuilder();

        if (isPriceUp && isVolumeIncreased) {
            analysis.append("價漲量增，多頭強勢");
            // 檢查是否連續放量
            if (yesterday.getVolume() > avgVolumeLong) {
                analysis.append("，連續放量上攻");
            }
        } else if (isPriceUp && isVolumeDecreased) {
            analysis.append("價漲量縮，上漲動能不足");
            analysis.append("，小心追高風險");
        } else if (isPriceDown && isVolumeIncreased) {
            analysis.append("價跌量增，空頭出籠");
            analysis.append("，殺盤力道強勁");
        } else if (isPriceDown && isVolumeDecreased) {
            analysis.append("價跌量縮，賣壓趨緩");
            analysis.append("，可能止跌");
        } else if (isVolumeDecreased) {
            analysis.append("成交清淡，市場觀望");
        } else if (isVolumeIncreased) {
            analysis.append("成交放大，市場關注度提升");
        } else {
            analysis.append("量能平穩，持續觀察");
        }

        // 額外分析：量價背離
        if (index >= 10) {
            // 檢查最近5天價格和成交量的趨勢
            int priceUpDays = 0;
            int volumeUpDays = 0;

            for (int i = index - 4; i <= index; i++) {
                if (data.get(i).getClose().compareTo(data.get(i - 1).getClose()) > 0) {
                    priceUpDays++;
                }
                if (data.get(i).getVolume().compareTo(data.get(i - 1).getVolume()) > 0) {
                    volumeUpDays++;
                }
            }

            // 價漲量縮背離 (連續上漲但成交量萎縮)
            if (priceUpDays >= 4 && volumeUpDays <= 2) {
                analysis.append("，出現量價背離（價漲量縮），上漲動能衰竭");
            }
            // 價跌量縮背離 (連續下跌但成交量萎縮)
            else if (priceUpDays <= 1 && volumeUpDays <= 2) {
                analysis.append("，出現量價背離（價跌量縮），下跌動能減弱");
            }
        }

        return analysis.toString();
    }
}
