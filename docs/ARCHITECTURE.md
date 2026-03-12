# Architecture

## Layer Model

The backend enforces a **strict unidirectional dependency order**:

```
┌──────────────────────────────────────────────────────┐
│  controller  (REST endpoints, exception handlers)     │
│     ↓ imports                                         │
│  service     (business logic interfaces + impls)      │
│     ↓ imports                                         │
│  repository  (JPA entities + Spring Data repos)       │
│     ↓ imports                                         │
│  config      (app configuration beans)                │
│     ↓ imports                                         │
│  types       (DTOs, enums, value objects)             │
│     (no dependencies on other layers)                 │
└──────────────────────────────────────────────────────┘
```

### Layer Definitions

| Layer | Package | Allowed to import |
|-------|---------|-------------------|
| `types` | `com.library.types` | Nothing (no other layers) |
| `config` | `com.library.config` | `types` |
| `repository` | `com.library.repository` | `types`, `config` |
| `service` | `com.library.service` | `types`, `config`, `repository` |
| `controller` | `com.library.controller` | `types`, `service` |

### Forbidden Dependencies (ArchUnit enforces these)

| Violation | Why Forbidden |
|-----------|--------------|
| `controller` → `repository` | Bypasses service layer; business logic leaks into HTTP layer |
| `service` → `controller` | Circular dependency; service must not know about HTTP |
| `types` → any other layer | Types are the foundation; circular deps break compilation |
| `repository` → `service` | Circular; repositories are passive data-access objects |

## Enforcement

**ArchUnit** (`src/test/java/com/library/architecture/LayerDependencyTest.java`) runs as part of `mvn test` and fails the build on any violation.

Error message format when violated:
```
Architecture violation: Class com.library.controller.BookController imports
com.library.repository.BookRepository directly.
REMEDIATION: Controllers must only import service interfaces.
Inject BookService instead of BookRepository.
See docs/ARCHITECTURE.md and docs/PATTERNS.md
```

**Checkstyle** (`linters/checkstyle.xml`) enforces code style at the `validate` phase.

## Request Flow

```
HTTP Request
    │
    ▼
BookController          ← validates input, delegates to service
    │
    ▼
BookService             ← business logic, transactions
    │
    ▼
BookRepository          ← Spring Data JPA query
    │
    ▼
PostgreSQL (library_db)
    │
    ▼
BookEntity              ← JPA mapping
    │
    ▼ mapped by BookServiceImpl
BookDto                 ← returned to controller
    │
    ▼
ApiResponse<BookDto>    ← HTTP response
```

## Database Schema

See [generated/db-schema.md](generated/db-schema.md) for the current schema.

Migrations live in `backend/src/main/resources/db/migration/`.
**Never modify existing migrations** — always add a new `V{n}__description.sql`.

## Docker Network

```
Host machine
  ├── :8080 → library-backend (container: library-backend)
  ├── :8501 → library-frontend (container: library-frontend)
  └── :5433 → library-db (container: library-db)

Docker network: library-network
  ├── backend:8080  (DNS: backend)
  ├── frontend:8501 (DNS: frontend)
  └── db:5432       (DNS: db)
```

Frontend reaches backend via `http://backend:8080` (Docker DNS).
Backend reaches database via `jdbc:postgresql://db:5432/library_db`.
