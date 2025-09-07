package com.acenexus.tata.nexusbot.lock;

public interface DistributedLock {

    boolean tryLock(String lockKey);

    void releaseLock(String lockKey);

    void cleanExpiredLocks();
}