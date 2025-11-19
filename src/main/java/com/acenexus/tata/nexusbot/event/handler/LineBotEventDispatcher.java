package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.acenexus.tata.nexusbot.service.MessageService;
import com.linecorp.bot.model.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * LINE Bot 事件分發器
 * 使用責任鏈模式（Chain of Responsibility）將事件分發給合適的 Handler 處理
 */
@Component
public class LineBotEventDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(LineBotEventDispatcher.class);

    private final List<LineBotEventHandler> handlers;
    private final MessageService messageService;

    /**
     * Constructor
     * Spring 自動注入所有 LineBotEventHandler 並按優先級排序
     *
     * @param handlers       所有已註冊的事件處理器
     * @param messageService 訊息發送服務
     */
    public LineBotEventDispatcher(List<LineBotEventHandler> handlers, MessageService messageService) {
        // 按優先級排序（數字越小優先級越高）
        this.handlers = handlers.stream()
                .sorted(Comparator.comparingInt(LineBotEventHandler::getPriority))
                .toList();
        this.messageService = messageService;

        logger.info("Registered {} event handlers (sorted by priority)", handlers.size());
        handlers.forEach(handler ->
                logger.debug("  - {} (priority: {})",
                        handler.getClass().getSimpleName(),
                        handler.getPriority())
        );
    }

    /**
     * 分發事件給合適的 Handler 處理
     * 使用責任鏈模式尋找並執行第一個可處理的 Handler
     *
     * @param event 統一的 LINE Bot 事件
     */
    public void dispatch(LineBotEvent event) {
        logger.debug("Dispatching event: type={}, roomId={}, roomType={}, userId={}, replyToken={}", event.getEventType(), event.getRoomId(), event.getRoomType(), event.getUserId(), event.getReplyToken().substring(0, Math.min(8, event.getReplyToken().length())) + "...");

        try {
            // 依序詢問每個 Handler
            for (LineBotEventHandler handler : handlers) {
                try {
                    if (handler.canHandle(event)) {
                        logger.debug("Handler {} can handle event", handler.getClass().getSimpleName());

                        // 執行處理並取得回覆訊息
                        Message message = handler.handle(event);

                        // 發送訊息（如果有）
                        if (message != null) {
                            messageService.sendMessage(event.getReplyToken(), message);
                        }

                        // 已處理完成，不再詢問其他 Handler
                        return;
                    }
                } catch (Exception e) {
                    logger.error("Error in handler {}: {}", handler.getClass().getSimpleName(), e.getMessage(), e);
                    // 繼續嘗試下一個 Handler
                }
            }

            // 沒有任何 Handler 可以處理
            logger.warn("No handler found for event: type={}, roomId={}, roomType={}, userId={}", event.getEventType(), event.getRoomId(), event.getRoomType(), event.getUserId());

        } catch (Exception e) {
            logger.error("Fatal error dispatching event: {}", e.getMessage(), e);
        }
    }

    /**
     * 判斷是否應該發送預設訊息
     * 只有文字訊息和 Postback 需要預設回覆
     */
    private boolean shouldSendDefaultMessage(LineBotEvent event) {
        return switch (event.getEventType()) {
            case TEXT_MESSAGE, POSTBACK -> true;
            default -> false;
        };
    }
}
