package enterprises.iwakura.akasha.service;

import enterprises.iwakura.akasha.AkashaConfiguration;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.exporter.servlet.jakarta.PrometheusMetricsServlet;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@Getter
@RequiredArgsConstructor
public class PrometheusService {

    private final PrometheusRegistry prometheusRegistry = new PrometheusRegistry();
    private final PrometheusMetricsServlet prometheusMetricsServlet = new PrometheusMetricsServlet(prometheusRegistry);
    private final Collectors collectors = new Collectors();

    private final AkashaConfiguration configuration;

    /**
     * Initializes the Prometheus service
     */
    public void init() {
        log.info("Initializing Prometheus service...");

        var prometheusConfiguration = configuration.getPrometheus();

        if (prometheusConfiguration.isJvmMetricsEnabled()) {
            JvmMetrics.builder().register(prometheusRegistry);
        }

        collectors.init(prometheusRegistry);
    }

    @Getter
    public static class Collectors {

        private final Counter reads = Counter.builder()
            .name("akasha_reads")
            .help("Counter for read operations")
            .labelNames("data_source", "file_path")
            .build();

        private final Counter writes = Counter.builder()
            .name("akasha_writes")
            .help("Counter for write operations")
            .labelNames("data_source", "file_path")
            .build();

        public void init(PrometheusRegistry prometheusRegistry) {
            prometheusRegistry.register(reads);
            prometheusRegistry.register(writes);
        }
    }
}
