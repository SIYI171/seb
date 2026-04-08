package com.seb.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RequestRateLimiter {

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public boolean allow(String key, int maxRequests, Duration window) {
        long now = System.currentTimeMillis();
        long windowMillis = window.toMillis();

        WindowCounter counter = counters.compute(key, (ignored, existing) -> {
            if (existing == null || now - existing.windowStart >= windowMillis) {
                return new WindowCounter(now, 1);
            }
            existing.count.incrementAndGet();
            return existing;
        });

        cleanup(now, windowMillis);
        return counter.count.get() <= maxRequests;
    }

    private void cleanup(long now, long windowMillis) {
        if (counters.size() < 1024) {
            return;
        }
        counters.entrySet().removeIf(entry -> now - entry.getValue().windowStart >= windowMillis * 2);
    }

    private static class WindowCounter {
        private final long windowStart;
        private final AtomicInteger count;

        private WindowCounter(long windowStart, int initialCount) {
            this.windowStart = windowStart;
            this.count = new AtomicInteger(initialCount);
        }
    }
}
