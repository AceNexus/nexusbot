package com.acenexus.tata.nexusbot.event.converter;

import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.acenexus.tata.nexusbot.exception.UnsupportedEventException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 事件轉換器註冊中心
 * 使用責任鏈模式（Chain of Responsibility）依序嘗試所有 Converter
 */
@Component
public class EventConverterRegistry {
    private static final Logger logger = LoggerFactory.getLogger(EventConverterRegistry.class);

    private final List<EventConverter> converters;

    /**
     * Constructor
     * Spring 自動注入所有 EventConverter 實作
     *
     * @param converters 所有已註冊的事件轉換器
     */
    public EventConverterRegistry(List<EventConverter> converters) {
        this.converters = converters;
        logger.info("Registered {} event converters", converters.size());
    }

    /**
     * 轉換事件為統一的 LineBotEvent
     * 使用責任鏈模式尋找合適的 Converter
     *
     * @param event LINE Webhook 原始事件 JSON
     * @return 轉換後的 LineBotEvent
     * @throws UnsupportedEventException 當沒有 Converter 可以處理此事件時
     */
    public LineBotEvent convert(JsonNode event) {
        String eventType = event.path("type").asText();

        for (EventConverter converter : converters) {
            if (converter.canConvert(event)) {
                logger.debug("Converting event type '{}' using {}", eventType, converter.getClass().getSimpleName());
                return converter.convert(event);
            }
        }

        String errorMsg = String.format("No converter found for event type: %s", eventType);
        logger.error(errorMsg);
        throw new UnsupportedEventException(errorMsg);
    }
}
