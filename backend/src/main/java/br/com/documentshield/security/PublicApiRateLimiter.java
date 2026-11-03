package br.com.documentshield.security;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class PublicApiRateLimiter {
    private final Map<UUID, Window> windows = new ConcurrentHashMap<>();

    public boolean allow(UUID credentialId, int limit) {
        long minute = Instant.now().getEpochSecond() / 60;
        Window window = windows.compute(credentialId, (id, current) -> {
            if (current == null || current.minute != minute) {
                return new Window(minute, 1);
            }
            return new Window(minute, current.count + 1);
        });
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(e -> e.getValue().minute < minute - 1);
        }
        return window.count <= Math.max(1, limit);
    }

    private record Window(long minute, int count) {}
}
