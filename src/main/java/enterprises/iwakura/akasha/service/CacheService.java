package enterprises.iwakura.akasha.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.function.Consumer;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@RequiredArgsConstructor
public class CacheService {

    private static final List<Cache<?, ?>> caches = new ArrayList<>();
    private static final Timer cacheCleanupTimer = new Timer("CacheCleanupTimer");

    @SuppressWarnings("unchecked")
    public static <K, V> Cache<K, V> createCache(Consumer<CacheBuilder<K, V>> builderConsumer) {
        CacheBuilder<K, V> builder = (CacheBuilder<K, V>) CacheBuilder.newBuilder();
        builderConsumer.accept(builder);
        Cache<K, V> cache = builder.build();
        caches.add(cache);
        return cache;
    }

    public void init() {
        log.info("Initializing CacheService...");

        cacheCleanupTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                for (Cache<?, ?> cache : caches) {
                    try {
                        cache.cleanUp();
                    } catch (Exception e) {
                        log.error("Error during cache cleanup", e);
                    }
                }
            }
        }, 60000, 60000); // Run every 60 seconds
    }
}
