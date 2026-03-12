# Coding Style Guide

> Checkstyle (`linters/checkstyle.xml`) mechanically enforces these rules.
> Violations fail the build at the `validate` phase.

## Java Style

### General

- **Indentation**: 4 spaces (no tabs)
- **Line length**: max 120 characters
- **File encoding**: UTF-8
- **Braces**: always required, even for single-line blocks
- **Newline at end of file**: required

### Naming

| Element | Convention | Example |
|---------|-----------|---------|
| Class | `UpperCamelCase` | `BookServiceImpl` |
| Interface | `UpperCamelCase` | `BookService` |
| Method | `lowerCamelCase` | `searchBooks` |
| Variable | `lowerCamelCase` | `totalPages` |
| Constant | `UPPER_SNAKE_CASE` | `MAX_PAGE_SIZE` |
| Package | `lowercase.dots` | `com.library.service` |
| DTO suffix | `Dto` | `BookDto` |
| Entity suffix | `Entity` | `BookEntity` |
| Controller suffix | `Controller` | `BookController` |
| Service interface | no suffix | `BookService` |
| Service impl suffix | `Impl` | `BookServiceImpl` |

### Imports

- No wildcard imports (`import java.util.*` is forbidden)
- No unused imports
- Order: `java`, `javax`, `jakarta`, `org`, `com` (separated by blank lines)

### Classes

- One top-level class per file
- Utility classes must have private constructors
- Always override `hashCode()` when overriding `equals()`

### Annotations

- Each annotation on its own line (no `@NotNull @Size String name` in one line)

### Comments

- No `//TODO` without an associated `features.json` entry
- Javadoc required on `public` methods (warning, not error)
- Avoid obvious comments (`// increment counter` above `counter++`)

## Java Patterns

### DTOs

```java
// CORRECT: Lombok + constructor
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookDto {
    private UUID id;
    @NotBlank
    private String title;
    @NotBlank
    private String author;
    private Genre genre;
}

// WRONG: Mutable fields without Lombok
public class BookDto {
    public String title; // never public fields in DTOs
}
```

### Service Methods

```java
// CORRECT: interface + @Transactional on impl
public interface BookService {
    Page<BookDto> searchBooks(BookSearchRequest request, Pageable pageable);
}

@Service
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {
    @Override
    public Page<BookDto> searchBooks(BookSearchRequest request, Pageable pageable) { ... }
}

// WRONG: @Service on interface
@Service // never annotate the interface
public interface BookService { ... }
```

### Controller Methods

```java
// CORRECT: return ApiResponse<T>
@GetMapping("/search")
public ResponseEntity<ApiResponse<PagedResponse<BookDto>>> searchBooks(...) {
    Page<BookDto> page = bookService.searchBooks(request, pageable);
    return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(page)));
}

// WRONG: return raw DTO
@GetMapping("/search")
public Page<BookDto> searchBooks(...) { ... } // never return raw DTOs
```

## Python Style (Streamlit Frontend)

### General

- **Indentation**: 4 spaces
- **Line length**: max 100 characters
- **Type hints**: required on function parameters and return types
- **Imports**: stdlib → third-party → local, separated by blank lines

### Naming

| Element | Convention | Example |
|---------|-----------|---------|
| Function | `snake_case` | `search_books` |
| Variable | `snake_case` | `total_pages` |
| Constant | `UPPER_SNAKE_CASE` | `BACKEND_URL` |
| Session state keys | `st_` prefix | `st_search_query` |

### Logging

```python
# CORRECT: use module-level logger with structured format
logger = logging.getLogger(__name__)
logger.info("Searching books query='%s' genre='%s' page=%d", query, genre, page)

# WRONG: print statements
print("Searching...")  # never use print for logging
```

### Error Handling

```python
# CORRECT: catch specific exceptions, call st.error()
try:
    response = requests.get(url, timeout=10)
    response.raise_for_status()
except requests.exceptions.ConnectionError:
    st.error("Cannot connect to backend. Is it running?")
    return None

# WRONG: bare except or silent swallowing
try:
    ...
except:  # bare except is forbidden
    pass
```
