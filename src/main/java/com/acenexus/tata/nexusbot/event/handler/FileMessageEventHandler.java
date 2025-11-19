package com.acenexus.tata.nexusbot.event.handler;

import com.acenexus.tata.nexusbot.event.EventType;
import com.acenexus.tata.nexusbot.event.LineBotEvent;
import com.acenexus.tata.nexusbot.service.MessageProcessorService;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 處理檔案訊息事件
 */
@Component
@RequiredArgsConstructor
public class FileMessageEventHandler implements LineBotEventHandler {
    private static final Logger logger = LoggerFactory.getLogger(FileMessageEventHandler.class);

    private final MessageProcessorService messageProcessorService;

    @Override
    public boolean canHandle(LineBotEvent event) {
        return event.getEventType() == EventType.FILE_MESSAGE;
    }

    @Override
    public Message handle(LineBotEvent event) {
        String messageId = event.getPayloadString("messageId");
        String fileName = event.getPayloadString("fileName");
        Long fileSize = event.getPayloadLong("fileSize");
        logger.info("Room {} sent file: {}", event.getRoomId(), fileName);
        messageProcessorService.processFileMessage(event.getRoomId(), messageId, fileName, fileSize != null ? fileSize : 0L, event.getReplyToken());
        return null;
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
