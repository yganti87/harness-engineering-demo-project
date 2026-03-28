package com.library.config;

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metrics allowlist — add new metric names here before using them in service code.
 *
 * <p>Registers AOP aspects for {@code @Timed} and {@code @Counted} annotations and
 * a {@link MeterFilter} that denies any metric name not in the allowlist. This prevents
 * metrics sprawl by requiring all custom metrics to be declared upfront.
 *
 * <p>When adding a new metric:
 * <ol>
 *   <li>Add its name to {@code ALLOWED_METRICS} below.</li>
 *   <li>Use it in service code via {@code meterRegistry.counter(name, tags)} or
 *       {@code @Timed(value = name)}.</li>
 * </ol>
 *
 * <p>See docs/PATTERNS.md section "8. Metrics Pattern" for the full convention.
 */
@Configuration
public class MetricsConfig {

    private static final Set<String> ALLOWED_METRICS = Set.of(
        // JVM metrics
        "jvm.memory.used",
        "jvm.memory.max",
        "jvm.gc.pause",
        "jvm.threads.live",
        // System metrics
        "system.cpu.usage",
        "process.cpu.usage",
        "process.uptime",
        // HTTP metrics
        "http.server.requests",
        // HikariCP metrics
        "hikaricp.connections.active",
        "hikaricp.connections.idle",
        "hikaricp.connections.pending",
        // Custom auth counters
        "auth_registration_total",
        "auth_login_total",
        "auth_email_verification_total",
        "auth_resend_verification_total",
        "auth_verification_email_sent_total",
        // @Timed timers (Micrometer converts dots to underscores in Prometheus output)
        "auth.register",
        "auth.login",
        "auth.verify_email",
        "auth.resend_verification",
        "email.send_verification"
    );

    /**
     * Enables {@code @Timed} annotation support via AOP.
     *
     * @param registry the meter registry to record timings into
     * @return the {@link TimedAspect} bean
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * Enables {@code @Counted} annotation support via AOP.
     *
     * @param registry the meter registry to record counts into
     * @return the {@link CountedAspect} bean
     */
    @Bean
    public CountedAspect countedAspect(MeterRegistry registry) {
        return new CountedAspect(registry);
    }

    /**
     * Allowlist filter — denies any metric whose name is not in {@code ALLOWED_METRICS}.
     *
     * @return the {@link MeterFilter} bean
     */
    @Bean
    public MeterFilter metricsAllowlistFilter() {
        return new MeterFilter() {
            @Override
            public MeterFilterReply accept(Meter.Id id) {
                if (ALLOWED_METRICS.contains(id.getName())) {
                    return MeterFilterReply.NEUTRAL;
                }
                return MeterFilterReply.DENY;
            }
        };
    }

}
