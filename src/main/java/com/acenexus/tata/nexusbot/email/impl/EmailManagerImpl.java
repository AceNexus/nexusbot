package com.acenexus.tata.nexusbot.email.impl;

import com.acenexus.tata.nexusbot.email.EmailManager;
import com.acenexus.tata.nexusbot.entity.Email;
import com.acenexus.tata.nexusbot.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Email 管理服務實作
 */
@Service
@RequiredArgsConstructor
public class EmailManagerImpl implements EmailManager {

    private static final Logger logger = LoggerFactory.getLogger(EmailManagerImpl.class);

    private final EmailRepository emailRepository;

    @Override
    @Transactional
    public Email addEmail(String roomId, String emailAddress) {
        try {
            Email email = Email.builder()
                    .roomId(roomId)
                    .email(emailAddress)
                    .isEnabled(true)
                    .isActive(true)
                    .build();

            Email savedEmail = emailRepository.save(email);
            logger.info("Added email {} for room {}", emailAddress, roomId);
            return savedEmail;

        } catch (Exception e) {
            logger.error("Failed to add email {} for room {}: {}", emailAddress, roomId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Email> getActiveEmails(String roomId) {
        try {
            return emailRepository.findActiveEmailsByRoomId(roomId);
        } catch (Exception e) {
            logger.error("Failed to get active emails for room {}: {}", roomId, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public List<String> getEnabledEmailAddresses(String roomId) {
        try {
            List<Email> emails = emailRepository.findEnabledEmailsByRoomId(roomId);
            return emails.stream()
                    .map(Email::getEmail)
                    .toList();
        } catch (Exception e) {
            logger.error("Failed to get enabled email addresses for room {}: {}", roomId, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    @Transactional
    public boolean enableEmail(Long emailId, String roomId) {
        try {
            return emailRepository.findByIdAndRoomId(emailId, roomId)
                    .map(email -> {
                        email.setIsEnabled(true);
                        emailRepository.save(email);
                        logger.info("Enabled email {} for room {}", email.getEmail(), roomId);
                        return true;
                    })
                    .orElseGet(() -> {
                        logger.warn("Email {} not found for room {}", emailId, roomId);
                        return false;
                    });
        } catch (Exception e) {
            logger.error("Failed to enable email {} for room {}: {}", emailId, roomId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean disableEmail(Long emailId, String roomId) {
        try {
            return emailRepository.findByIdAndRoomId(emailId, roomId)
                    .map(email -> {
                        email.setIsEnabled(false);
                        emailRepository.save(email);
                        logger.info("Disabled email {} for room {}", email.getEmail(), roomId);
                        return true;
                    })
                    .orElseGet(() -> {
                        logger.warn("Email {} not found for room {}", emailId, roomId);
                        return false;
                    });
        } catch (Exception e) {
            logger.error("Failed to disable email {} for room {}: {}", emailId, roomId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean deleteEmail(Long emailId, String roomId) {
        try {
            return emailRepository.findByIdAndRoomId(emailId, roomId)
                    .map(email -> {
                        email.setIsActive(false);
                        emailRepository.save(email);
                        logger.info("Deleted email {} for room {}", email.getEmail(), roomId);
                        return true;
                    })
                    .orElseGet(() -> {
                        logger.warn("Email {} not found for room {}", emailId, roomId);
                        return false;
                    });
        } catch (Exception e) {
            logger.error("Failed to delete email {} for room {}: {}", emailId, roomId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean hasEnabledEmails(String roomId) {
        try {
            List<String> enabledEmails = getEnabledEmailAddresses(roomId);
            return !enabledEmails.isEmpty();
        } catch (Exception e) {
            logger.error("Failed to check enabled emails for room {}: {}", roomId, e.getMessage(), e);
            return false;
        }
    }
}
