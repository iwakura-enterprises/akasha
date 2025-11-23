package enterprises.iwakura.akasha.service;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import enterprises.iwakura.akasha.AkashaConfiguration;
import enterprises.iwakura.akasha.exception.HandledException;
import enterprises.iwakura.akasha.object.DataSource;
import enterprises.iwakura.akasha.object.DataSourceType;
import enterprises.iwakura.akasha.service.handler.DataSourceHandler;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import enterprises.iwakura.sigewine.core.utils.collections.TypedArrayList;
import io.javalin.http.Context;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@RequiredArgsConstructor
public class DataSourceService {

    private final AkashaConfiguration configuration;
    private final PermissionService permissionService;

    @Bean
    private final List<DataSourceHandler> dataSourceHandlers = new TypedArrayList<>(DataSourceHandler.class);

    public DataSource getDataSource(String dataSourceName, String filePath, boolean write, Context ctx) {
        if (dataSourceName == null || dataSourceName.isEmpty() || filePath == null || filePath.isEmpty()) {
            ctx.status(400).result("Data source name and file path must be provided");
            return null;
        }

        var optionalDataSource = findDataSource(dataSourceName);

        if (optionalDataSource.isPresent()) {
            var dataSource = optionalDataSource.get();

            if (!permissionService.hasPermission(dataSource, filePath, write, ctx)) {
                ctx.status(403).result("You do not have permission to access this data source or file path");
                log.warn("[{}] [{}] Permission denied for file path: {}", dataSourceName, ctx.ip(), filePath);
                return null;
            }

            return dataSource;
        } else {
            log.warn("[{}] [{}] Data source not found", dataSourceName, ctx.ip());
            ctx.status(404).result("Data source not found: " + dataSourceName);
            return null;
        }
    }

    public void read(String dataSourceName, String filePath, Context ctx) {
        var dataSource = getDataSource(dataSourceName, filePath, false, ctx);

        if (dataSource != null) {
            var optionalDataSourceHandler = findDataSourceHandler(dataSource);

            if (optionalDataSourceHandler.isPresent()) {
                var handler = optionalDataSourceHandler.get();

                try {
                    var readContext = handler.read(dataSource, filePath);
                    ctx.contentType("application/octet-stream");
                    ctx.header("Content-Disposition", "attachment; filename=\"" + readContext.getFileName() + "\"");
                    ctx.header("Content-Length", String.valueOf(readContext.getFileSizeBytes()));
                    ctx.result(readContext.getInputStream());
                    ctx.status(200);
                    log.info("[{}] [{}] Downloaded file {} of size {} bytes", dataSourceName, ctx.ip(), filePath,
                        readContext.getFileSizeBytes());
                } catch (HandledException exception) {
                    ctx.status(400).result("Error reading from data source: " + exception.getMessage());
                    log.warn("Handled error reading data source: {} with file path: {}: {}", dataSourceName,
                        filePath, exception.getMessage());
                } catch (Exception exception) {
                    ctx.status(500).result("An unexpected error occurred while reading the file");
                    log.error("Error reading data source: {} with file path: {}", dataSourceName, filePath,
                        exception);
                }
            } else {
                log.error("[{}] [{}] BUG! No handler found for data source type: {}", dataSourceName, ctx.ip(),
                    dataSource.getType());
                ctx.status(500).result("No handler found for data source type: " + dataSource.getType());
            }
        }
    }

    public void write(String dataSourceName, String filePath, InputStream inputStream, @NotNull Context ctx) {
        var dataSource = getDataSource(dataSourceName, filePath, true, ctx);

        if (dataSource != null) {
            var optionalDataSourceHandler = findDataSourceHandler(dataSource);

            if (optionalDataSourceHandler.isPresent()) {
                var handler = optionalDataSourceHandler.get();

                try {
                    var fileSizeBytes = inputStream.available();
                    log.info("[{}] [{}] Uploading file of size {} to path {}", dataSourceName, ctx.ip(), fileSizeBytes, filePath);
                    handler.write(dataSource, filePath, inputStream);
                    ctx.status(200).result("File written successfully");
                    log.info("[{}] [{}] Uploaded file of size {} to path {}", dataSourceName, ctx.ip(), filePath, fileSizeBytes);
                } catch (HandledException exception) {
                    ctx.status(400).result("Error writing to data source: " + exception.getMessage());
                    log.error("Error writing to data source: {} with file path: {}: {}", dataSourceName, filePath, exception.getMessage());
                } catch (Exception exception) {
                    ctx.status(500).result("An unexpected error occurred while writing the file");
                    log.error("Error writing to data source: {} with file path: {}", dataSourceName, filePath, exception);
                }
            } else {
                ctx.status(500).result("No handler found for data source type: " + dataSource.getType());
            }
        }
    }

    public Optional<DataSource> findDataSource(String dataSourceName) {
        return configuration.get().getSources()
            .stream()
            .filter(dataSource -> dataSource.getName().equals(dataSourceName))
            .findFirst();
    }

    private <T extends DataSource> Optional<DataSourceHandler<T>> findDataSourceHandler(T dataSource) {
        return dataSourceHandlers.stream()
            .filter(handler -> handler.getType() == dataSource.getType())
            .map(handler -> (DataSourceHandler<T>) handler)
            .findFirst();
    }
}
