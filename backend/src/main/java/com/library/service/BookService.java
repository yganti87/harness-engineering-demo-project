package com.library.service;

import com.library.types.dto.BookDto;
import com.library.types.dto.BookSearchRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for book catalog operations.
 *
 * <p>Controllers depend on this interface, never on {@code BookServiceImpl} directly.
 * Implementations are annotated with {@code @Service}.
 *
 * <p>See docs/PATTERNS.md section "3. Service Pattern".
 */
public interface BookService {

    /**
     * Searches the book catalog with optional keyword and genre filters.
     *
     * @param request search parameters (keyword and/or genre)
     * @param pageable pagination and sorting
     * @return paginated list of matching books
     */
    Page<BookDto> searchBooks(BookSearchRequest request, Pageable pageable);

    /**
     * Retrieves a single book by its unique identifier.
     *
     * @param id book UUID
     * @return the book DTO
     * @throws jakarta.persistence.EntityNotFoundException if no book with the given id exists
     */
    BookDto getBookById(UUID id);

}
