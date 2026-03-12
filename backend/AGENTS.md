# Backend Agent Guide

> Read root AGENTS.md first, then this file.

## Stack

- Java 17
- Spring Boot 3.2.x
- Spring Data JPA + Hibernate
- PostgreSQL 15 (via Flyway migrations)
- Maven build system

## Package Structure

```
com.library/
├── LibraryApplication.java       # Entry point
├── types/
│   ├── dto/                      # ApiResponse, BookDto, BookSearchRequest, PagedResponse
│   └── enums/                    # Genre
├── config/                       # WebConfig (CORS), OpenApiConfig
├── repository/
│   ├── entity/BookEntity.java    # JPA entity
│   └── BookRepository.java       # Spring Data JPA interface
├── service/
│   ├── BookService.java          # Interface
│   └── BookServiceImpl.java      # @Service @Transactional
└── controller/
    ├── BookController.java
    └── advice/GlobalExceptionHandler.java
```

## Adding a New Feature

Work in this order (layer model is enforced by ArchUnit):

1. **types**: Add DTOs or enums needed
2. **config**: Add any new config beans if needed
3. **repository**: Add entity fields + repository methods
4. **service**: Add interface method + implementation
5. **controller**: Add endpoint
6. **migration**: Create `V{n}__description.sql` for any schema changes
7. **tests**: Unit test for service, integration test for endpoint

## Build & Test

```bash
# Compile
mvn compile

# Checkstyle only
mvn checkstyle:check

# Unit + architecture tests
mvn test -Dgroups='!integration'

# Integration tests
mvn test -Dgroups=integration

# All
./scripts/run-tests.sh
```

## Critical Rules

- **Layer order**: types → config → repository → service → controller
- **ApiResponse envelope**: Every controller method returns `ResponseEntity<ApiResponse<T>>`
- **Never raw SQL**: Use JPQL `@Query` in repository; never concatenate user input
- **Never modify migrations**: Add `V{n+1}__*.sql` instead
- **@Valid on controller params**: Required for DTO validation to trigger
- **Logging**: Use `log.info("message='{}' key='{}'", val1, val2)` — never string concat

## Log Location

- Container: `/var/log/app/app.log` (JSON)
- Host: `./logs/backend/app.log`
- HTTP: `curl http://localhost:8080/actuator/logfile`

## Swagger UI

http://localhost:8080/swagger-ui.html
