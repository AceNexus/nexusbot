package com.acenexus.tata.nexusbot.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * K 線型態枚舉
 * 包含型態名稱、說明、可靠度
 */
@Getter
@AllArgsConstructor
public enum CandlestickPattern {

    // ========== 反轉型態（高可靠度） ==========
    BULLISH_ENGULFING(
            "看漲吞沒",
            "大陽線完全吞沒前一根陰線，表示多方力量強勢反攻，常出現在下跌趨勢末端，是重要的底部反轉訊號。",
            Reliability.HIGH,
            true
    ),

    BEARISH_ENGULFING(
            "看跌吞沒",
            "大陰線完全吞沒前一根陽線，表示空方力量掌控局面，常出現在上漲趨勢末端，是重要的頂部反轉訊號。",
            Reliability.HIGH,
            false
    ),

    MORNING_STAR(
            "早晨之星",
            "下跌後出現跳空十字星再拉陽線，三根K線組合形成底部反轉訊號，可靠度高，建議等待確認後進場。",
            Reliability.HIGH,
            true
    ),

    EVENING_STAR(
            "黃昏之星",
            "上漲後出現跳空十字星再殺陰線，三根K線組合形成頂部反轉訊號，可靠度高，建議獲利了結或減碼。",
            Reliability.HIGH,
            false
    ),

    PIERCING_LINE(
            "曙光初現",
            "陽線深入前陰線實體1/2以上，顯示多方開始反攻，常出現在下跌趨勢末端，是反彈訊號。",
            Reliability.HIGH,
            true
    ),

    DARK_CLOUD_COVER(
            "烏雲罩頂",
            "陰線深入前陽線實體1/2以下，顯示空方開始壓制，常出現在上漲趨勢末端，是回檔訊號。",
            Reliability.HIGH,
            false
    ),

    // ========== 中等可靠度型態 ==========
    HAMMER(
            "錘子線",
            "長下影線（至少為實體2倍）配合短上影線，顯示下方有強力買盤支撐，常出現在底部，是反彈訊號。",
            Reliability.MEDIUM,
            true
    ),

    HANGING_MAN(
            "上吊線",
            "外型類似錘子線但出現在上漲趨勢，長下影線表示下方賣壓增加，可能是頂部訊號，需觀察後續確認。",
            Reliability.MEDIUM,
            false
    ),

    SHOOTING_STAR(
            "流星線",
            "長上影線（至少為實體2倍）配合短下影線，顯示上方賣壓沉重，常出現在頂部，是回檔訊號。",
            Reliability.MEDIUM,
            false
    ),

    INVERTED_HAMMER(
            "倒錘線",
            "外型類似流星線但出現在下跌趨勢，長上影線表示多方開始試探，可能是底部訊號，需觀察後續確認。",
            Reliability.MEDIUM,
            true
    ),

    DOJI(
            "十字星",
            "開盤價與收盤價幾乎相同（實體<總範圍10%），表示多空拉鋸僵持，常是趨勢即將反轉的前兆。",
            Reliability.MEDIUM,
            null  // 中性
    ),

    DRAGONFLY_DOJI(
            "蜻蜓線",
            "T字型十字星，長下影線無上影線，顯示賣壓遇強力買盤支撐，在底部出現是強烈反彈訊號。",
            Reliability.MEDIUM,
            true
    ),

    GRAVESTONE_DOJI(
            "墓碑線",
            "倒T字型十字星，長上影線無下影線，顯示買氣遇強力賣壓壓制，在頂部出現是強烈回檔訊號。",
            Reliability.MEDIUM,
            false
    ),

    // ========== 趨勢延續型態 ==========
    LONG_BULLISH(
            "長紅K",
            "實體較大的陽線（漲幅>2%），表示多方力量強勁，若出現在上漲趨勢中代表趨勢延續。",
            Reliability.MEDIUM,
            true
    ),

    LONG_BEARISH(
            "長黑K",
            "實體較大的陰線（跌幅>2%），表示空方力量強勁，若出現在下跌趨勢中代表趨勢延續。",
            Reliability.MEDIUM,
            false
    ),

    THREE_WHITE_SOLDIERS(
            "三連陽",
            "連續三根陽線且一根比一根高，表示多方持續進攻，趨勢強勁，常出現在底部築底完成後。",
            Reliability.MEDIUM,
            true
    ),

    THREE_BLACK_CROWS(
            "三連陰",
            "連續三根陰線且一根比一根低，表示空方持續壓制，趨勢疲弱，常出現在頂部築頂完成後。",
            Reliability.MEDIUM,
            false
    ),

    // ========== 影線型態（低可靠度，需結合其他指標） ==========
    LONG_UPPER_SHADOW(
            "長上影線",
            "上影線長度超過實體1.5倍，表示上方遇到賣壓，多方攻擊受阻，短期可能回檔整理。",
            Reliability.LOW,
            false
    ),

    LONG_LOWER_SHADOW(
            "長下影線",
            "下影線長度超過實體1.5倍，表示下方有買盤支撐，空方攻擊受阻，短期可能反彈。",
            Reliability.LOW,
            true
    ),

    SPINNING_TOP(
            "紡錘線",
            "上下影線都較長且相當，實體較小，表示多空拉鋸激烈但勢均力敵，趨勢可能即將改變。",
            Reliability.LOW,
            null  // 中性
    );

    private final String name;              // 型態名稱
    private final String description;       // 詳細說明
    private final Reliability reliability;  // 可靠度
    private final Boolean bullish;          // true=看漲, false=看跌, null=中性

    /**
     * 可靠度枚舉
     */
    @Getter
    @AllArgsConstructor
    public enum Reliability {
        HIGH("高"),
        MEDIUM("中"),
        LOW("低");

        private final String label;
    }

    /**
     * 根據型態名稱查找
     *
     * @param name 型態名稱
     * @return 對應的型態枚舉，找不到則返回 null
     */
    public static CandlestickPattern findByName(String name) {
        if (name == null) {
            return null;
        }

        for (CandlestickPattern pattern : values()) {
            if (pattern.getName().equals(name)) {
                return pattern;
            }
        }
        return null;
    }

    /**
     * 判斷是否為看漲型態
     */
    public boolean isBullish() {
        return Boolean.TRUE.equals(bullish);
    }

    /**
     * 判斷是否為看跌型態
     */
    public boolean isBearish() {
        return Boolean.FALSE.equals(bullish);
    }

    /**
     * 判斷是否為中性型態
     */
    public boolean isNeutral() {
        return bullish == null;
    }
}
