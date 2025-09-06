package com.acenexus.tata.nexusbot.util;

import com.acenexus.tata.nexusbot.ai.AIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

@Component
public class AnalyzerUtil {

    private static final Logger logger = LoggerFactory.getLogger(AnalyzerUtil.class);
    private static final Pattern TIME_FORMAT = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static AIService aiService;

    @Autowired
    public void setAiService(AIService aiService) {
        AnalyzerUtil.aiService = aiService;
    }

    /**
     * 使用 AI 語意分析解析使用者輸入的時間
     */
    public static LocalDateTime parseTime(String input) {
        if (input == null || input.isBlank()) return null;

        try {
            String prompt = buildPrompt(input.trim());
            AIService.ChatResponse response = aiService.chatWithContext("system", prompt, "llama-3.1-8b-instant");

            if (response.success() && response.content() != null) {
                String result = response.content().trim();
                logger.debug("AI input: '{}', output: '{}'", input, result);

                if (result.isEmpty() || result.equals("\"\"")) return null;
                if (TIME_FORMAT.matcher(result).matches()) return LocalDateTime.parse(result, FORMATTER);

                logger.warn("AI returned invalid format: {}", result);
            } else {
                logger.error("AI service failed for input: '{}'", input);
            }
        } catch (Exception e) {
            logger.error("Error parsing time: {}", e.getMessage());
        }
        return null;
    }

    private static String buildPrompt(String userInput) {
        String now = LocalDateTime.now().format(FORMATTER);
        return String.format("""
                系統角色：你是「時間解析助手」，你的任務是將使用者的自然語言時間表達解析為標準日期時間格式 YYYY-MM-DD HH:MM（24小時制，Asia/Taipei 時區）。
                            
                當前時間：%s
                            
                規則：
                1. 僅輸出一行文字，格式必須完全符合 `YYYY-MM-DD HH:MM`。
                2. 若只含日期，補上時間：
                   - 早上 -> 09:00
                   - 中午 -> 12:00
                   - 下午 -> 15:00
                   - 傍晚 -> 18:00
                   - 晚上 -> 18:00（例如「晚上六點」解析為 18:00）
                   - 半夜/凌晨 -> 02:00
                   - 無暗示時間，預設 09:00
                3. 若只含時間或相對詞（今天、明天、後天、下週二、下一個工作日…），以當前時間為基準解析為完整日期時間。
                4. 若包含具體時間（如 6 點、18:30），對照上面時段規則解析。
                5. 支援模糊時間表達，如「下午三點半」、「凌晨一點」。
                6. 若輸入帶時區，請轉換為 Asia/Taipei 時區。
                7. 如果無法解析或矛盾，請輸出空字串 ""。
                8. 月份/日期/小時/分鐘皆應補零，例如 09、06。
                            
                範例：
                - "明天下午三點" -> "2025-09-07 15:00"
                - "下週二上午十點" -> "2025-09-16 10:00"
                - "三天後早上八點半" -> "2025-09-09 08:30"
                - "今天晚上六點" -> "2025-09-06 18:00"
                - "不確定" -> ""
                            
                使用者輸入：%s
                """, now, userInput);
    }
}
