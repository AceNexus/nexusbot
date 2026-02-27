package com.acenexus.tata.nexusbot.util;

import com.acenexus.tata.nexusbot.ai.AIService;
import com.acenexus.tata.nexusbot.constants.AiModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 測試 AnalyzerUtil 的時區解析功能
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.profiles.active=test"
})
class AnalyzerUtilTimezoneTest {

    @MockitoBean
    private AIService aiService;

    @Autowired
    private AnalyzerUtil analyzerUtil;

    @Test
    void shouldParseTimeWithoutTimezone() {
        // Given: AI 返回沒有時區的結果
        String aiResponse = """
                {"datetime": "2025-12-11 15:00", "timezone": null}
                """;
        when(aiService.chatWithContext(eq("system"), anyString(), eq(AiModel.LLAMA_3_1_8B.id)))
                .thenReturn(new AIService.ChatResponse(aiResponse, AiModel.LLAMA_3_1_8B.id, 50, 100L, true));

        // When
        ParsedTimeResult result = analyzerUtil.parseTimeWithTimezone("明天下午3點", "Asia/Taipei");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDateTime()).isNotNull();
        assertThat(result.getDateTime().getHour()).isEqualTo(15);
        assertThat(result.getDateTime().getMinute()).isEqualTo(0);
        assertThat(result.hasTimezone()).isFalse();
        assertThat(result.getTimezone()).isNull();
    }

    @Test
    void shouldParseTimeWithExplicitTimezone() {
        // Given: AI 返回包含時區的結果
        String aiResponse = """
                {"datetime": "2025-12-11 15:00", "timezone": "America/New_York"}
                """;
        when(aiService.chatWithContext(eq("system"), anyString(), eq(AiModel.LLAMA_3_1_8B.id)))
                .thenReturn(new AIService.ChatResponse(aiResponse, AiModel.LLAMA_3_1_8B.id, 50, 100L, true));

        // When
        ParsedTimeResult result = analyzerUtil.parseTimeWithTimezone("明天紐約時間下午3點", "Asia/Taipei");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDateTime()).isNotNull();
        assertThat(result.getDateTime().getHour()).isEqualTo(15);
        assertThat(result.hasTimezone()).isTrue();
        assertThat(result.getTimezone()).isEqualTo("America/New_York");
    }

    @Test
    void shouldParseTokyoTimezone() {
        // Given: AI 返回東京時區
        String aiResponse = """
                {"datetime": "2025-12-11 09:00", "timezone": "Asia/Tokyo"}
                """;
        when(aiService.chatWithContext(eq("system"), anyString(), eq(AiModel.LLAMA_3_1_8B.id)))
                .thenReturn(new AIService.ChatResponse(aiResponse, AiModel.LLAMA_3_1_8B.id, 50, 100L, true));

        // When
        ParsedTimeResult result = analyzerUtil.parseTimeWithTimezone("東京時間早上9點", "Asia/Taipei");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasTimezone()).isTrue();
        assertThat(result.getTimezone()).isEqualTo("Asia/Tokyo");
        assertThat(result.getDateTime().getHour()).isEqualTo(9);
    }

    @Test
    void shouldHandleMarkdownCodeBlocks() {
        // Given: AI 返回包含 markdown 代碼塊的結果
        String aiResponse = """
                ```json
                {"datetime": "2025-12-11 15:00", "timezone": "Asia/Tokyo"}
                ```
                """;
        when(aiService.chatWithContext(eq("system"), anyString(), eq(AiModel.LLAMA_3_1_8B.id)))
                .thenReturn(new AIService.ChatResponse(aiResponse, AiModel.LLAMA_3_1_8B.id, 50, 100L, true));

        // When
        ParsedTimeResult result = analyzerUtil.parseTimeWithTimezone("東京時間下午3點", "Asia/Taipei");

        // Then: 應正確解析（移除 markdown 標記）
        assertThat(result).isNotNull();
        assertThat(result.hasTimezone()).isTrue();
        assertThat(result.getTimezone()).isEqualTo("Asia/Tokyo");
    }

    @Test
    void shouldValidateAndResolveTimezone() {
        // Given: AI 返回中文時區名稱
        String aiResponse = """
                {"datetime": "2025-12-11 15:00", "timezone": "台北"}
                """;
        when(aiService.chatWithContext(eq("system"), anyString(), eq(AiModel.LLAMA_3_1_8B.id)))
                .thenReturn(new AIService.ChatResponse(aiResponse, AiModel.LLAMA_3_1_8B.id, 50, 100L, true));

        // When
        ParsedTimeResult result = analyzerUtil.parseTimeWithTimezone("台北時間下午3點", "Asia/Taipei");

        // Then: 應自動解析為標準 IANA ID
        assertThat(result).isNotNull();
        assertThat(result.hasTimezone()).isTrue();
        assertThat(result.getTimezone()).isEqualTo("Asia/Taipei");
    }

    @Test
    void shouldIgnoreInvalidTimezone() {
        // Given: AI 返回無效的時區
        String aiResponse = """
                {"datetime": "2025-12-11 15:00", "timezone": "Invalid/Timezone"}
                """;
        when(aiService.chatWithContext(eq("system"), anyString(), eq(AiModel.LLAMA_3_1_8B.id)))
                .thenReturn(new AIService.ChatResponse(aiResponse, AiModel.LLAMA_3_1_8B.id, 50, 100L, true));

        // When
        ParsedTimeResult result = analyzerUtil.parseTimeWithTimezone("明天下午3點", "Asia/Taipei");

        // Then: 應忽略無效時區，時區欄位為 null
        assertThat(result).isNotNull();
        assertThat(result.hasTimezone()).isFalse();
        assertThat(result.getTimezone()).isNull();
    }

    @Test
    void shouldReturnNullForEmptyInput() {
        // When
        ParsedTimeResult result = analyzerUtil.parseTimeWithTimezone("", "Asia/Taipei");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullForNullInput() {
        // When
        ParsedTimeResult result = analyzerUtil.parseTimeWithTimezone(null, "Asia/Taipei");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenAIFails() {
        // Given: AI 服務失敗
        when(aiService.chatWithContext(eq("system"), anyString(), eq(AiModel.LLAMA_3_1_8B.id)))
                .thenReturn(new AIService.ChatResponse(null, AiModel.LLAMA_3_1_8B.id, 0, 0L, false));

        // When
        ParsedTimeResult result = analyzerUtil.parseTimeWithTimezone("明天下午3點", "Asia/Taipei");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullForInvalidJsonResponse() {
        // Given: AI 返回無效的 JSON
        when(aiService.chatWithContext(eq("system"), anyString(), eq(AiModel.LLAMA_3_1_8B.id)))
                .thenReturn(new AIService.ChatResponse("This is not JSON", AiModel.LLAMA_3_1_8B.id, 50, 100L, true));

        // When
        ParsedTimeResult result = analyzerUtil.parseTimeWithTimezone("明天下午3點", "Asia/Taipei");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldStripSecondsAndNanos() {
        // Given: AI 返回正常結果
        String aiResponse = """
                {"datetime": "2025-12-11 15:30", "timezone": null}
                """;
        when(aiService.chatWithContext(eq("system"), anyString(), eq(AiModel.LLAMA_3_1_8B.id)))
                .thenReturn(new AIService.ChatResponse(aiResponse, AiModel.LLAMA_3_1_8B.id, 50, 100L, true));

        // When
        ParsedTimeResult result = analyzerUtil.parseTimeWithTimezone("明天下午3點半", "Asia/Taipei");

        // Then: 秒和納秒應為 0
        assertThat(result).isNotNull();
        assertThat(result.getDateTime().getSecond()).isEqualTo(0);
        assertThat(result.getDateTime().getNano()).isEqualTo(0);
    }
}
