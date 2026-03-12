# Execution Plan 001: Anonymous Library Search (F001)

**Feature**: F001 — Anonymous Book Search
**Status**: completed
**Started**: 2026-03-12
**Completed**: 2026-03-12

## Goal

Users can search the library catalog by title, author, ISBN, or genre without authentication.

## Acceptance Criteria

From `features.json`:
1. `GET /api/v1/books/search` returns paginated list of books
2. Keyword search (q param) matches title, author, or ISBN (case-insensitive)
3. Genre filter returns only books of that genre
4. Empty search returns all books sorted by title
5. Response wrapped in `ApiResponse<PagedResponse<BookDto>>`
6. Pagination works (page, size, totalPages, totalElements)
7. Streamlit UI: search bar + genre dropdown + paginated book cards

## Implementation Steps

- [x] Create `types/enums/Genre.java`
- [x] Create `types/dto/ApiResponse.java`
- [x] Create `types/dto/BookDto.java`
- [x] Create `types/dto/BookSearchRequest.java`
- [x] Create `types/dto/PagedResponse.java`
- [x] Create `config/WebConfig.java` (CORS)
- [x] Create `config/OpenApiConfig.java`
- [x] Create `repository/entity/BookEntity.java`
- [x] Create `repository/BookRepository.java`
- [x] Create `service/BookService.java` (interface)
- [x] Create `service/BookServiceImpl.java`
- [x] Create `controller/BookController.java`
- [x] Create `controller/advice/GlobalExceptionHandler.java`
- [x] Create `V1__create_library_schema.sql` (with 10 seed books)
- [x] Create `BookServiceTest.java` (3 unit tests)
- [x] Create `BookSearchIntegrationTest.java` (3 integration tests)
- [x] Create `LayerDependencyTest.java` (architecture enforcement)
- [x] Create `frontend/app.py` (Streamlit UI)

## Test Plan

```bash
# Run all tests
./scripts/run-tests.sh

# Verify API
curl http://localhost:8080/actuator/health
curl "http://localhost:8080/api/v1/books/search?q=spring"
curl "http://localhost:8080/api/v1/books/search?genre=TECHNOLOGY&page=0&size=5"

# Verify UI
open http://localhost:8501
```

## Decision Log

- Used `JPQL LOWER(LIKE)` instead of `pg_trgm` for initial implementation — simpler, sufficient for small catalog
- `pg_trgm` fuzzy search added to tech debt tracker for later
- Fixed Checkstyle `HideUtilityClassConstructor` on `LibraryApplication` by excluding it in checkstyle.xml (Spring Boot requires visible constructor)
- Switched backend Docker base from `eclipse-temurin:17-*-alpine` to Debian variants for Apple Silicon (arm64) compatibility
