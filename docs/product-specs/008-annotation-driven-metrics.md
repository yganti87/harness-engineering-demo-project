# Product Spec 008: Annotation-Driven Metrics with Cost-Controlled Export

**Feature ID**: F008
**Status**: planned

## User Story

As a backend engineer, I want service-layer metrics declared with `@Timed` and `@Counted` annotations and a Prometheus allowlist enforced in code, so that adding metrics requires no boilerplate and only explicitly approved metrics are ever exported, preventing cost surprises.

## Summary

`AuthServiceImpl` and `EmailServiceImpl` currently declare 14 `Counter` fields initialised in `@PostConstruct` methods. This creates boilerplate, requires fragile reflection-based test setup, and is not enforced by any convention. Additionally, all Micrometer meters are exported to Prometheus by default, which can result in unexpected cardinality and cost. This feature introduces annotation-driven metrics infrastructure (`@Timed` / `@Counted` via AOP), refactors both service classes to remove field-level counters, and adds a `MeterFilter` allowlist that makes exporting any new metric an explicit, reviewable act. ArchUnit rules are added to permanently prevent the old pattern from re-entering any service class.

## UX Design

This feature has no user-visible UI changes. The observable surface is the Prometheus metrics endpoint.

### Before (current state)

```
GET /actuator/prometheus

# All Micrometer meters exported — logback.events, tomcat.*, jvm.*, auth_*, ...
auth_registration_total{status="success",...} 1.0
logback_events_total{level="warn",...} 3.0    <- cost-generating noise
tomcat_threads_current_threads{...} 200.0      <- unwanted
```

### After (target state)

```
GET /actuator/prometheus

# Only allowlisted meters appear
jvm_memory_used_bytes{...} 12345.0
http_server_requests_seconds_count{...} 42.0
auth_register_seconds_count{...} 5.0           <- @Timed on AuthServiceImpl.register()
auth_registration_total{status="success",...} 1.0
# logback.events, tomcat.* -> absent
```

## API Contract

No new REST endpoints. The existing Prometheus scrape endpoint is unchanged:

`GET /actuator/prometheus`

## Technical Design

### MetricsConfig (new)

Registered in `com.library.config` (config layer — allowed to be imported by service layer):

- `TimedAspect` bean — enables `@Timed` on any Spring-managed bean
- `CountedAspect` bean — enables `@Counted` on any Spring-managed bean
- `MeterFilter` bean — denies all meters not in the explicit allowlist

### Allowlist (default)

| Group | Metric names |
|-------|-------------|
| JVM | `jvm.memory.used`, `jvm.memory.max`, `jvm.gc.pause`, `jvm.threads.live` |
| System | `system.cpu.usage`, `process.cpu.usage`, `process.uptime` |
| HTTP | `http.server.requests` |
| HikariCP | `hikaricp.connections.active`, `hikaricp.connections.idle`, `hikaricp.connections.pending` |
| Auth counters | `auth_registration_total`, `auth_login_total`, `auth_email_verification_total`, `auth_resend_verification_total`, `auth_verification_email_sent_total` |
| @Timed (auth) | `auth.register`, `auth.login`, `auth.verify_email`, `auth.resend_verification` |
| @Timed (email) | `email.send_verification` |

### AuthServiceImpl refactor

- Remove 12 `Counter` fields and `@PostConstruct initMetrics()`
- Add `@Timed("auth.register")`, `@Timed("auth.login")`, `@Timed("auth.verify_email")`, `@Timed("auth.resend_verification")` on public methods
- Replace `counterField.increment()` with `meterRegistry.counter(name, tags).increment()` inline

### EmailServiceImpl refactor

- Remove 2 `Counter` fields and `@PostConstruct`
- Add `@Timed("email.send_verification")` on `sendVerificationEmail()`
- Replace counter increments with inline `meterRegistry.counter(name, tags).increment()`

### ArchUnit rules (LayerDependencyTest additions)

Two new rules to enforce the annotation-driven convention:

1. **No Counter/Timer/Gauge fields in service classes** — forces inline `meterRegistry.counter()` usage
2. **No @PostConstruct in service classes** — forces constructor injection for all initialization

## Acceptance Criteria

See `features.json` entry F008.

## Out of Scope

- Adding metrics to `BookService` or other services (future work, follows the new pattern)
- Grafana dashboard configuration
- Alerting rules
- Metrics for controllers or repositories (metrics belong in the service layer per architecture)
