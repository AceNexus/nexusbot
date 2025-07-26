package com.acenexus.tata.nexusbot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class SignatureValidator {
    private static final Logger logger = LoggerFactory.getLogger(SignatureValidator.class);

    @Value("${line.bot.channel-secret}")
    private String channelSecret;

    public boolean validate(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    channelSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = Base64.getEncoder().encodeToString(digest);

            return signature.equals(expectedSignature);
        } catch (Exception e) {
            logger.error("Error during signature validation: {}", e.getMessage(), e);
            return false;
        }
    }
}