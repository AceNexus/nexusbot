package com.acenexus.tata.nexusbot.service.message;

import com.acenexus.tata.nexusbot.service.IMessageService;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 媒體消息處理器（圖片、影片、音檔、檔案、貼圖）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MediaMessageProcessor {

    private final IMessageService messageService;
    private final MessageTemplateProvider messageTemplateProvider;

    /**
     * 處理圖片消息
     */
    public void processImage(String roomId, String messageId, String replyToken) {
        String response = messageTemplateProvider.imageResponse(messageId);
        log.info("Image message processed from room {}: messageId={}", roomId, messageId);
        messageService.sendReply(replyToken, response);
    }

    /**
     * 處理貼圖消息
     */
    public void processSticker(String roomId, String packageId, String stickerId, String replyToken) {
        String response = messageTemplateProvider.stickerResponse(packageId, stickerId);
        log.info("Sticker message processed from room {}: packageId={}, stickerId={}", roomId, packageId, stickerId);
        messageService.sendReply(replyToken, response);
    }

    /**
     * 處理影片消息
     */
    public void processVideo(String roomId, String messageId, String replyToken) {
        String response = messageTemplateProvider.videoResponse(messageId);
        log.info("Video message processed from room {}: messageId={}", roomId, messageId);
        messageService.sendReply(replyToken, response);
    }

    /**
     * 處理音檔消息
     */
    public void processAudio(String roomId, String messageId, String replyToken) {
        String response = messageTemplateProvider.audioResponse(messageId);
        log.info("Audio message processed from room {}: messageId={}", roomId, messageId);
        messageService.sendReply(replyToken, response);
    }

    /**
     * 處理檔案消息
     */
    public void processFile(String roomId, String messageId, String fileName, long fileSize, String replyToken) {
        String response = messageTemplateProvider.fileResponse(fileName, fileSize);
        log.info("File message processed from room {}: fileName={}, size={}, messageId={}", roomId, fileName, fileSize, messageId);
        messageService.sendReply(replyToken, response);
    }
}
