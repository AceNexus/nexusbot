package com.acenexus.tata.nexusbot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 根路徑控制器
 * 將根路徑重定向至專案 GitHub 首頁
 */
@Tag(name = "系統路徑", description = "處理系統層級的請求")
@Controller
public class RootController {

    @Operation(summary = "重定向至專案首頁", description = "將訪問根目錄的請求重定向至 GitHub 專案庫")
    @GetMapping("/")
    public String index() {
        return "redirect:https://github.com/orgs/AceNexus/repositories";
    }
}
