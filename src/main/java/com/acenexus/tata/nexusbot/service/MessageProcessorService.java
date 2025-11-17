package com.acenexus.tata.nexusbot.service;

import com.acenexus.tata.nexusbot.facade.LocationFacade;
import com.acenexus.tata.nexusbot.service.message.MediaMessageProcessor;
import com.acenexus.tata.nexusbot.service.message.TextMessageProcessor;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 消息處理協調服務
 * 職責：將消息分發給對應的處理器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessorService {

    private final TextMessageProcessor textMessageProcessor;
    private final MediaMessageProcessor mediaMessageProcessor;
    private final LocationFacade locationFacade;
    private final MessageService messageService;
    private final MessageTemplateProvider messageTemplateProvider;

    public void processTextMessage(String roomId, String sourceType, String userId, String messageText, String replyToken) {
        textMessageProcessor.process(roomId, sourceType, userId, messageText, replyToken);
    }

    public void processImageMessage(String roomId, String messageId, String replyToken) {
        mediaMessageProcessor.processImage(roomId, messageId, replyToken);
    }

    public void processStickerMessage(String roomId, String packageId, String stickerId, String replyToken) {
        mediaMessageProcessor.processSticker(roomId, packageId, stickerId, replyToken);
    }

    public void processVideoMessage(String roomId, String messageId, String replyToken) {
        mediaMessageProcessor.processVideo(roomId, messageId, replyToken);
    }

    public void processAudioMessage(String roomId, String messageId, String replyToken) {
        mediaMessageProcessor.processAudio(roomId, messageId, replyToken);
    }

    public void processFileMessage(String roomId, String messageId, String fileName,
                                   long fileSize, String replyToken) {
        mediaMessageProcessor.processFile(roomId, messageId, fileName, fileSize, replyToken);
    }

    public void processLocationMessage(String roomId, String title, String address, double latitude, double longitude, String replyToken) {
        Message response = locationFacade.handleLocationMessage(roomId, title, address, latitude, longitude, replyToken);
        if (response != null) {
            messageService.sendMessage(replyToken, response);
        }
    }

    public void processDefaultMessage(String roomId, String replyToken) {
        String response = messageTemplateProvider.unknownMessage();
        log.warn("Default message handler used for room {}", roomId);
        messageService.sendReply(replyToken, response);
    }
}
