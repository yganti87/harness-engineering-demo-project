# Testing Guide

## Overview

| Test Type | Location | Runs on | Docker needed |
|-----------|----------|---------|---------------|
| Unit | `src/test/.../unit/` | `mvn test -Dgroups='!integration'` | No |
| Architecture | `src/test/.../architecture/` | `mvn test -Dgroups='!integration'` | No |
| Integration | `src/test/.../integration/` | `mvn test -Dgroups=integration` | Yes |
| Checkstyle | N/A | `mvn checkstyle:check` | No |

## Unit Tests

**Convention**: No Spring context. Use Mockito to mock dependencies.

```java
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookServiceImpl bookService;

    @Test
    void searchBooks_withKeyword_returnsMatchingBooks() {
        // Arrange
        var request = BookSearchRequest.builder().q("spring").build();
        var pageable = PageRequest.of(0, 20);
        var entity = BookEntity.builder()
            .id(UUID.randomUUID())
            .title("Spring in Action")
            .author("Craig Walls")
            .genre(Genre.TECHNOLOGY)
            .build();
        when(bookRepository.searchBooks("spring", null, pageable))
            .thenReturn(new PageImpl<>(List.of(entity)));

        // Act
        Page<BookDto> result = bookService.searchBooks(request, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Spring in Action");
    }
}
```

### Naming Convention

- Class: `{ClassUnderTest}Test` (e.g., `BookServiceTest`)
- Method: `{method}_{condition}_{expectedResult}` (e.g., `searchBooks_withKeyword_returnsMatchingBooks`)

## Architecture Tests

**Convention**: Use ArchUnit to enforce layer constraints. Runs without Spring context.

```java
@AnalyzeClasses(packages = "com.library")
class LayerDependencyTest {

    @ArchTest
    static final ArchRule controllers_should_not_depend_on_repositories =
        noClasses().that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..")
            .because("Controllers must only call services. " +
                     "Inject the service interface, not the repository. " +
                     "See docs/ARCHITECTURE.md for the layer model.");
}
```

Run architecture tests to catch layer violations immediately after adding new classes.

## Integration Tests

**Convention**: Use `@SpringBootTest` with Testcontainers for a real PostgreSQL instance.
Tag all integration test classes with `@Tag("integration")`.

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@Tag("integration")
class BookSearchIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void search_withKeyword_returns200WithResults() {
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
            "/api/v1/books/search?q=spring", ApiResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isNotNull();
    }
}
```

**Important**: Integration tests depend on seed data in `V1__create_library_schema.sql`.
If you add books to the migration, update integration test assertions accordingly.

## Checkstyle

Checkstyle runs at the `validate` Maven phase (before compilation).

```bash
# Run checkstyle only
cd backend && mvn checkstyle:check

# Run validate phase (includes checkstyle)
cd backend && mvn validate
```

When Checkstyle fails, the error message contains:
1. File name and line number
2. Rule that was violated
3. Reference to `docs/CODING_STYLE.md`

## Running Tests

```bash
# All tests (full suite)
./scripts/run-tests.sh

# Unit + architecture tests only (fast, no Docker)
cd backend && mvn test -Dgroups='!integration' -q

# Integration tests only
cd backend && mvn test -Dgroups=integration -q

# Specific test class
cd backend && mvn test -Dtest=BookServiceTest -q

# Specific test method
cd backend && mvn test -Dtest='BookServiceTest#searchBooks_withKeyword_returnsMatchingBooks' -q

# Frontend tests
cd frontend && python -m pytest tests/ -v

# With coverage report
cd backend && mvn test jacoco:report
# Report at: backend/target/site/jacoco/index.html
```

## Test Coverage Expectations

| Module | Minimum Coverage |
|--------|----------------|
| `service` | 90% line coverage |
| `controller` | 80% line coverage |
| `repository` | Covered by integration tests |
| `types` | No tests needed (data classes) |
| `config` | No tests needed |
