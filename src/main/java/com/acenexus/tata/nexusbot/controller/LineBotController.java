package com.acenexus.tata.nexusbot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LineBotController {
    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("test")
    public String test() {
        return "Test";
    }

    @PostMapping("webhook")
    public String receiveWebhook(@RequestBody String payload) throws Exception {
        Object json = objectMapper.readValue(payload, Object.class);
        String pretty = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        System.out.println("Webhook:\n" + pretty);
        return "OK";
    }

}