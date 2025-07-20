package com.acenexus.tata.nexusbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class NexusBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusBotApplication.class, args);
    }

}
