package com.acenexus.tata.nexusbot.controller;

import com.linecorp.bot.spring.boot.annotation.LineMessageHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LineBotController {
    @GetMapping("test")
    public String test() {
        return "Test";
    }
}