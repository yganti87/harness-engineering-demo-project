package com.library.config;

import java.util.Set;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds the metrics allowlist from {@code metrics-allowlist.yml}.
 *
 * <p>See docs/PATTERNS.md section "8. Metrics Pattern" for usage.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.metrics")
public class MetricsProperties {

    /**
     * Metric names that are allowed through the MeterFilter.
     * All other metrics are denied (not exported to Prometheus).
     */
    private Set<String> allowed = Set.of();
}
