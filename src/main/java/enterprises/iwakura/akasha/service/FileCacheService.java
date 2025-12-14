package enterprises.iwakura.akasha.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.UUID;

import enterprises.iwakura.akasha.AkashaConfiguration;
import enterprises.iwakura.akasha.object.DataSource;
import enterprises.iwakura.akasha.object.FileCacheContext;
import enterprises.iwakura.akasha.object.ReadContext;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@RequiredArgsConstructor
public class FileCacheService {

    private final List<FileCacheContext> fileCacheContexts = Collections.synchronizedList(new ArrayList<>());

    private final AkashaConfiguration configuration;
    private final Timer cacheCleanupTimer = new Timer("FileCacheCleanupTimer");

    public void init() {
        log.info("Initializing FileCacheService...");
        cleanUpOldCacheEntries();
        setupPeriodicCleanup();
    }

    private void setupPeriodicCleanup() {
        cacheCleanupTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                try {
                    cleanUpOldCacheEntries();
                } catch (Exception exception) {
                    log.error("Error during file cache cleanup", exception);
                }
            }
        }, 60000, 60000); // Run every 60 seconds
    }

    /**
     * Retrieves a file input stream from the cache if it exists.
     *
     * @param dataSource The data source of the file
     * @param filePath   The path of the file
     *
     * @return An Optional containing the InputStream if found, otherwise empty
     */
    public Optional<ReadContext> getFileInputStreamFromCache(DataSource dataSource, String filePath) {
        var optionalFileCacheContext = getFileCacheContext(dataSource, filePath);

        if (optionalFileCacheContext.isPresent()) {
            var fileCacheContext = optionalFileCacheContext.get();
            var cacheDirectoryPath = prepareCacheDirectory();
            var fileCachePath = cacheDirectoryPath.resolve(fileCacheContext.getFileId().toString());

            try {
                if (Files.exists(fileCachePath)) {
                    log.info("Cache hit for file: {} from data source: {}", filePath, dataSource.getName());
                    fileCacheContext.updateLastAccessTime();
                    var fileInputStream = Files.newInputStream(fileCachePath);
                    var fileName = Path.of(filePath).getFileName().toString();
                    var fileSizeBytes = Files.size(fileCachePath);
                    return Optional.of(new ReadContext(fileInputStream, fileName, fileSizeBytes));
                } else {
                    fileCacheContexts.remove(fileCacheContext);
                }
            } catch (IOException exception) {
                log.error("Failed to read cached file at {}", fileCachePath.toAbsolutePath(), exception);
            }
        }

        return Optional.empty();
    }

    /**
     * Cleans up old cache entries based on TTL configuration.
     */
    private void cleanUpOldCacheEntries() {
        var cacheDirectoryPath = prepareCacheDirectory();

        try (var fileStream = Files.list(cacheDirectoryPath)) {
            fileStream.forEach(fileCachePath -> {
                var fileName = fileCachePath.getFileName().toString();
                UUID fileId;

                try {
                    fileId = UUID.fromString(fileName);
                } catch (Exception exception) {
                    log.warn("Failed to parse file ID from cache file name: {}", fileName, exception);
                    return;
                }

                var optionalFileCacheContext = getFileCacheContext(fileId);

                if (optionalFileCacheContext.isEmpty()) {
                    log.warn("Removing stale cache file: {}", fileCachePath.toAbsolutePath());
                    try {
                        Files.deleteIfExists(fileCachePath);
                    } catch (IOException exception) {
                        log.error("Failed to delete stale cache file at {}", fileCachePath.toAbsolutePath(), exception);
                    }
                } else {
                    var fileCacheContext = optionalFileCacheContext.get();
                    if (fileCacheContext.shouldDelete(configuration.get().getFileCache().getTtlSeconds())) {
                        log.info("Removing expired cache file: {}", fileCachePath.toAbsolutePath());
                        try {
                            Files.deleteIfExists(fileCachePath);
                            fileCacheContexts.remove(fileCacheContext);
                        } catch (IOException exception) {
                            log.error("Failed to delete expired cache file at {}", fileCachePath.toAbsolutePath(),
                                exception);
                        }
                    }
                }
            });
        } catch (Exception exception) {
            log.error("Failed to list files in cache directory at {}", cacheDirectoryPath.toAbsolutePath(), exception);
        }
    }

    private Optional<FileCacheContext> getFileCacheContext(UUID fileId) {
        return fileCacheContexts.stream()
            .filter(context -> context.getFileId().equals(fileId))
            .findFirst();
    }

    private Optional<FileCacheContext> getFileCacheContext(DataSource dataSource, String filePath) {
        return fileCacheContexts.stream()
            .filter(context -> context.getDataSource().equals(dataSource) && context.getFilePath().equals(filePath))
            .findFirst();
    }

    private long countCachedFileSizes() {
        return fileCacheContexts.stream()
            .mapToLong(FileCacheContext::getFileSizeBytes)
            .sum();
    }

    /**
     * Prepare the cache directory.
     *
     * @return Path to the cache directory
     */
    private Path prepareCacheDirectory() {
        var cacheDirectoryPath = Path.of(configuration.get().getFileCache().getDirectory());

        try {
            Files.createDirectories(cacheDirectoryPath);
            return cacheDirectoryPath;
        } catch (IOException exception) {
            log.error("Failed to create cache directory at {}", cacheDirectoryPath.toAbsolutePath(), exception);
            throw new RuntimeException("Failed to create cache directory", exception);
        }
    }

    /**
     * Caches the file input stream if caching is enabled and conditions are met.
     *
     * @param dataSource  Data source
     * @param filePath    File path
     * @param readContext Read context containing the input stream and file size
     *
     * @return InputStream that caches data while being read
     */
    public InputStream cacheFileInputStream(DataSource dataSource, String filePath, ReadContext readContext) {
        var fileCacheConfiguration = configuration.get().getFileCache();

        // File cache enabled?
        if (fileCacheConfiguration.isEnabled()) {
            // Is the file size within limits?
            if (fileCacheConfiguration.getMaxSizePerFileBytes() > readContext.getFileSizeBytes()) {
                // Will adding this file exceed total cache size?
                var totalFileCacheSizeBytesWithNewFile = countCachedFileSizes() + readContext.getFileSizeBytes();
                if (fileCacheConfiguration.getMaxTotalSizeBytes() > totalFileCacheSizeBytesWithNewFile) {
                    // Create new InputStream, that will accept the original one. It will write to cache while being
                    // read.
                    var cacheDirectoryPath = prepareCacheDirectory();
                    var fileCacheContext = new FileCacheContext(dataSource, filePath);
                    var fileCachePath = cacheDirectoryPath.resolve(fileCacheContext.getFileId().toString());
                    try {
                        var fileOutputStream = Files.newOutputStream(fileCachePath);
                        var cachingInputStream = new CachingInputStream(readContext.getInputStream(), fileOutputStream,
                            () -> {
                                fileCacheContext.setFileSizeBytes(readContext.getFileSizeBytes());
                                fileCacheContexts.add(fileCacheContext);
                            });
                        log.info("Caching file: {} from data source: {} to {}", filePath, dataSource.getName(),
                            fileCachePath.toAbsolutePath());
                        return cachingInputStream;
                    } catch (Exception exception) {
                        log.error("Failed to create cache file at {}, not caching", fileCachePath.toAbsolutePath(),
                            exception);
                    }
                }
            }
        }

        // Ignore caching if disabled
        return readContext.getInputStream();
    }

    @RequiredArgsConstructor
    public static class CachingInputStream extends InputStream {

        private final InputStream originalInputStream;
        private final OutputStream fileCacheOutputStream;
        private final Runnable onFullyDownloaded;

        @Override
        public int read() throws IOException {
            int byteRead = originalInputStream.read();
            if (byteRead != -1) {
                fileCacheOutputStream.write(byteRead);
            } else {
                closeStreams();
            }
            return byteRead;
        }

        private void closeStreams() {
            try {
                originalInputStream.close();
            } catch (IOException e) {
                log.error("Failed to close original input stream", e);
            }
            try {
                fileCacheOutputStream.close();
            } catch (IOException e) {
                log.error("Failed to close file cache output stream", e);
            }
            onFullyDownloaded.run();
        }
    }
}
