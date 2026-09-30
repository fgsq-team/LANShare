package com.fgsqw.lanshare.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class StringLockManager {

    private static final Map<String, Lock> stringLocks = new HashMap<>();

    public static Lock getStringLock(String key) {
        synchronized (stringLocks) {
            // 使用 ArrayMap 存储每个字符串对应的锁
            if (!stringLocks.containsKey(key)) {
                stringLocks.put(key, new ReentrantLock());
            }
            return stringLocks.get(key);
        }
    }
}
