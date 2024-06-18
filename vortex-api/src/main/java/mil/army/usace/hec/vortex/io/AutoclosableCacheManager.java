package mil.army.usace.hec.vortex.io;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.logging.Logger;

public final class AutoclosableCacheManager<K, T extends AutoCloseable> {
    private static final Logger logger = Logger.getLogger(AutoclosableCacheManager.class.getName());
    private static final long DEFAULT_SECONDS_BEFORE_EXPIRATION = 300;

    private final long expirationSeconds;
    private final Map<K, T> cache = new ConcurrentHashMap<>();

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private Future<?> future;

    private AutoclosableCacheManager(long expirationSeconds) {
        this.expirationSeconds = expirationSeconds;
    }

    public static <K, T extends AutoCloseable> AutoclosableCacheManager<K, T> create() {
        return new AutoclosableCacheManager<>(DEFAULT_SECONDS_BEFORE_EXPIRATION);
    }

    public void put(K key, T value) {
        cache.put(key, value);
        scheduleExpiration(key);
    }

    public T get(K key) {
        scheduleExpiration(key);
        return cache.getOrDefault(key, null);
    }

    public void remove(K key) {
        T resource = cache.getOrDefault(key, null);
        if (resource == null) {
            return;
        }

        try {
            resource.close();
        } catch (Exception e) {
            logger.warning(e.getMessage());
        } finally {
            cache.remove(key, resource);
        }
    }

    private void scheduleExpiration(K key) {
        Optional.ofNullable(future).ifPresent(f -> f.cancel(true));
        Runnable runnable = () -> remove(key);
        future = executor.schedule(runnable, expirationSeconds, TimeUnit.SECONDS);
    }
}
