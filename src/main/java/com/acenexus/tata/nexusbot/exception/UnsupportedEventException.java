package com.acenexus.tata.nexusbot.exception;

/**
 * 不支援的事件類型異常
 * 當收到無法處理的 LINE Webhook 事件時拋出
 */
public class UnsupportedEventException extends RuntimeException {
    public UnsupportedEventException(String message) {
        super(message);
    }

    public UnsupportedEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
