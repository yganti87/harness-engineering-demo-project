# Execution Plan 008: Annotation-Driven Metrics Refactoring (F008)

**Feature**: F008 — Annotation-Driven Metrics with Cost-Controlled Export
**Status**: completed
**Product Spec**: [008-annotation-driven-metrics.md](../../product-specs/008-annotation-driven-metrics.md)
**Started**: 2026-03-27

## Goal

Refactor Micrometer metrics in `AuthServiceImpl` and `EmailServiceImpl` from pre-registered `Counter` fields with `@PostConstruct` initialization to annotation-driven `@Timed` and inline `meterRegistry.counter()` calls. Add a `MetricsConfig` that registers AOP aspects (`TimedAspect`, `CountedAspect`) and a `MeterFilter` allowlist to control which metrics are exposed to Prometheus. Enforce the new convention in `LayerDependencyTest` via ArchUnit rules that ban metric fields and `@PostConstruct` in the service layer. Document the pattern in `docs/PATTERNS.md` and add pitfall entries to `docs/COMMON_PITFALLS.md`.

## Acceptance Criteria

From `features.json` entry F008: 11 criteria.

## Implementation Steps

### Phase 1: MetricsConfig — AOP beans + metrics allowlist

- [x] **New file**: `backend/src/main/java/com/library/config/MetricsConfig.java`
  - Annotate with `@Configuration`
  - Register `TimedAspect` bean: `@Bean public TimedAspect timedAspect(MeterRegistry r)`
  - Register `CountedAspect` bean: `@Bean public CountedAspect countedAspect(MeterRegistry r)`
  - Register `MeterFilter` bean with an allowlist:
    - JVM: `jvm.memory.used`, `jvm.memory.max`, `jvm.gc.pause`, `jvm.threads.live`
    - System: `system.cpu.usage`, `process.cpu.usage`, `process.uptime`
    - HTTP: `http.server.requests`
    - HikariCP: `hikaricp.connections.active`, `hikaricp.connections.idle`, `hikaricp.connections.pending`
    - Custom counters: `auth_registration_total`, `auth_login_total`, `auth_email_verification_total`, `auth_resend_verification_total`, `auth_verification_email_sent_total`
    - `@Timed` timers: `auth.register`, `auth.login`, `auth.verify_email`, `auth.resend_verification`, `email.send_verification`
  - Deny all other metric names by returning `MeterFilterReply.DENY`
  - Add Javadoc: "Metrics allowlist — add new metric names here before using them in service code"

### Phase 2: Refactor AuthServiceImpl

- [x] **File**: `backend/src/main/java/com/library/service/AuthServiceImpl.java`
  - Remove 12 `Counter` fields (lines 46-63)
  - Remove `@PostConstruct initMetrics()` method (lines 65-94)
  - Remove `import io.micrometer.core.instrument.Counter`
  - Remove `import jakarta.annotation.PostConstruct`
  - Add `import io.micrometer.core.annotation.Timed`
  - Add `@Timed(value = "auth.register")` on `register()` method
  - Add `@Timed(value = "auth.login")` on `login()` method
  - Add `@Timed(value = "auth.verify_email")` on `verifyEmail()` method
  - Add `@Timed(value = "auth.resend_verification")` on `resendVerification()` method
  - Replace all counter field `.increment()` calls with inline `meterRegistry.counter(...)` calls:
    - `regSuccessCounter.increment()` -> `meterRegistry.counter("auth_registration_total", "status", "success").increment()`
    - `regDuplicateCounter.increment()` -> `meterRegistry.counter("auth_registration_total", "status", "duplicate_email").increment()`
    - `regErrorCounter.increment()` -> `meterRegistry.counter("auth_registration_total", "status", "error").increment()`
    - `loginSuccessCounter.increment()` -> `meterRegistry.counter("auth_login_total", "status", "success").increment()`
    - `loginUnverifiedCounter.increment()` -> `meterRegistry.counter("auth_login_total", "status", "unverified").increment()`
    - `loginInvalidCounter.increment()` -> `meterRegistry.counter("auth_login_total", "status", "invalid_credentials").increment()`
    - `verifySuccessCounter.increment()` -> `meterRegistry.counter("auth_email_verification_total", "status", "success").increment()`
    - `verifyExpiredCounter.increment()` -> `meterRegistry.counter("auth_email_verification_total", "status", "expired").increment()`
    - `verifyInvalidCounter.increment()` -> `meterRegistry.counter("auth_email_verification_total", "status", "invalid").increment()`
    - `resendSentCounter.increment()` -> `meterRegistry.counter("auth_resend_verification_total", "status", "sent").increment()`
    - `resendRateLimitedCounter.increment()` -> `meterRegistry.counter("auth_resend_verification_total", "status", "rate_limited").increment()`
    - `resendNoActionCounter.increment()` -> `meterRegistry.counter("auth_resend_verification_total", "status", "no_action").increment()`

### Phase 3: Refactor EmailServiceImpl

- [x] **File**: `backend/src/main/java/com/library/service/EmailServiceImpl.java`
  - Remove `private Counter sentCounter` and `private Counter errorCounter` fields (lines 31-32)
  - Remove `@PostConstruct initMetrics()` method (lines 34-44)
  - Remove `import io.micrometer.core.instrument.Counter`
  - Remove `import jakarta.annotation.PostConstruct`
  - Add `import io.micrometer.core.annotation.Timed`
  - Add `@Timed(value = "email.send_verification")` on `sendVerificationEmail()` method
  - Replace `sentCounter.increment()` -> `meterRegistry.counter("auth_verification_email_sent_total", "status", "success").increment()`
  - Replace `errorCounter.increment()` -> `meterRegistry.counter("auth_verification_email_sent_total", "status", "error").increment()`

### Phase 4: Update unit tests

- [x] **File**: `backend/src/test/java/com/library/unit/service/AuthServiceTest.java`
  - Remove the `try/catch` reflection block in `setUp()` that invokes `initMetrics()` (lines 77-85)
  - Keep the `new SimpleMeterRegistry()` passed to the constructor
  - `@Timed` is AOP-driven and transparent to unit tests (no AOP context in `@ExtendWith(MockitoExtension.class)`)

- [x] **File**: `backend/src/test/java/com/library/unit/service/EmailServiceTest.java`
  - Remove the `try/catch` reflection block in `setUp()` that invokes `initMetrics()` (lines 44-51)
  - Keep the `new SimpleMeterRegistry()` passed to the constructor

### Phase 5: Add ArchUnit enforcement

- [x] **File**: `backend/src/test/java/com/library/architecture/LayerDependencyTest.java`
  - Add imports:
    ```java
    import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
    import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
    import io.micrometer.core.instrument.Counter;
    import io.micrometer.core.instrument.Timer;
    import io.micrometer.core.instrument.Gauge;
    import jakarta.annotation.PostConstruct;
    ```
  - Add rule `services_should_not_have_metric_fields`:
    ```java
    @ArchTest
    static final ArchRule services_should_not_have_metric_fields =
        noFields().that().haveRawType(Counter.class)
            .or().haveRawType(Timer.class)
            .or().haveRawType(Gauge.class)
            .should().beDeclaredInClassesThat().resideInAPackage("..service..")
            .because(
                "Services must use inline meterRegistry.counter() calls, not pre-registered "
                + "Counter/Timer/Gauge fields. "
                + "REMEDIATION: Replace field + @PostConstruct with "
                + "meterRegistry.counter(name, tags).increment(). "
                + "See docs/PATTERNS.md section '8. Metrics Pattern'."
            );
    ```
  - Add rule `services_should_not_use_postconstruct`:
    ```java
    @ArchTest
    static final ArchRule services_should_not_use_postconstruct =
        noMethods().that().areAnnotatedWith(PostConstruct.class)
            .should().beDeclaredInClassesThat().resideInAPackage("..service..")
            .because(
                "Services must not use @PostConstruct for metric initialization. "
                + "REMEDIATION: Remove @PostConstruct initMetrics() and use inline "
                + "meterRegistry.counter() calls instead. "
                + "See docs/PATTERNS.md section '8. Metrics Pattern'."
            );
    ```

### Phase 6: Run tests

- [x] `cd backend && mvn clean test -Dgroups='!integration'` — unit + architecture tests must pass
- [x] `cd backend && mvn checkstyle:check` — no checkstyle violations

### Phase 7: Update documentation

- [x] **File**: `docs/PATTERNS.md`
  - Add section "8. Metrics Pattern" after section 7 with:
    - When to use `@Timed` (method-level latency timing via AOP)
    - When to use inline `meterRegistry.counter()` (event counting at business logic decision points)
    - How to add a new metric: add name to allowlist in `MetricsConfig` first, then use in service code
    - Anti-patterns table (enforced by ArchUnit rules)

- [x] **File**: `docs/COMMON_PITFALLS.md`
  - Add **P016**: Pre-registered metric fields in services
    - Symptom: ArchUnit fails with `services_should_not_have_metric_fields`
    - Fix: inline `meterRegistry.counter()`
  - Add **P017**: Metric not appearing in Prometheus
    - Symptom: counter incremented but not visible in `/actuator/prometheus`
    - Cause: metric name not in `MetricsConfig` allowlist
    - Fix: add name to the `MeterFilter` allowlist in `MetricsConfig`

### Phase 8: Integration verification

- [x] `./scripts/build.sh` — build Docker images
- [x] `docker compose down && docker compose up -d` — start all services fresh
- [x] Run integration tests: `cd backend && mvn verify -Dgroups=integration`
- [x] Prometheus spot checks (see Test Plan below)

---

## Test Plan (Manual Verification)

### API Verification

```bash
# 1. Start services fresh
docker compose down && docker compose up -d

# 2. Wait for backend to be healthy
curl -s http://localhost:8080/actuator/health | python3 -m json.tool
# Expect: {"status":"UP",...}

# 3. Register a user to generate auth_registration_total counter
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"metrics-test@example.com","password":"password123","confirmPassword":"password123"}'
# Expect: 201 with emailVerified=false

# 4. Attempt login before verification to generate auth_login_total{status=unverified}
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"metrics-test@example.com","password":"password123"}'
# Expect: 403

# 5. Verify auth counters are present in Prometheus output
curl -s http://localhost:8080/actuator/prometheus | grep auth_registration_total
# Expect: auth_registration_total{status="success",...} 1.0

curl -s http://localhost:8080/actuator/prometheus | grep auth_login_total
# Expect: auth_login_total{status="unverified",...} 1.0

# 6. Verify @Timed timers are present
curl -s http://localhost:8080/actuator/prometheus | grep 'auth_register_seconds'
# Expect: auth_register_seconds_count, auth_register_seconds_sum, auth_register_seconds_max

# 7. Verify allowlisted JVM metrics are present
curl -s http://localhost:8080/actuator/prometheus | grep 'jvm_memory_used'
# Expect: jvm_memory_used_bytes{...} <value>

# 8. Verify denied metrics are NOT present
curl -s http://localhost:8080/actuator/prometheus | grep -c 'logback'
# Expect: 0

curl -s http://localhost:8080/actuator/prometheus | grep -c 'executor'
# Expect: 0

# 9. Verify health endpoint unchanged
curl -s http://localhost:8080/actuator/health | python3 -m json.tool
# Expect: Status UP, no degradation
```

## Technical Decisions

- **Inline `meterRegistry.counter()`**: Micrometer caches meters by name + tags internally, so calling `meterRegistry.counter("auth_registration_total", "status", "success")` on every invocation is safe and does not create duplicate registrations
- **`@Timed` via AOP**: Requires `TimedAspect` bean; transparent to unit tests since they run without Spring AOP context — `SimpleMeterRegistry` absorbs the inline counters without error
- **`@Timed` + `@Transactional` ordering**: Spring Boot's default proxy ordering handles this correctly — timing includes transaction commit time, which is the desired behavior
- **MeterFilter allowlist**: Prevents metrics sprawl by denying unrecognized metric names at registration time; new metrics must be added to `MetricsConfig` before use
- **ArchUnit enforcement**: Two new rules (`services_should_not_have_metric_fields`, `services_should_not_use_postconstruct`) prevent regression to the pre-registered Counter field pattern
