# Common Pitfalls

> Read this before writing any code. Each entry was added after an agent failure.
> When you fix a new agent failure, add it here.

## Architectural Pitfalls

### P001: Controller importing Repository directly

**Symptom**: ArchUnit test fails with "controller imports repository"
**Wrong**:
```java
@RestController
public class BookController {
    @Autowired
    private BookRepository bookRepository; // WRONG: bypass service layer
}
```
**Fix**: Inject the service interface only:
```java
@RestController
public class BookController {
    private final BookService bookService; // CORRECT
}
```

### P002: Raw DTO returned from controller

**Symptom**: Controller returns `BookDto` or `Page<BookDto>` directly
**Wrong**:
```java
@GetMapping("/search")
public Page<BookDto> searchBooks(...) { ... }
```
**Fix**: Always wrap in `ApiResponse<T>`:
```java
@GetMapping("/search")
public ResponseEntity<ApiResponse<PagedResponse<BookDto>>> searchBooks(...) {
    return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(result)));
}
```

### P003: Missing `@Transactional(readOnly = true)` on service

**Symptom**: N+1 query problem or unnecessary write locks
**Fix**: Add `@Transactional(readOnly = true)` at the class level in `BookServiceImpl`, override individual write methods with `@Transactional`.

---

## Database Pitfalls

### P004: Editing an existing Flyway migration

**Symptom**: `FlywayException: Migration checksum mismatch`
**Wrong**: Modifying `V1__create_library_schema.sql` after it has been applied
**Fix**: Create a new migration file: `V2__your_change.sql`
Flyway checks checksums of applied migrations. Editing them breaks all existing databases.

### P005: Case-sensitive LIKE query

**Symptom**: Searching "Spring" doesn't match "spring in action"
**Wrong**:
```sql
WHERE title LIKE CONCAT('%', :query, '%')
```
**Fix**: Use `ILIKE` (PostgreSQL) or `LOWER()`:
```sql
WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%'))
```

### P006: UUID generation in entity vs database

**Symptom**: `GenerationType.AUTO` uses sequence, not UUID
**Fix**: Use `@GeneratedValue(strategy = GenerationType.UUID)` with `@Id UUID id`

---

## Spring Boot Pitfalls

### P007: Missing `@Valid` on controller parameter

**Symptom**: Validation annotations on DTO fields are ignored
**Wrong**:
```java
public ResponseEntity<...> searchBooks(BookSearchRequest request) { ... }
```
**Fix**: Add `@Valid`:
```java
public ResponseEntity<...> searchBooks(@Valid BookSearchRequest request) { ... }
```

### P008: Lombok and JPA entity `equals`/`hashCode`

**Symptom**: Entity comparison fails in collections; Hibernate proxy issues
**Wrong**: Using `@Data` on JPA entities (generates `equals`/`hashCode` based on all fields including lazy-loaded ones)
**Fix**: Use `@Getter @Setter @ToString @Builder @NoArgsConstructor @AllArgsConstructor` on entities. Implement `equals`/`hashCode` based on `id` only, or use `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`.

### P009: `@SpringBootTest` without Testcontainers in integration tests

**Symptom**: Integration test tries to connect to `localhost:5432` and fails
**Fix**: Always add `@Testcontainers` and `@DynamicPropertySource` in integration test classes. See `docs/TESTING.md`.

---

## Docker / Environment Pitfalls

### P010: Backend URL in frontend using `localhost`

**Symptom**: Frontend container cannot reach backend; `ConnectionError: localhost:8080`
**Wrong**: `BACKEND_URL=http://localhost:8080` in docker-compose.yml
**Fix**: Use Docker service DNS: `BACKEND_URL=http://backend:8080`

### P011: Log directory not existing before container starts

**Symptom**: Container crashes on start because `/var/log/app` doesn't exist
**Fix**: `./scripts/start.sh` creates `./logs/backend` and `./logs/frontend` before `docker compose up`.

### P012: Environment variables not in `.env`

**Symptom**: docker-compose uses wrong values or fails substitution
**Fix**: Copy `.env.example` to `.env` before running docker-compose. The `start.sh` script does this automatically.

---

## Checkstyle Pitfalls

### P013: Wildcard imports

**Symptom**: `Checkstyle: import.avoidStar`
**Wrong**: `import java.util.*;`
**Fix**: Import each class explicitly: `import java.util.List;`

### P014: Tabs instead of spaces

**Symptom**: `Checkstyle: containsTab`
**Fix**: Configure your editor to use 4 spaces. Replace all tabs: `sed -i 's/\t/    /g' File.java`

### P015: Line too long

**Symptom**: `Checkstyle: maxLineLength` (max is 120)
**Fix**: Break long method chains or long strings across multiple lines.
