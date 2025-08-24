package com.acenexus.tata.nexusbot.template;

/**
 * UI 常數管理 - 統一的配色和尺寸管理
 */
public final class UIConstants {

    private UIConstants() {
    }

    // ============= 配色管理 =============
    public static final class Colors {
        // 主要配色 - Instagram 漸層風格
        public static final String PRIMARY = "#E4405F";
        public static final String SUCCESS = "#00C896";
        public static final String INFO = "#5B51D8";
        public static final String ERROR = "#FF6B6B";
        public static final String SECONDARY = "#95A5A6";

        // 背景色彩
        public static final String BACKGROUND = "#FAFAFA";   // 淺灰背景
        public static final String CARD_BACKGROUND = "#FFFFFF"; // 卡片背景

        // 文字色彩
        public static final String TEXT_PRIMARY = "#1D1D1F";    // 主要文字
        public static final String TEXT_SECONDARY = "#8E8E93";  // 次要文字
        public static final String TEXT_DISABLED = "#C7C7CC";   // 禁用文字

        // 分隔線色彩
        public static final String SEPARATOR = "#E5E5EA";       // 分隔線
    }

    // ============= 尺寸管理 =============
    public static final class Sizes {
        // 間距
        public static final String SPACING_XS = "4px";
        public static final String SPACING_SM = "8px";
        public static final String SPACING_MD = "12px";
        public static final String SPACING_LG = "16px";
        public static final String SPACING_XL = "20px";
        public static final String SPACING_XXL = "24px";
    }

    // ============= 圖標管理 =============
    public static final class Icons {
        public static final String AI = "🤖";
        public static final String HOME = "🏠";
        public static final String INFO = "ℹ️";
        public static final String ABOUT = "📖";
        public static final String SUCCESS = "✅";
        public static final String ERROR = "❌";
        public static final String IMAGE = "📸";
        public static final String VIDEO = "🎥";
        public static final String AUDIO = "🎵";
        public static final String FILE = "📁";
        public static final String STICKER = "😊";
        public static final String STAR = "✨";
        public static final String MODEL = "⚙️";
        public static final String BRAIN = "🧠";
        public static final String LIGHTNING = "⚡";
        public static final String ROCKET = "🚀";
        public static final String CRYSTAL = "💎";
    }
}