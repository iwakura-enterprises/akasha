package enterprises.iwakura.akasha.config;

import lombok.Data;

@Data
public class FileCacheConfiguration {

    private boolean enabled = true;
    private boolean resetTtlOnAccess = true;
    private String directory = "./file_cache/";
    private long ttlSeconds = 3600; // 1 hour
    private long maxSizePerFileBytes = 10 * 1024 * 1024; // 10 MB
    private long maxTotalSizeBytes = 1000 * 1024 * 1024; // 1000 MB
    private long httpCacheMaxAgeSeconds = 3600; // 1 hour

}
