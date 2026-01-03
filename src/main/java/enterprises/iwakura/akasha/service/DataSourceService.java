package enterprises.iwakura.akasha.service;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import enterprises.iwakura.akasha.AkashaConfiguration;
import enterprises.iwakura.akasha.exception.HandledException;
import enterprises.iwakura.akasha.object.DataSource;
import enterprises.iwakura.akasha.object.ReadContext;
import enterprises.iwakura.akasha.service.handler.DataSourceHandler;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import io.javalin.http.Context;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@RequiredArgsConstructor
public class DataSourceService {

    private final AkashaConfiguration configuration;
    private final PermissionService permissionService;
    private final FileCacheService fileCacheService;
    private final ResponseHandlerService responseHandlerService;
    private final PrometheusService prometheusService;

    @SuppressWarnings("rawtypes")
    private final List<DataSourceHandler> dataSourceHandlers;

    /**
     * Returns a data source if it exists and the user has permission for the specified file path.
     *
     * @param dataSourceName Data source name
     * @param filePath       File path
     * @param write          Whether the operation is a write operation
     * @param ctx            Javalin context
     *
     * @return Data source or null if not found or no permission
     */
    private DataSource getDataSource(String dataSourceName, String filePath, boolean write, Context ctx) {
        if (dataSourceName == null || dataSourceName.isEmpty() || filePath == null || filePath.isEmpty()) {
            responseHandlerService.respondWithMessage(ctx, 400, "Data source name and file path must be provided");
            return null;
        }

        var optionalDataSource = configuration.getDataSource().getSources()
            .stream()
            .filter(dataSource -> dataSource.getName().equals(dataSourceName))
            .findFirst();

        if (optionalDataSource.isPresent()) {
            var dataSource = optionalDataSource.get();

            if (!permissionService.hasPermission(dataSource, filePath, write, ctx)) {
                responseHandlerService.respondWithMessage(ctx, 403, "You do not have permission to access this data source or file path");
                log.warn("[{}] [{}] Permission denied for file path: {}", dataSourceName, ctx.ip(), filePath);
                return null;
            }

            return dataSource;
        } else {
            responseHandlerService.respondWithMessage(ctx, 404, "Data source not found: " + dataSourceName);
            log.warn("[{}] [{}] Data source not found", dataSourceName, ctx.ip());
            return null;
        }
    }

    /**
     * Reads a file from the specified data source and file path, verifying permissions, utilizing caching and
     * then responding via the provided Javalin context with input stream and appropriate headers.
     *
     * @param dataSourceName Data source name
     * @param filePath       File path
     * @param ctx            Javalin context
     */
    public void read(String dataSourceName, String filePath, Context ctx) {
        var dataSource = getDataSource(dataSourceName, filePath, false, ctx);

        if (dataSource != null) {
            var optionalDataSourceHandler = findDataSourceHandler(dataSource);

            if (optionalDataSourceHandler.isPresent()) {
                var handler = optionalDataSourceHandler.get();
                ReadContext readContext;

                try {
                    var fileCacheReadContext = fileCacheService.getFileInputStreamFromCache(dataSource, filePath);
                    if (fileCacheReadContext.isPresent()) {
                        readContext = fileCacheReadContext.get();
                        log.info("[{}] [{}] Loading cache file {} of size {} bytes", dataSourceName, ctx.ip(), filePath,
                            readContext.getFileSizeBytes());
                    } else {
                        readContext = handler.read(dataSource, filePath);
                        log.info("[{}] [{}] Downloading file {} of size {} bytes", dataSourceName, ctx.ip(), filePath,
                            readContext.getFileSizeBytes());
                        readContext.setInputStream(
                            fileCacheService.cacheFileInputStream(dataSource, filePath, readContext));
                    }

                    responseHandlerService.respondWithReadContext(readContext, ctx);
                } catch (HandledException exception) {
                    responseHandlerService.respondWithMessage(ctx, exception.isNotFound() ? 404 : 400, "Error reading from data source: " + exception.getMessage());
                    log.warn("Handled error reading data source: {} with file path: {}: {}", dataSourceName,
                        filePath, exception.getMessage());
                } catch (Exception exception) {
                    responseHandlerService.respondWithMessage(ctx, 500, "An unexpected error occurred while reading the file");
                    log.error("Error reading data source: {} with file path: {}", dataSourceName, filePath,
                        exception);
                } finally {
                    prometheusService.getCollectors().getReads().labelValues(dataSource.getName(), filePath).inc();
                }
            } else {
                responseHandlerService.respondWithMessage(ctx, 500, "No handler found for data source type: " + dataSource.getType());
                log.error("[{}] [{}] BUG! No handler found for data source type: {}", dataSourceName, ctx.ip(), dataSource.getType());
            }
        }
    }

    /**
     * Writes a file to the specified data source and file path, verifying permissions, from the provided input stream
     * and responding via the provided Javalin context.
     *
     * @param dataSourceName Data source name
     * @param filePath       File path
     * @param inputStream    Input stream
     * @param ctx            Javalin context
     */
    public void write(String dataSourceName, String filePath, InputStream inputStream, @NotNull Context ctx) {
        var dataSource = getDataSource(dataSourceName, filePath, true, ctx);

        if (dataSource != null) {
            var optionalDataSourceHandler = findDataSourceHandler(dataSource);

            if (optionalDataSourceHandler.isPresent()) {
                var handler = optionalDataSourceHandler.get();

                try {
                    var fileSizeBytes = Long.parseLong(ctx.req().getHeader("Content-Length"));
                    log.info("[{}] [{}] Uploading file of size {} bytes to path {}", dataSourceName, ctx.ip(), fileSizeBytes,
                        filePath);
                    handler.write(dataSource, filePath, inputStream);
                    responseHandlerService.respondWithMessage(ctx, 200, "File written successfully");
                } catch (HandledException exception) {
                    responseHandlerService.respondWithMessage(ctx, exception.isNotFound() ? 404 : 400, "Error writing to data source: " + exception.getMessage());
                    log.error("Error writing to data source: {} with file path: {}: {}", dataSourceName, filePath,
                        exception.getMessage());
                } catch (Exception exception) {
                    responseHandlerService.respondWithMessage(ctx, 500, "An unexpected error occurred while writing the file");
                    log.error("Error writing to data source: {} with file path: {}", dataSourceName, filePath,
                        exception);
                } finally {
                    prometheusService.getCollectors().getWrites().labelValues(dataSource.getName(), filePath).inc();
                }
            } else {
                responseHandlerService.respondWithMessage(ctx, 500, "No handler found for data source type: " + dataSource.getType());
                log.error("[{}] [{}] BUG! No handler found for data source type: {}", dataSourceName, ctx.ip(), dataSource.getType());
            }
        }
    }

    /**
     * Finds the appropriate data source handler for the given data source.
     *
     * @param dataSource Data source
     * @param <T>        Data source type
     *
     * @return Optional data source handler
     */
    private <T extends DataSource> Optional<DataSourceHandler<T>> findDataSourceHandler(T dataSource) {
        //noinspection unchecked
        return dataSourceHandlers.stream()
            .filter(handler -> handler.getType() == dataSource.getType())
            .map(handler -> (DataSourceHandler<T>) handler)
            .findFirst();
    }

    /**
     * Validates data source configurations.
     */
    public void validateDataSources() {
        var dataSourceConfiguration = configuration.getDataSource();

        StringBuilder writableDataSourcesWithoutTokens = new StringBuilder();

        for (var dataSource : dataSourceConfiguration.getSources()) {
            for (var entry : dataSource.getPermission().getEntries()) {
                if (entry.isWrite() && (entry.getTokens() == null || entry.getTokens().isEmpty())) {
                    writableDataSourcesWithoutTokens.append("%s=%s, ".formatted(dataSource.getName(), entry.getPath()));
                }
            }
        }

        if (!writableDataSourcesWithoutTokens.isEmpty()) {
            var paths = writableDataSourcesWithoutTokens.substring(0, writableDataSourcesWithoutTokens.length() - 2);

            if (dataSourceConfiguration.isValidateWritePermissionEntriesHaveToken()) {
                throw new IllegalStateException(("There are writable data source permission entries that do not have tokens specified, "
                    + "this could be a security risk. (To ignore this warning see ./configs/data_source.json) Affected data source paths: %s")
                    .formatted(paths));
            } else {
                log.warn("There are writable data source permission entries that do not have tokens specified, "
                    + "this could be a security risk. Affected data source paths: {}", paths);
            }
        }
    }
}
