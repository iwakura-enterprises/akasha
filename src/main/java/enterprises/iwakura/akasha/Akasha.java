package enterprises.iwakura.akasha;

import enterprises.iwakura.akasha.controller.DataSourceController;
import enterprises.iwakura.akasha.service.CacheService;
import enterprises.iwakura.akasha.service.DataSourceService;
import enterprises.iwakura.akasha.service.FileCacheService;
import enterprises.iwakura.akasha.service.PrometheusService;
import enterprises.iwakura.ganyu.Ganyu;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@RequiredArgsConstructor
public class Akasha {

    private final AkashaConfiguration configuration;

    private final DataSourceController dataSourceController;
    private final AkashaConfiguration akashaConfiguration;
    private final DataSourceService dataSourceService;
    private final PrometheusService prometheusService;
    private final FileCacheService fileCacheService;
    private final CacheService cacheService;
    private final Ganyu ganyu;

    public void start(String[] args) {
        log.info("Loading...");

        akashaConfiguration.init();
        ganyu.run();
        cacheService.init();
        fileCacheService.init();
        dataSourceService.validateDataSources();
        dataSourceController.registerRoutes();
        prometheusService.init();
    }
}
