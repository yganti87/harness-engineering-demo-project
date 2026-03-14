# Execution Plan 004: Prometheus & Grafana Observability (F004)

**Feature**: F004 — Prometheus & Grafana Observability
**Status**: completed
**Product Spec**: [004-prometheus-grafana-observability.md](../../product-specs/004-prometheus-grafana-observability.md)
**Started**: 2026-03-13
**Completed**: 2026-03-13

## Goal

Add a local Prometheus and Grafana stack to the Docker Compose environment so developers and coding agents can query metrics with PromQL, visualize dashboards, and validate SLOs without external tooling.

## Acceptance Criteria

From `features.json`: All 7 criteria met.

## Implementation Steps

### Phase 1: Backend — Prometheus metrics endpoint

- [x] Add `micrometer-registry-prometheus` dependency to `backend/pom.xml`
- [x] Add `prometheus` to `management.endpoints.web.exposure.include` in `backend/src/main/resources/application.yml`
- [x] Verify `GET /actuator/prometheus` returns Prometheus text format (run backend locally or via Docker)

### Phase 2: Prometheus service

- [x] Create `monitoring/prometheus/prometheus.yml` with scrape config
- [x] Add `prometheus` service to `docker-compose.yml`
- [x] Add `prometheus-data` volume for persistence

### Phase 3: Grafana service

- [x] Create `monitoring/grafana/provisioning/datasources/datasource.yml`
- [x] Add `grafana` service to `docker-compose.yml`
- [x] Add `grafana-data` volume for persistence

### Phase 4: Scripts & documentation

- [x] Update `scripts/start.sh`: add Grafana and Prometheus URLs to success banner
- [x] Update `docs/RELIABILITY.md`: add "Prometheus & Grafana" section
- [x] Update `AGENTS.md`: add Prometheus (9090) and Grafana (3000) to component table

### Phase 5: Verification

- [x] Run `./scripts/start.sh` — all services healthy
- [x] `curl http://localhost:8080/actuator/prometheus` — returns Prometheus format
- [x] Prometheus Targets show `library-backend` UP
- [x] Grafana login (admin/admin), Explore → PromQL `jvm_memory_used_bytes`

## Technical Decisions

- **Image versions**: `prom/prometheus:v2.52.0`, `grafana/grafana:11.2.0` — pinned for reproducibility
- **Micrometer Prometheus**: Spring Boot Actuator + Micrometer provide JVM, HTTP, and HikariCP metrics out of the box
- **Scrape target**: Use Docker service name `backend:8080` — Prometheus runs in same network as backend
