package enterprises.iwakura.akasha;

import java.nio.file.Path;

import com.google.gson.Gson;

import enterprises.iwakura.akasha.config.DataSourceConfiguration;
import enterprises.iwakura.akasha.config.FileCacheConfiguration;
import enterprises.iwakura.akasha.config.JavalinConfiguration;
import enterprises.iwakura.akasha.config.PrometheusConfiguration;
import enterprises.iwakura.akasha.object.SftpDataSource;
import enterprises.iwakura.jean.Jean;
import enterprises.iwakura.jean.LoadOptions;
import enterprises.iwakura.jean.serializer.GsonSerializer;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@Getter
public class AkashaConfiguration extends Jean {

    private final static Path CONFIG_DIRECTORY_PATH = Path.of("./configs/");

    public AkashaConfiguration(Gson gson) {
        super(
            CONFIG_DIRECTORY_PATH,
            new GsonSerializer(gson),
            LoadOptions.builder().saveOnLoad(true).build()
        );
    }

    /**
     * Initializes all Akasha configurations by loading them.
     */
    public void init() {
        log.info("Initializing Akasha configurations...");
        this.getJavalin();
        this.getFileCache();
        this.getDataSource();
        log.info("Akasha configurations initialized.");
    }

    /**
     * {@link #getOrLoad(String, Class)}s and returns the {@link JavalinConfiguration}.
     *
     * @return the Javalin configuration
     */
    public JavalinConfiguration getJavalin() {
        return this.getOrLoad("javalin", JavalinConfiguration.class);
    }

    /**
     * {@link #getOrLoad(String, Class)}s and returns the {@link FileCacheConfiguration}.
     *
     * @return the file cache configuration
     */
    public FileCacheConfiguration getFileCache() {
        return this.getOrLoad("file_cache", FileCacheConfiguration.class);
    }

    /**
     * {@link #getOrLoad(String, Class)}s and returns the {@link DataSourceConfiguration}.
     *
     * @return the data source configuration
     */
    public DataSourceConfiguration getDataSource() {
        var dataSourceConfiguration = this.getOrLoad("data_source", DataSourceConfiguration.class);

        if (dataSourceConfiguration.getSources().isEmpty()) {
            log.warn("No data sources found in configuration. Adding default SFTP data source for first-time setup.");
            dataSourceConfiguration.getSources().add(SftpDataSource.example());
            save("data_source", dataSourceConfiguration);
        }

        return dataSourceConfiguration;
    }

    /**
     * {@link #getOrLoad(String, Class)}s and returns the {@link PrometheusConfiguration}.
     *
     * @return the Prometheus configuration
     */
    public PrometheusConfiguration getPrometheus() {
        return this.getOrLoad("prometheus", PrometheusConfiguration.class);
    }
}
