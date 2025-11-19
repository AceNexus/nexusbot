package com.acenexus.tata.nexusbot.event;

import com.acenexus.tata.nexusbot.event.converter.EventConverterRegistry;
import com.acenexus.tata.nexusbot.event.handler.LineBotEventDispatcher;
import com.acenexus.tata.nexusbot.exception.UnsupportedEventException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * LINE Bot 事件協調者
 * 負責統一處理 LINE Webhook 事件的流程協調
 */
@Service
@RequiredArgsConstructor
public class LineBotEventCoordinator {
    private static final Logger logger = LoggerFactory.getLogger(LineBotEventCoordinator.class);

    private final EventConverterRegistry converterRegistry;
    private final LineBotEventDispatcher dispatcher;

    /**
     * 處理 LINE Webhook 事件陣列
     * 每個事件獨立處理，單一事件失敗不影響其他事件
     *
     * @param events LINE Webhook 事件陣列
     */
    public void processWebhookEvents(JsonNode events) {
        if (events == null || !events.isArray()) {
            logger.warn("Invalid events payload: not an array");
            return;
        }

        logger.debug("Processing {} events", events.size());

        for (JsonNode event : events) {
            processEvent(event);
        }
    }

    /**
     * 處理單一事件
     * 錯誤處理確保單一事件失敗不影響其他事件
     *
     * @param event 單一 LINE Webhook 事件
     */
    private void processEvent(JsonNode event) {
        try {
            // Step 1: 轉換事件
            LineBotEvent lineBotEvent = converterRegistry.convert(event);
            logger.debug("Converted event: type={}, roomId={}, roomType={}, userId={}", lineBotEvent.getEventType(), lineBotEvent.getRoomId(), lineBotEvent.getRoomType(), lineBotEvent.getUserId());

            // Step 2: 分發事件
            dispatcher.dispatch(lineBotEvent);

        } catch (UnsupportedEventException e) {
            logger.warn("Unsupported event type: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing event: {}", e.getMessage(), e);
        }
    }
}
