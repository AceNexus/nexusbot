package com.acenexus.tata.nexusbot.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 測試時區驗證與轉換工具
 */
class TimezoneValidatorTest {

    @Test
    void shouldValidateCorrectTimezoneIds() {
        // Given & When & Then
        assertThat(TimezoneValidator.isValidTimezone("Asia/Taipei")).isTrue();
        assertThat(TimezoneValidator.isValidTimezone("America/New_York")).isTrue();
        assertThat(TimezoneValidator.isValidTimezone("Europe/London")).isTrue();
        assertThat(TimezoneValidator.isValidTimezone("UTC")).isTrue();
    }

    @Test
    void shouldRejectInvalidTimezoneIds() {
        // Given & When & Then
        assertThat(TimezoneValidator.isValidTimezone("Invalid/Timezone")).isFalse();
        assertThat(TimezoneValidator.isValidTimezone("")).isFalse();
        assertThat(TimezoneValidator.isValidTimezone(null)).isFalse();
        assertThat(TimezoneValidator.isValidTimezone("   ")).isFalse();
    }

    @Test
    void shouldResolveChineseAliases() {
        // Given & When & Then
        assertThat(TimezoneValidator.resolveTimezone("台北")).isEqualTo("Asia/Taipei");
        assertThat(TimezoneValidator.resolveTimezone("台灣")).isEqualTo("Asia/Taipei");
        assertThat(TimezoneValidator.resolveTimezone("東京")).isEqualTo("Asia/Tokyo");
        assertThat(TimezoneValidator.resolveTimezone("日本")).isEqualTo("Asia/Tokyo");
        assertThat(TimezoneValidator.resolveTimezone("紐約")).isEqualTo("America/New_York");
        assertThat(TimezoneValidator.resolveTimezone("美東")).isEqualTo("America/New_York");
        assertThat(TimezoneValidator.resolveTimezone("洛杉磯")).isEqualTo("America/Los_Angeles");
        assertThat(TimezoneValidator.resolveTimezone("美西")).isEqualTo("America/Los_Angeles");
    }

    @Test
    void shouldResolveEnglishAliases() {
        // Given & When & Then
        assertThat(TimezoneValidator.resolveTimezone("taipei")).isEqualTo("Asia/Taipei");
        assertThat(TimezoneValidator.resolveTimezone("tokyo")).isEqualTo("Asia/Tokyo");
        assertThat(TimezoneValidator.resolveTimezone("new york")).isEqualTo("America/New_York");
        assertThat(TimezoneValidator.resolveTimezone("ny")).isEqualTo("America/New_York");
        assertThat(TimezoneValidator.resolveTimezone("la")).isEqualTo("America/Los_Angeles");
    }

    @Test
    void shouldResolveStandardIanaIds() {
        // Given & When & Then: 標準 IANA ID 應直接返回
        assertThat(TimezoneValidator.resolveTimezone("Asia/Taipei")).isEqualTo("Asia/Taipei");
        assertThat(TimezoneValidator.resolveTimezone("America/New_York")).isEqualTo("America/New_York");
        assertThat(TimezoneValidator.resolveTimezone("Europe/London")).isEqualTo("Europe/London");
    }

    @Test
    void shouldReturnNullForUnknownInput() {
        // Given & When & Then
        assertThat(TimezoneValidator.resolveTimezone("Unknown City")).isNull();
        assertThat(TimezoneValidator.resolveTimezone("")).isNull();
        assertThat(TimezoneValidator.resolveTimezone(null)).isNull();
    }

    @Test
    void shouldGetDisplayNames() {
        // Given & When & Then
        assertThat(TimezoneValidator.getDisplayName("Asia/Taipei")).contains("台北");
        assertThat(TimezoneValidator.getDisplayName("Asia/Tokyo")).contains("東京");
        assertThat(TimezoneValidator.getDisplayName("America/New_York")).contains("紐約");
        assertThat(TimezoneValidator.getDisplayName("America/Los_Angeles")).contains("洛杉磯");
    }

    @Test
    void shouldReturnTimezoneIdForUnknownDisplayName() {
        // Given & When: 未知的時區應返回原始 ID
        String unknown = "Some/Unknown";

        // Then
        assertThat(TimezoneValidator.getDisplayName(unknown)).isEqualTo(unknown);
    }

    @Test
    void shouldReturnDefaultForNullDisplayName() {
        // Given & When & Then
        assertThat(TimezoneValidator.getDisplayName(null)).isEqualTo("未知時區");
    }
}
