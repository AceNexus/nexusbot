package com.acenexus.tata.nexusbot.template;

public final class UIConstants {

    private UIConstants() {
    }

    public static final class Colors {
        public static final String SUCCESS = "#16A34A";
        public static final String ERROR = "#DC2626";
        public static final String BACKGROUND = "#F8FAFC";
        public static final String PRIMARY_LIGHT = "#EFF6FF";
        public static final String TEXT_PRIMARY = "#1E293B";
        public static final String TEXT_SECONDARY = "#64748B";
        public static final String BORDER = "#E2E8F0";
    }

    public static final class Spacing {
        public static final String SM = "8px";
    }

    public static final class BorderRadius {
        public static final String SM = "4px";
        public static final String MD = "8px";
    }

    public static final class Button {
        // 統一使用適合 SECONDARY 樣式的顏色，確保協調性
        public static final String PRIMARY = "#E5E7EB";
        public static final String SECONDARY = "#E5E7EB";
        public static final String SUCCESS = "#E5E7EB";
        public static final String DANGER = "#E5E7EB";
        public static final String WARNING = "#E5E7EB";
        public static final String INFO = "#E5E7EB";

        // 狀態按鈕 - 選中時使用強調色
        public static final String SELECTED = "#3B82F6";
        public static final String UNSELECTED = "#E5E7EB";
    }

    public static final class Status {
        public static final String SUCCESS_BACKGROUND = "#F0FDF4";
        public static final String WARNING_BACKGROUND = "#FFF7ED";
        public static final String ERROR_BACKGROUND = "#FEF2F2";
        public static final String INFO_BACKGROUND = "#EFF6FF";

        public static final String SUCCESS_BORDER = "#16A34A";
        public static final String WARNING_BORDER = "#EA580C";
        public static final String ERROR_BORDER = "#DC2626";
        public static final String INFO_BORDER = "#3B82F6";
    }
}