package com.acenexus.tata.nexusbot.constants;

import java.time.format.DateTimeFormatter;

public final class TimeFormatters {
    
    private TimeFormatters() {
        // 防止實例化
    }
    
    /**
     * 標準時間格式：yyyy-MM-dd HH:mm
     * 用於提醒時間顯示、用戶輸入驗證等
     */
    public static final DateTimeFormatter STANDARD_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
}