# Product Spec 004: Prometheus & Grafana Observability

**Feature ID**: F004
**Status**: completed

## User Story

As a developer or coding agent, I want a local Prometheus and Grafana stack so that I can query metrics with PromQL, visualize dashboards, and validate SLOs (e.g., "service startup completes in under 800ms") without external tooling.

## Summary

Add Prometheus and Grafana to the local Docker Compose stack. The backend exposes metrics in Prometheus format via Micrometer; Prometheus scrapes them; Grafana visualizes and enables PromQL exploration. This aligns with the [harness engineering](https://openai.com/index/harness-engineering/) model: agents can query logs (LogQL) and metrics (PromQL) to reason about app behavior and performance.

## System Design

```
┌─────────────────────────────────────────────────────────────────────────┐
│ Local Observability Stack (ephemeral per worktree)                       │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   ┌──────────────┐     scrape /actuator/prometheus     ┌─────────────┐  │
│   │   Backend    │ ─────────────────────────────────►  │  Prometheus │  │
│   │   :8080      │           every 15s                  │   :9090     │  │
│   └──────────────┘                                     └──────┬──────┘  │
│         │                                                      │        │
│         │                                                      │ PromQL │
│         │                                                      ▼        │
│         │                                               ┌─────────────┐ │
│         │                                               │   Grafana   │ │
│         │                                               │   :3000     │ │
│         │                                               └─────────────┘ │
│         │                                                               │
│         └──► Logs: ./logs/backend/app.log (JSON)                        │
│              HTTP: GET /actuator/logfile                                │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

## API Contract (Backend Changes)

No new REST endpoints. Extend Actuator:

- **New endpoint**: `GET /actuator/prometheus` — Prometheus text format (requires `micrometer-registry-prometheus`)

Existing endpoints unchanged: `GET /actuator/health`, `GET /actuator/metrics`, `GET /actuator/logfile`.

See [API_REFERENCE.md](../API_REFERENCE.md) for actuator endpoints.

## Acceptance Criteria

See `features.json` entry F004.

## Agent Legibility

Document the following so agents can use the harness:

1. **Grafana URL**: http://localhost:3000 (default `admin`/`admin`, change on first login)
2. **Prometheus UI**: http://localhost:9090 — Targets, PromQL query
3. **Example PromQL**: `jvm_memory_used_bytes`, `rate(http_server_requests_seconds_count[5m])`, `process_start_time_seconds`
4. **Example prompts**: "ensure service startup completes in under 800ms", "verify HTTP latency p95 under 500ms"

## Security & Scope

- **Local only**: Prometheus and Grafana run in Docker, no public exposure. Grafana default credentials documented for local dev.
- **Production**: Out of scope — production monitoring is a separate concern.

## Out of Scope (this feature)

- Loki or other log aggregation (LogQL) — logs remain file/HTTP based
- Distributed tracing (OpenTelemetry, Jaeger)
- Pre-built Grafana dashboards (optional follow-up)
- Production deployment of Prometheus/Grafana
