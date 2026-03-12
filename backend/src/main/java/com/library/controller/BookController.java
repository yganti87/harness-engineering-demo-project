package com.library.controller;

import com.library.service.BookService;
import com.library.types.dto.ApiResponse;
import com.library.types.dto.BookDto;
import com.library.types.dto.BookSearchRequest;
import com.library.types.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for book catalog endpoints.
 *
 * <p>All responses are wrapped in {@link ApiResponse}.
 * Controllers only call {@link BookService} — never repositories directly.
 * See docs/ARCHITECTURE.md for layer rules and docs/PATTERNS.md for response patterns.
 */
@RestController
@RequestMapping("/api/v1/books")
@Slf4j
@RequiredArgsConstructor
@Validated
@Tag(name = "Books", description = "Library book catalog")
public class BookController {

    private final BookService bookService;

    /**
     * Searches books by optional keyword and/or genre with pagination.
     *
     * @param q search keyword (matches title, author, ISBN)
     * @param genre genre filter
     * @param page page number (0-indexed)
     * @param size page size (1–100)
     * @return paginated search results
     */
    @GetMapping("/search")
    @Operation(summary = "Search books", description = "Search the catalog by keyword and/or genre")
    public ResponseEntity<ApiResponse<PagedResponse<BookDto>>> searchBooks(
        @Valid BookSearchRequest request,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BookDto> result = bookService.searchBooks(request, pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(result)));
    }

    /**
     * Retrieves full details of a single book by its ID.
     *
     * @param id book UUID
     * @return book details or 404 if not found
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get book by ID", description = "Retrieve full book details")
    public ResponseEntity<ApiResponse<BookDto>> getBookById(@PathVariable UUID id) {
        BookDto book = bookService.getBookById(id);
        return ResponseEntity.ok(ApiResponse.success(book));
    }

}
