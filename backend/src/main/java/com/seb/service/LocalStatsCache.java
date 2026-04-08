package com.seb.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class LocalStatsCache {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T getOrCompute(String key, Duration ttl, Supplier<T> supplier) {
        long now = System.currentTimeMillis();
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.expiresAt > now) {
            return (T) cached.value;
        }

        T value = supplier.get();
        cache.put(key, new CacheEntry(value, now + ttl.toMillis()));
        cleanup(now);
        return value;
    }

    public void evictByPrefix(String prefix) {
        cache.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private void cleanup(long now) {
        if (cache.size() < 2048) {
            return;
        }
        cache.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
    }

    private record CacheEntry(Object value, long expiresAt) {
    }
}
