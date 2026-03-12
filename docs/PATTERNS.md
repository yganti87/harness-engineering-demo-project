# Patterns

> Follow these patterns exactly. Do not invent new patterns without updating this file.

## 1. API Response Envelope

**Every** controller method wraps its response in `ApiResponse<T>`.

```java
// types/dto/ApiResponse.java
@Data @Builder
public class ApiResponse<T> {
    private int status;
    private T data;
    private String error;
    private Instant timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .status(HttpStatus.OK.value())
            .data(data)
            .timestamp(Instant.now())
            .build();
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return ApiResponse.<T>builder()
            .status(status)
            .error(message)
            .timestamp(Instant.now())
            .build();
    }
}
```

Success response:
```json
{ "status": 200, "data": { ... }, "error": null, "timestamp": "2026-03-12T10:00:00Z" }
```

Error response:
```json
{ "status": 404, "data": null, "error": "Book not found: id=abc123", "timestamp": "..." }
```

## 2. Repository Pattern

Repositories extend `JpaRepository` and use Spring Data query methods. Complex queries use `@Query` with JPQL (not native SQL unless required).

```java
// repository/BookRepository.java
public interface BookRepository extends JpaRepository<BookEntity, UUID> {

    // Spring Data method derivation — no @Query needed for simple filters
    Page<BookEntity> findByGenre(Genre genre, Pageable pageable);

    // JPQL for multi-field search
    @Query("""
        SELECT b FROM BookEntity b
        WHERE (:query IS NULL OR :query = ''
               OR LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :query, '%')))
          AND (:genre IS NULL OR b.genre = :genre)
        ORDER BY b.title ASC
        """)
    Page<BookEntity> searchBooks(
        @Param("query") String query,
        @Param("genre") Genre genre,
        Pageable pageable
    );
}
```

## 3. Service Pattern

Services are interfaces implemented by a single `Impl` class. The impl is annotated with `@Service` and `@Transactional`. Read-only methods use `@Transactional(readOnly = true)`.

```java
// service/BookService.java
public interface BookService {
    Page<BookDto> searchBooks(BookSearchRequest request, Pageable pageable);
    BookDto getBookById(UUID id);
}

// service/BookServiceImpl.java
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {
    private final BookRepository bookRepository;

    @Override
    public Page<BookDto> searchBooks(BookSearchRequest request, Pageable pageable) {
        log.info("Searching books query='{}' genre='{}'", request.getQ(), request.getGenre());
        Page<BookEntity> entities = bookRepository.searchBooks(
            request.getQ(),
            request.getGenre(),
            pageable
        );
        return entities.map(this::toDto);
    }

    @Override
    public BookDto getBookById(UUID id) {
        return bookRepository.findById(id)
            .map(this::toDto)
            .orElseThrow(() -> new EntityNotFoundException("Book not found: id=" + id));
    }

    private BookDto toDto(BookEntity entity) {
        return BookDto.builder()
            .id(entity.getId())
            .title(entity.getTitle())
            .author(entity.getAuthor())
            .isbn(entity.getIsbn())
            .genre(entity.getGenre())
            .publicationYear(entity.getPublicationYear())
            .description(entity.getDescription())
            .build();
    }
}
```

## 4. Controller Pattern

Controllers delegate entirely to services. Validation uses `@Valid`. Responses use `ApiResponse<T>`.

```java
@RestController
@RequestMapping("/api/v1/books")
@Slf4j
@RequiredArgsConstructor
@Validated
public class BookController {
    private final BookService bookService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PagedResponse<BookDto>>> searchBooks(
        @Valid BookSearchRequest request,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BookDto> result = bookService.searchBooks(request, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(result)));
    }
}
```

## 5. Exception Handling Pattern

All exceptions are caught by `GlobalExceptionHandler`. Never catch and swallow exceptions in service or controller layers.

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(404, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(400, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(500, "Internal server error"));
    }
}
```

## 6. Pagination Pattern

Search endpoints return `PagedResponse<T>` wrapping Spring's `Page<T>`.

```java
// types/dto/PagedResponse.java
@Data @Builder
public class PagedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public static <T> PagedResponse<T> of(Page<T> page) {
        return PagedResponse.<T>builder()
            .content(page.getContent())
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .last(page.isLast())
            .build();
    }
}
```

## 7. Entity Pattern

JPA entities use UUID primary keys and `@CreationTimestamp`.

```java
@Entity
@Table(name = "books")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 300)
    private String author;

    @Column(length = 20)
    private String isbn;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Genre genre;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(length = 2000)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
```
