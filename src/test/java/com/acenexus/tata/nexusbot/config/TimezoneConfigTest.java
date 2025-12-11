package com.acenexus.tata.nexusbot.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.ZoneId;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 測試系統時區配置
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.profiles.active=test"
})
class TimezoneConfigTest {

    @Test
    void shouldSetSystemTimezoneToUTC() {
        // Given & When: TimezoneConfig 應該在應用啟動時自動執行
        TimeZone defaultTimezone = TimeZone.getDefault();

        // Then: 系統預設時區應為 UTC
        assertThat(defaultTimezone.getID()).isEqualTo("UTC");
        assertThat(defaultTimezone.getRawOffset()).isEqualTo(0);
    }

    @Test
    void shouldUseUTCForSystemOperations() {
        // Given: 系統時區已設為 UTC
        ZoneId systemZone = ZoneId.systemDefault();

        // Then: Java 8 Time API 也應使用 UTC
        assertThat(systemZone.getId()).isEqualTo("UTC");
    }
}
