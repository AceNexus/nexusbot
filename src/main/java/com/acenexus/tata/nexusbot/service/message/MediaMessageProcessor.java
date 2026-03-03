package com.acenexus.tata.nexusbot.service.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 媒體消息處理器（圖片、影片、音檔、檔案、貼圖）
 */
@Slf4j
@Component
public class MediaMessageProcessor {

    /**
     * 處理圖片消息
     */
    public void processImage(String roomId, String messageId, String replyToken) {
        log.info("Image message processed from room {}: messageId={}", roomId, messageId);
    }

    /**
     * 處理貼圖消息
     */
    public void processSticker(String roomId, String packageId, String stickerId, String replyToken) {
        log.info("Sticker message processed from room {}: packageId={}, stickerId={}", roomId, packageId, stickerId);
    }

    /**
     * 處理影片消息
     */
    public void processVideo(String roomId, String messageId, String replyToken) {
        log.info("Video message processed from room {}: messageId={}", roomId, messageId);
    }

    /**
     * 處理音檔消息
     */
    public void processAudio(String roomId, String messageId, String replyToken) {
        log.info("Audio message processed from room {}: messageId={}", roomId, messageId);
    }

    /**
     * 處理檔案消息
     */
    public void processFile(String roomId, String messageId, String fileName, long fileSize, String replyToken) {
        log.info("File message processed from room {}: fileName={}, size={}, messageId={}", roomId, fileName, fileSize, messageId);
    }
}
