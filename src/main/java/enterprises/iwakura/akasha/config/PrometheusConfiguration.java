package enterprises.iwakura.akasha.config;

import java.util.UUID;

import lombok.Data;

@Data
public class PrometheusConfiguration {

    private boolean enabled = true;
    private boolean jvmMetricsEnabled = true;
    private String bearerToken = UUID.randomUUID().toString();

    /**
     * Checks if the bearer token is set and not blank.
     *
     * @return True if the bearer token is set and not blank, false otherwise.
     */
    public boolean isBearerTokenSet() {
        return bearerToken != null && !bearerToken.isBlank();
    }
}
