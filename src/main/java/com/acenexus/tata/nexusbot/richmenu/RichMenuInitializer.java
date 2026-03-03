package com.acenexus.tata.nexusbot.richmenu;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 應用啟動後自動執行 Rich Menu 初始化
 */
@Component
@RequiredArgsConstructor
public class RichMenuInitializer implements ApplicationRunner {

    private final RichMenuService richMenuService;

    @Override
    public void run(ApplicationArguments args) {
        richMenuService.setupRichMenu();
    }
}
