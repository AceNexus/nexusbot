package com.acenexus.tata.nexusbot.dto;

/**
 * 內外盤類型
 */
public enum TickType {

    /**
     * 外盤（成交價 >= 賣價，主動買進）
     */
    BUY("外盤"),

    /**
     * 內盤（成交價 <= 買價，主動賣出）
     */
    SELL("內盤"),

    /**
     * 中性（介於買賣價之間）
     */
    NEUTRAL("中性");

    private final String displayName;

    TickType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
