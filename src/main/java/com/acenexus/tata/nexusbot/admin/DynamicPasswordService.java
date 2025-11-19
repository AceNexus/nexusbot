package com.acenexus.tata.nexusbot.admin;

import com.acenexus.tata.nexusbot.config.properties.AdminProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicPasswordService {

    private final AdminProperties adminProperties;

    /**
     * 生成當前時間週期的動態密碼
     * 基於簡單規則：日期 + 種子密碼
     */
    public String getCurrentPassword() {
        LocalDateTime now = LocalDateTime.now();
        String dateStr = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return dateStr + adminProperties.getPasswordSeed();
    }

}