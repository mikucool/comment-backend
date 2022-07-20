package com.hzz.commentbackend.utils;

public interface ILock {

    /**
     * 尝试获取锁
     * @param timeoutSec 超时时间
     * @return 是否获取锁成功
     */
    boolean tryLock(long timeoutSec);

    void unlock();
}
