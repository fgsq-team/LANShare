package com.fgsqw.lanshare.utils;

/**
 * @Author: fgsq
 */

import java.util.HashMap;

/**
 * 去除map key大小写敏感
 * @param <V>
 */
public class CaseInsensitiveMap<V> extends HashMap<String, V> {

    @Override
    public V put(String key, V value) {
        return super.put(key.toLowerCase(), value);
    }

    @Override
    public V get(Object key) {
        if (!(key instanceof String))
            return null;
        return super.get(((String) key).toLowerCase());
    }

    @Override
    public boolean containsKey(Object key) {
        if (!(key instanceof String))
            return false;
        return super.containsKey(((String) key).toLowerCase());
    }
}
