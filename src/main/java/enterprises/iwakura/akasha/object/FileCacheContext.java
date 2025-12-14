package enterprises.iwakura.akasha.object;

import java.util.UUID;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class FileCacheContext {

    private final DataSource dataSource;
    private final String filePath;
    private UUID fileId = UUID.randomUUID();
    private long fileSizeBytes;
    private long lastAccessAtMillis = System.currentTimeMillis();

    /**
     * Determine if the cache entry should be deleted based on its age and the provided TTL.
     *
     * @param ttlSeconds Time-to-live in seconds
     *
     * @return true if the cache entry should be deleted, false otherwise
     */
    public boolean shouldDelete(long ttlSeconds) {
        var currentTimeMillis = System.currentTimeMillis();
        var ageMillis = currentTimeMillis - lastAccessAtMillis;
        var ttlMillis = ttlSeconds * 1000;
        return ageMillis > ttlMillis;
    }

    /**
     * Updates the last access time to the current time.
     */
    public void updateLastAccessTime() {
        this.lastAccessAtMillis = System.currentTimeMillis();
    }
}
