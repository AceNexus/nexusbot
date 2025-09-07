package com.acenexus.tata.nexusbot.lock;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReminderLockService implements DistributedLock {

    private static final Logger logger = LoggerFactory.getLogger(ReminderLockService.class);
    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean tryLock(String lockKey) {
        try {
            String sql = "INSERT INTO reminder_locks (lock_key) VALUES (?)";
            jdbcTemplate.update(sql, lockKey);
            return true;

        } catch (DuplicateKeyException e) {
            return false;
        } catch (Exception e) {
            logger.error("Failed to acquire lock {}: {}", lockKey, e.getMessage());
            return false;
        }
    }

    @Override
    public void releaseLock(String lockKey) {
        try {
            String sql = "DELETE FROM reminder_locks WHERE lock_key = ?";
            int result = jdbcTemplate.update(sql, lockKey);

            if (result > 0) {
                logger.debug("Successfully released lock: {}", lockKey);
            }
        } catch (Exception e) {
            logger.error("Failed to release lock {}: {}", lockKey, e.getMessage());
        }
    }

    @Override
    public void cleanExpiredLocks() {
        try {
            LocalDateTime expiredBefore = LocalDateTime.now().minusMinutes(5);
            String sql = "DELETE FROM reminder_locks WHERE locked_at < ?";
            int result = jdbcTemplate.update(sql, expiredBefore);

            if (result > 0) {
                logger.info("Cleaned {} expired locks", result);
            }
        } catch (Exception e) {
            logger.error("Failed to clean expired locks: {}", e.getMessage());
        }
    }
}