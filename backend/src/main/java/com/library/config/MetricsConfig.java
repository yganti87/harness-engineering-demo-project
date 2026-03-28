package com.library.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metrics configuration — AOP support and allowlist filter.
 *
 * <p>The allowlist is externalized to {@code metrics-allowlist.yml} and bound
 * via {@link MetricsProperties}. Add new metric names there before using them
 * in service code. Common tags (application, environment) are configured in
 * {@code application.yml} under {@code management.metrics.tags}.
 *
 * <p>See docs/PATTERNS.md section "8. Metrics Pattern" for the full convention.
 */
@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    private final MetricsProperties metricsProperties;

    /**
     * Enables {@code @Timed} annotation support via AOP.
     * Spring Boot 3.2 does not auto-configure this bean.
     *
     * @param registry the meter registry to record timings into
     * @return the {@link TimedAspect} bean
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * Allowlist filter — denies any metric whose name is not in
     * {@code app.metrics.allowed} from {@code metrics-allowlist.yml}.
     *
     * @return the {@link MeterFilter} bean
     */
    @Bean
    public MeterFilter metricsAllowlistFilter() {
        return new MeterFilter() {
            @Override
            public MeterFilterReply accept(Meter.Id id) {
                if (metricsProperties.getAllowed().contains(id.getName())) {
                    return MeterFilterReply.NEUTRAL;
                }
                return MeterFilterReply.DENY;
            }
        };
    }
}
