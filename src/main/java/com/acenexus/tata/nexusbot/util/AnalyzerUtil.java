package com.acenexus.tata.nexusbot.util;

import com.acenexus.tata.nexusbot.ai.AIService;
import com.acenexus.tata.nexusbot.config.properties.GroqProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import static com.acenexus.tata.nexusbot.constants.TimeFormatters.STANDARD_TIME;

@Component
@RequiredArgsConstructor
public class AnalyzerUtil {

    private final Logger logger = LoggerFactory.getLogger(AnalyzerUtil.class);
    private final Pattern TIME_FORMAT = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$");
    private final DateTimeFormatter FORMATTER = STANDARD_TIME;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AIService aiService;
    private final GroqProperties groqProperties;

    /**
     * 使用 AI 解析時間，支援時區識別
     *
     * @param input           使用者輸入（可能包含時區資訊）
     * @param defaultTimezone 預設時區（當使用者未指定時區時使用）
     * @return ParsedTimeResult 包含解析後的時間與時區，失敗時返回 null
     */
    public ParsedTimeResult parseTimeWithTimezone(String input, String defaultTimezone) {
        if (input == null || input.isBlank()) return null;

        try {
            String prompt = buildPromptWithTimezone(input.trim(), defaultTimezone);
            AIService.ChatResponse response = aiService.chatWithContext("system", prompt, groqProperties.getDefaultModel());

            if (response.success() && response.content() != null) {
                String result = response.content().trim();
                logger.debug("AI timezone parsing - input: '{}', output: '{}'", input, result);

                // 移除可能的 markdown 代碼塊標記
                result = result.replace("```json", "").replace("```", "").trim();

                if (result.isEmpty() || result.equals("\"\"")) return null;

                // 解析 JSON 回應
                JsonNode jsonNode = objectMapper.readTree(result);
                String datetimeStr = jsonNode.get("datetime").asText();
                JsonNode timezoneNode = jsonNode.get("timezone");
                String timezone = (timezoneNode != null && !timezoneNode.isNull()) ? timezoneNode.asText() : null;

                // 驗證日期時間格式
                if (TIME_FORMAT.matcher(datetimeStr).matches()) {
                    LocalDateTime parsedTime = LocalDateTime.parse(datetimeStr, FORMATTER);
                    parsedTime = parsedTime.withSecond(0).withNano(0);

                    // 驗證時區（如果有的話）
                    if (timezone != null && !timezone.isEmpty()) {
                        String resolvedTimezone = TimezoneValidator.resolveTimezone(timezone);
                        if (resolvedTimezone == null) {
                            logger.warn("AI returned invalid timezone: {}, ignoring", timezone);
                            timezone = null;
                        } else {
                            timezone = resolvedTimezone;
                        }
                    }

                    return new ParsedTimeResult(parsedTime, timezone);
                }

                logger.warn("AI returned invalid datetime format: {}", datetimeStr);
            } else {
                logger.error("AI service failed for timezone parsing input: '{}'", input);
            }
        } catch (Exception e) {
            logger.error("Error parsing time with timezone: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 建立包含準確當前時間和預設時區的 Prompt 訊息。
     * * @param userInput 使用者輸入的文本。
     *
     * @param defaultTimezone IANA 時區 ID (例如 "Asia/Taipei")。
     * @return 供 AI 模型解析的完整 Prompt 文本。
     */
    private String buildPromptWithTimezone(String userInput, String defaultTimezone) {

        try {
            // 1. 取得目標時區 (ZoneId)
            ZoneId zoneId = ZoneId.of(defaultTimezone);

            // 2. 取得該時區下的當前時間 (ZonedDateTime)
            ZonedDateTime nowZoned = ZonedDateTime.now(zoneId);

            // 3. 格式化為 Prompt 所需的字串
            String now = nowZoned.format(FORMATTER);

            // 假設當前時間是 2025-12-10 16:31 (CST)
            // now = "2025-12-10 16:31"

            // --- Prompt 內容 ---
            return String.format("""
                    系統角色：你是「時間解析助手」，解析使用者的時間表達。

                    **注意：所有相對時間（如「明天」、「一小時後」、「一分鐘後」）的計算，請嚴格以「當前時間」和「預設時區」為準！**

                    當前時間：%s
                    預設時區：%s

                    任務：
                    1. 解析日期時間為 YYYY-MM-DD HH:MM 格式
                    2. 解析時區（如果使用者有指定）

                    時區對照表：
                    - 「紐約」、「美國東岸」、「美東」、「NY」→ America/New_York
                    - 「洛杉磯」、「美國西岸」、「美西」、「LA」→ America/Los_Angeles
                    - 「東京」、「日本」→ Asia/Tokyo
                    - 「台北」、「台灣」→ Asia/Taipei
                    - 「香港」→ Asia/Hong_Kong
                    - 「新加坡」→ Asia/Singapore
                    - 「倫敦」、「英國」→ Europe/London
                    - 「首爾」、「韓國」→ Asia/Seoul
                    - 「上海」、「中國」→ Asia/Shanghai

                    回應格式（純 JSON）：
                    {
                      "datetime": "YYYY-MM-DD HH:MM",
                      "timezone": "IANA時區ID或null"
                    }

                    規則：
                    1. 如果使用者明確指定時區，必須解析出 timezone
                    2. 如果沒有指定時區，timezone 設為 null
                    3. 只回傳 JSON，不要其他文字
                    4. 時間預設規則（若未明確指定）：
                       - 早上 -> 09:00
                       - 中午 -> 12:00
                       - 下午 -> 15:00
                       - 傍晚/晚上 -> 18:00
                       - 半夜/凌晨 -> 02:00
                    5. 月份/日期/小時/分鐘皆應補零，例如 09、06

                    範例：
                    輸入：「明天紐約時間下午 3 點」
                    輸出：{"datetime": "2025-12-11 15:00", "timezone": "America/New_York"}

                    輸入：「明天下午 3 點」
                    輸出：{"datetime": "2025-12-11 15:00", "timezone": null}

                    輸入：「東京時間早上 9 點」
                    輸出：{"datetime": "2025-12-11 09:00", "timezone": "Asia/Tokyo"}

                    使用者輸入：%s
                    """, now, defaultTimezone, userInput);

        } catch (Exception e) {
            logger.warn("時區 ID 無效: {}", defaultTimezone);
            String now = LocalDateTime.now().format(FORMATTER);
            return String.format("Error generating prompt: %s. Using local time: %s", e.getMessage(), now);
        }
    }
}
