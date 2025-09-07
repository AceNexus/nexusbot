package com.acenexus.tata.nexusbot.scheduler;

import com.acenexus.tata.nexusbot.lock.DistributedLock;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LockCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(LockCleanupScheduler.class);
    private final DistributedLock distributedLock;

    @Scheduled(fixedRate = 600000) // 每10分鐘清理一次過期鎖
    public void cleanExpiredLocks() {
        try {
            distributedLock.cleanExpiredLocks();
        } catch (Exception e) {
            logger.error("Failed to clean expired locks: {}", e.getMessage(), e);
        }
    }
}