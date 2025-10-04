package com.acenexus.tata.nexusbot.service;

import com.acenexus.tata.nexusbot.ai.AIService;
import com.acenexus.tata.nexusbot.chatroom.ChatRoomManager;
import com.acenexus.tata.nexusbot.entity.ChatMessage;
import com.acenexus.tata.nexusbot.entity.ChatRoom;
import com.acenexus.tata.nexusbot.entity.ReminderState;
import com.acenexus.tata.nexusbot.location.LocationService;
import com.acenexus.tata.nexusbot.reminder.ReminderService;
import com.acenexus.tata.nexusbot.reminder.ReminderStateManager;
import com.acenexus.tata.nexusbot.repository.ChatMessageRepository;
import com.acenexus.tata.nexusbot.template.MessageTemplateProvider;
import com.acenexus.tata.nexusbot.util.AnalyzerUtil;
import com.linecorp.bot.model.message.Message;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

import static com.acenexus.tata.nexusbot.constants.TimeFormatters.STANDARD_TIME;

@Service
@RequiredArgsConstructor
public class MessageProcessorService {
    private static final Logger logger = LoggerFactory.getLogger(MessageProcessorService.class);
    private static final DateTimeFormatter TIME_FORMATTER = STANDARD_TIME;
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private final MessageService messageService;
    private final AIService aiService;
    private final MessageTemplateProvider messageTemplateProvider;
    private final ChatRoomManager chatRoomManager;
    private final ChatMessageRepository chatMessageRepository;
    private final AdminService adminService;
    private final ReminderService reminderService;
    private final ReminderStateManager reminderStateManager;
    private final LocationService locationService;
    private final com.acenexus.tata.nexusbot.handler.PostbackEventHandler postbackEventHandler;
    private final com.acenexus.tata.nexusbot.email.EmailManager emailManager;

    public void processTextMessage(String roomId, String sourceType, String userId, String messageText, String replyToken) {
        String normalizedText = messageText.toLowerCase().trim();
        ChatRoom.RoomType roomType = chatRoomManager.determineRoomType(sourceType);

        // 處理所有命令
        if (handleAllCommands(roomId, roomType, messageText, normalizedText, replyToken)) {
            return;
        }

        // 檢查 AI 是否啟用
        if (!chatRoomManager.isAiEnabled(roomId, roomType)) {
            logger.info("AI disabled for room: {} (type: {}), skipping AI processing", roomId, roomType);
            return;
        }

        // 儲存用戶對話
        ChatMessage userMessage = ChatMessage.createUserMessage(roomId, roomType, userId, messageText);
        chatMessageRepository.save(userMessage);

        // 非同步處理 AI 對話
        handleAIMessage(roomId, roomType, messageText, replyToken);
    }

    /**
     * 處理所有命令類型
     */
    private boolean handleAllCommands(String roomId, ChatRoom.RoomType roomType, String messageText, String normalizedText, String replyToken) {
        // 1. 管理員認證命令
        String authResponse = adminService.processAuthCommand(roomId, roomType, messageText);
        if (authResponse != null) {
            messageService.sendReply(replyToken, authResponse);
            return true;
        }

        // 2. 管理員指令
        Message adminMessage = adminService.processAdminCommand(roomId, roomType, messageText);
        if (adminMessage != null) {
            messageService.sendMessage(replyToken, adminMessage);
            return true;
        }

        // 3. 提醒互動流程處理
        if (handleReminderInteraction(roomId, messageText, replyToken)) {
            return true;
        }

        // 4. Email 輸入處理
        if (handleEmailInput(roomId, roomType, messageText, replyToken)) {
            return true;
        }

        // 5. 預定義指令
        try {
            return switch (normalizedText) {
                case "menu", "選單" -> {
                    messageService.sendMessage(replyToken, messageTemplateProvider.mainMenu());
                    yield true;
                }
                default -> false;
            };
        } catch (Exception e) {
            logger.error("Error processing command for room {}: {}", roomId, e.getMessage());
            return false; // 讓它繼續走 AI 處理
        }
    }

    private void handleAIMessage(String roomId, ChatRoom.RoomType roomType, String messageText, String replyToken) {
        CompletableFuture.runAsync(() -> {
            try {
                // 獲取聊天室指定的AI模型
                String selectedModel = chatRoomManager.getAiModel(roomId, roomType);
                AIService.ChatResponse chatResponse = aiService.chatWithContext(roomId, messageText, selectedModel);
                String finalResponse = (chatResponse.success() && chatResponse.content() != null && !chatResponse.content().trim().isEmpty()) ? chatResponse.content() : messageTemplateProvider.defaultTextResponse(messageText);

                messageService.sendReply(replyToken, finalResponse);

                // 儲存 AI 對話（現在有真實的 tokens 和處理時間）
                ChatMessage aiMessage = ChatMessage.createAIMessage(roomId, roomType, finalResponse, chatResponse.model(), chatResponse.tokensUsed(), chatResponse.processingTime().intValue());
                chatMessageRepository.save(aiMessage);

                logger.info("AI response sent to room {}, tokens: {}, time: {}ms", roomId, chatResponse.tokensUsed(), chatResponse.processingTime());
            } catch (Exception e) {
                logger.error("AI processing error for room {}: {}", roomId, e.getMessage());
                String fallbackResponse = messageTemplateProvider.defaultTextResponse(messageText);
                messageService.sendReply(replyToken, fallbackResponse);

                // 也儲存錯誤回應
                ChatMessage aiMessage = ChatMessage.createAIMessage(roomId, roomType, fallbackResponse, "fallback", 0, 0);
                chatMessageRepository.save(aiMessage);
            }
        });
    }

    public void processImageMessage(String roomId, String messageId, String replyToken) {
        String response = messageTemplateProvider.imageResponse(messageId);
        logger.info("Image message processed from room {}: messageId={}", roomId, messageId);
        messageService.sendReply(replyToken, response);
    }

    public void processStickerMessage(String roomId, String packageId, String stickerId, String replyToken) {
        String response = messageTemplateProvider.stickerResponse(packageId, stickerId);
        logger.info("Sticker message processed from room {}: packageId={}, stickerId={}", roomId, packageId, stickerId);
        messageService.sendReply(replyToken, response);
    }

    public void processVideoMessage(String roomId, String messageId, String replyToken) {
        String response = messageTemplateProvider.videoResponse(messageId);
        logger.info("Video message processed from room {}: messageId={}", roomId, messageId);
        messageService.sendReply(replyToken, response);
    }

    public void processAudioMessage(String roomId, String messageId, String replyToken) {
        String response = messageTemplateProvider.audioResponse(messageId);
        logger.info("Audio message processed from room {}: messageId={}", roomId, messageId);
        messageService.sendReply(replyToken, response);
    }

    public void processFileMessage(String roomId, String messageId, String fileName, long fileSize, String replyToken) {
        String response = messageTemplateProvider.fileResponse(fileName, fileSize);
        logger.info("File message processed from room {}: fileName={}, size={}, messageId={}", roomId, fileName, fileSize, messageId);
        messageService.sendReply(replyToken, response);
    }

    public void processLocationMessage(String roomId, String title, String address, double latitude, double longitude, String replyToken) {
        logger.info("Location message processed from room {}: title={}, address={}, lat={}, lon={}", roomId, title, address, latitude, longitude);

        // 檢查是否正在等待位置以搜尋廁所
        boolean isWaitingForToiletSearch = chatRoomManager.isWaitingForToiletSearch(roomId);

        if (isWaitingForToiletSearch) {
            // 清除廁所搜尋等待狀態（記錄肯定存在，因為上面返回了 true）
            chatRoomManager.updateWaitingForToiletSearch(roomId, false);
            logger.info("Processing toilet search for room {} with location: lat={}, lon={}", roomId, latitude, longitude);

            CompletableFuture.runAsync(() -> {
                try {
                    locationService.findNearbyToilets(latitude, longitude, 1000)
                            .thenAccept(toilets -> {
                                Message response = messageTemplateProvider.nearbyToiletsResponse(toilets, latitude, longitude);
                                messageService.sendMessage(replyToken, response);
                            })
                            .exceptionally(throwable -> {
                                logger.error("Error finding nearby toilets", throwable);
                                String fallbackResponse = messageTemplateProvider.locationResponse(title, address, latitude, longitude);
                                messageService.sendReply(replyToken, fallbackResponse);
                                return null;
                            });
                } catch (Exception e) {
                    logger.error("Error processing location for toilet search", e);
                    String fallbackResponse = messageTemplateProvider.locationResponse(title, address, latitude, longitude);
                    messageService.sendReply(replyToken, fallbackResponse);
                }
            });
        } else {
            // 一般位置訊息處理，僅回覆位置資訊
            String response = messageTemplateProvider.locationResponse(title, address, latitude, longitude);
            messageService.sendReply(replyToken, response);
            logger.info("General location message processed for room {}", roomId);
        }
    }

    public void processDefaultMessage(String roomId, String replyToken) {
        String response = messageTemplateProvider.unknownMessage();
        logger.warn("Default message handler used for room {}", roomId);
        messageService.sendReply(replyToken, response);
    }

    private boolean handleReminderInteraction(String roomId, String messageText, String replyToken) {
        ReminderState.Step currentStep = reminderStateManager.getCurrentStep(roomId);
        if (currentStep == null) {
            return false; // 用戶不在提醒流程中
        }

        try {
            switch (currentStep) {
                case WAITING_FOR_TIME -> {
                    String input = messageText.trim();
                    LocalDateTime reminderTime;

                    // 先嘗試標準格式，失敗則使用 AI 解析
                    if (input.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")) {
                        try {
                            reminderTime = LocalDateTime.parse(input, TIME_FORMATTER)
                                    .withSecond(0).withNano(0); // 確保為整分
                        } catch (Exception e) {
                            reminderTime = AnalyzerUtil.parseTime(input);
                        }
                    } else {
                        reminderTime = AnalyzerUtil.parseTime(input);
                    }

                    if (reminderTime == null) {
                        messageService.sendMessage(replyToken, messageTemplateProvider.reminderInputError(input, "無法解析時間格式"));
                        return true;
                    }

                    if (reminderTime.isBefore(LocalDateTime.now())) {
                        messageService.sendMessage(replyToken, messageTemplateProvider.reminderInputError(input, "時間必須是未來\n" + reminderTime.format(TIME_FORMATTER)));
                        return true;
                    }

                    // 儲存時間並進入下一步
                    reminderStateManager.setTime(roomId, reminderTime);
                    Message nextStepMessage = messageTemplateProvider.reminderInputMenu("content", reminderTime.format(TIME_FORMATTER));
                    messageService.sendMessage(replyToken, nextStepMessage);
                    return true;
                }
                case WAITING_FOR_CONTENT -> {
                    // 獲取之前輸入的時間和重複類型
                    LocalDateTime reminderTime = reminderStateManager.getTime(roomId);
                    String repeatType = reminderStateManager.getRepeatType(roomId);
                    String content = messageText.trim();

                    // 創建提醒 (以聊天室為主，createdBy 使用 roomId)
                    reminderService.createReminder(roomId, content, reminderTime, repeatType, roomId);

                    // 清除狀態
                    reminderStateManager.clearState(roomId);

                    String repeatTypeText = switch (repeatType) {
                        case "DAILY" -> "每日重複";
                        case "WEEKLY" -> "每週重複";
                        default -> "僅一次";
                    };

                    Message successMessage = messageTemplateProvider.reminderCreatedSuccess(reminderTime.format(TIME_FORMATTER), repeatTypeText, content);

                    messageService.sendMessage(replyToken, successMessage);

                    logger.info("Reminder created for room {}: {} at {}", roomId, content, reminderTime);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.error("Error processing reminder interaction: {}", e.getMessage());
            reminderStateManager.clearState(roomId);
            messageService.sendMessage(replyToken, messageTemplateProvider.reminderInputError("系統錯誤", "處理提醒時發生錯誤"));
            return true;
        }

        return false;
    }

    /**
     * 處理 Email 輸入
     */
    private boolean handleEmailInput(String roomId, ChatRoom.RoomType roomType, String messageText, String replyToken) {
        // 檢查是否正在等待 Email 輸入
        if (!postbackEventHandler.isWaitingForEmailInput(roomId)) {
            return false;
        }

        try {
            String email = messageText.trim();

            // 驗證 Email 格式
            if (!email.matches(EMAIL_REGEX)) {
                logger.warn("Invalid email format from room {}: {}", roomId, email);
                messageService.sendMessage(replyToken, messageTemplateProvider.emailInvalidFormat());
                return true;
            }

            // 新增 Email
            com.acenexus.tata.nexusbot.entity.Email addedEmail = emailManager.addEmail(roomId, email);

            if (addedEmail != null) {
                // 清除等待狀態
                postbackEventHandler.clearEmailInputState(roomId);

                // 發送成功訊息
                messageService.sendMessage(replyToken, messageTemplateProvider.emailAddSuccess(email));
                logger.info("Email added successfully for room {}: {}", roomId, email);
            } else {
                messageService.sendMessage(replyToken, messageTemplateProvider.error("新增 Email 時發生錯誤，請稍後再試。"));
                logger.error("Failed to add email for room {}", roomId);
            }

            return true;

        } catch (Exception e) {
            logger.error("Error processing email input for room {}: {}", roomId, e.getMessage());
            postbackEventHandler.clearEmailInputState(roomId);
            messageService.sendMessage(replyToken, messageTemplateProvider.error("處理 Email 輸入時發生錯誤。"));
            return true;
        }
    }
}
