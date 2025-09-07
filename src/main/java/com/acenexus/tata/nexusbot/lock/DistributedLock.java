package com.acenexus.tata.nexusbot.lock;

public interface DistributedLock {

    boolean tryLock(String lockKey);

    void cleanExpiredLocks();
}