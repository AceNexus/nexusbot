package com.acenexus.tata.nexusbot.util;

import com.acenexus.tata.nexusbot.ai.AIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

@Component
public class AnalyzerUtil {
    private static final Logger logger = LoggerFactory.getLogger(AnalyzerUtil.class);
    private static final Pattern TIME_FORMAT_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static AIService aiService;

    @Autowired
    public void setAiService(AIService aiService) {
        AnalyzerUtil.aiService = aiService;
    }

    /**
     * 使用 AI 語意分析將用戶輸入解析為標準日期時間格式
     *
     * @param input 用戶輸入的時間表達式
     * @return LocalDateTime 物件，無法解析則返回 null
     */
    public static LocalDateTime parseTime(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        try {
            // 構建 AI 提示詞
            String aiPrompt = buildTimeParsePrompt(input.trim());

            // 調用 AI 服務進行語意分析
            AIService.ChatResponse response = aiService.chatWithContext("system", aiPrompt, "llama-3.1-8b-instant");

            if (response.success() && response.content() != null) {
                String aiResult = response.content().trim();
                logger.debug("AI time parsing - Input: '{}', AI Output: '{}'", input, aiResult);

                // 驗證 AI 返回的格式是否正確
                if (TIME_FORMAT_PATTERN.matcher(aiResult).matches()) {
                    return LocalDateTime.parse(aiResult, FORMATTER);
                } else if (aiResult.isEmpty() || aiResult.equals("\"\"")) {
                    logger.debug("AI could not parse time: '{}'", input);
                    return null;
                } else {
                    logger.warn("AI returned invalid time format: '{}' for input: '{}'", aiResult, input);
                    return null;
                }
            } else {
                logger.error("AI service failed for time parsing. Input: '{}'", input);
                return null;
            }

        } catch (DateTimeParseException e) {
            logger.error("Failed to parse AI time result for input '{}': {}", input, e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error during AI time parsing for input '{}': {}", input, e.getMessage());
            return null;
        }
    }

    /**
     * 構建 AI 時間解析提示詞
     */
    private static String buildTimeParsePrompt(String userInput) {
        return String.format("""
                系統角色：你是「時間解析助手」。
                            
                任務：把使用者給的一段自然語句解析為確切的日期時間，輸出精確的格式：YYYY-MM-DD HH:MM（24小時制）。
                            
                規則：
                1. 輸出**僅**一行，純文字，格式完全符合 `\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}`。不要額外文字或註解。
                2. 若輸入只含日期，請補上時間：若語句暗示時間段（如「下午」「早上」）則用該區段的代表時間（早上 -> 09:00；中午 -> 12:00；下午 -> 15:00；傍晚 -> 18:00；晚上 -> 20:00）；若完全無暗示，預設時間 09:00。
                3. 若輸入只含時間或相對詞（例如「明天中午」），請以**當前當地時間**解析為具體日期時間（系統請以 Asia/Taipei 時區為基準）。
                4. 支援常見相對詞/短語：今天、明天、後天、上週、下週/下個星期、下個月、下個禮拜一、下週二、下個月 5 日、下禮拜三下午、兩週後、三天後凌晨、下一個工作日等。
                5. 支援模糊時段：早上/上午 -> 09:00；中午 -> 12:00；下午 -> 15:00；傍晚 -> 18:00；晚上 -> 20:00；半夜/凌晨 -> 02:00。
                6. 若輸入包含時區，請轉換並輸出為 Asia/Taipei 時區時間（格式不帶時區標記）。
                7. 如果無法推斷準確日期（矛盾、模糊到無法解），請輸出空字串 ""。
                8. 請遵守輸出格式嚴格驗證：`YYYY-MM-DD HH:MM`，月份/日期/小時/分鐘皆應有兩位數。
                            
                範例：
                - "下週二上午十點開會" -> "2025-09-16 10:00"
                - "明天下午" -> "2025-09-07 15:00"
                - "2025/9/6 12:00" -> "2025-09-06 12:00"
                - "下個月5號" -> "2025-10-05 09:00"
                - "三天後早上八點半" -> "2025-09-09 08:30"
                - "不確定" -> ""
                            
                用戶輸入：%s
                """, userInput);
    }
}
