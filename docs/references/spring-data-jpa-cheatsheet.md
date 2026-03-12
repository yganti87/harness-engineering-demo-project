# Spring Data JPA Cheat Sheet

Quick reference for common patterns used in this project.

## Repository Methods

```java
// By field value (Spring derives the SQL)
Optional<BookEntity> findById(UUID id);
Page<BookEntity> findAll(Pageable pageable);
Page<BookEntity> findByGenre(Genre genre, Pageable pageable);
List<BookEntity> findByAuthorContainingIgnoreCase(String author);

// Custom JPQL
@Query("SELECT b FROM BookEntity b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :q, '%'))")
Page<BookEntity> findByTitleContainingIgnoreCase(@Param("q") String q, Pageable pageable);
```

## Pageable

```java
// In controller
Pageable pageable = PageRequest.of(page, size); // 0-indexed page
Pageable sorted = PageRequest.of(page, size, Sort.by("title").ascending());

// In service
Page<BookEntity> result = bookRepository.findAll(pageable);
result.getContent();       // List<BookEntity>
result.getTotalElements(); // long
result.getTotalPages();    // int
result.isLast();           // boolean
```

## Entity Annotations

```java
@Entity
@Table(name = "books",
    indexes = {
        @Index(name = "idx_books_genre", columnList = "genre"),
        @Index(name = "idx_books_title_author", columnList = "title, author")
    })
public class BookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Genre genre;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
```

## Transactional

```java
@Service
@Transactional(readOnly = true)  // Class-level default for reads
public class BookServiceImpl {

    // Inherits readOnly = true
    public Page<BookDto> searchBooks(...) { ... }

    // Override for writes
    @Transactional
    public BookDto createBook(BookDto dto) { ... }
}
```

## Common Mistakes

- Using `@Data` on JPA entities — causes issues with lazy loading and `equals`/`hashCode`. Use individual Lombok annotations instead.
- Calling `findAll()` without `Pageable` — fetches entire table.
- `@Query` with native SQL — use JPQL unless absolutely necessary.
