package com.acenexus.tata.nexusbot.config;

import com.acenexus.tata.nexusbot.config.properties.EmailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * 電子郵件配置
 * 配置 JavaMailSender 用於發送郵件
 */
@Configuration
@RequiredArgsConstructor
public class EmailConfig {

    private final EmailProperties emailProperties;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // SMTP 伺服器設定
        mailSender.setHost(emailProperties.getHost());
        mailSender.setPort(emailProperties.getPort());

        // 認證資訊
        mailSender.setUsername(emailProperties.getUsername());
        mailSender.setPassword(emailProperties.getPassword());

        // 郵件屬性設定
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", emailProperties.getAuth().toString());
        props.put("mail.smtp.starttls.enable", emailProperties.getStarttls().toString());
        props.put("mail.debug", emailProperties.getDebug().toString());

        return mailSender;
    }
}
