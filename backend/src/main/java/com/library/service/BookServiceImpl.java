package com.library.service;

import com.library.repository.BookRepository;
import com.library.repository.entity.BookEntity;
import com.library.types.dto.BookDto;
import com.library.types.dto.BookSearchRequest;
import com.library.types.enums.Genre;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link BookService}.
 *
 * <p>All public methods are read-only transactions by default (class-level annotation).
 * Write methods must override with {@code @Transactional}.
 *
 * <p>See docs/PATTERNS.md section "3. Service Pattern".
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    @Override
    public Page<BookDto> searchBooks(BookSearchRequest request, Pageable pageable) {
        String query = request.getQ();
        Genre genre = request.getGenre();

        log.info("Searching books query='{}' genre='{}'", query, genre);

        Page<BookEntity> entities = bookRepository.searchBooks(query, genre, pageable);

        log.info("Search completed results={} query='{}' genre='{}'",
            entities.getTotalElements(), query, genre);

        return entities.map(this::toDto);
    }

    @Override
    public BookDto getBookById(UUID id) {
        log.info("Getting book id='{}'", id);

        return bookRepository.findById(id)
            .map(this::toDto)
            .orElseThrow(() -> {
                log.warn("Book not found id='{}'", id);
                return new EntityNotFoundException("Book not found: id=" + id);
            });
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
