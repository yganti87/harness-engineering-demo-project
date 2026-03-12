package com.library.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.library.repository.BookRepository;
import com.library.repository.entity.BookEntity;
import com.library.service.BookServiceImpl;
import com.library.types.dto.BookDto;
import com.library.types.dto.BookSearchRequest;
import com.library.types.enums.Genre;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Unit tests for {@link BookServiceImpl}.
 *
 * <p>No Spring context — uses Mockito to isolate the service.
 * Follow naming convention: methodName_condition_expectedResult
 */
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookServiceImpl bookService;

    @Test
    void searchBooks_withKeyword_returnsMatchingBooks() {
        // Arrange
        BookSearchRequest request = BookSearchRequest.builder().q("spring").build();
        Pageable pageable = PageRequest.of(0, 20);
        BookEntity entity = buildBookEntity("Spring in Action", "Craig Walls", Genre.TECHNOLOGY);
        when(bookRepository.searchBooks(eq("spring"), eq(null), eq(pageable)))
            .thenReturn(new PageImpl<>(List.of(entity)));

        // Act
        Page<BookDto> result = bookService.searchBooks(request, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Spring in Action");
        assertThat(result.getContent().get(0).getAuthor()).isEqualTo("Craig Walls");
        assertThat(result.getContent().get(0).getGenre()).isEqualTo(Genre.TECHNOLOGY);
    }

    @Test
    void searchBooks_withGenreFilter_returnsFilteredBooks() {
        // Arrange
        BookSearchRequest request = BookSearchRequest.builder().genre(Genre.TECHNOLOGY).build();
        Pageable pageable = PageRequest.of(0, 20);
        BookEntity tech1 = buildBookEntity("Spring in Action", "Craig Walls", Genre.TECHNOLOGY);
        BookEntity tech2 = buildBookEntity("Clean Code", "Robert Martin", Genre.TECHNOLOGY);
        when(bookRepository.searchBooks(eq(null), eq(Genre.TECHNOLOGY), eq(pageable)))
            .thenReturn(new PageImpl<>(List.of(tech1, tech2)));

        // Act
        Page<BookDto> result = bookService.searchBooks(request, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
            .extracting(BookDto::getGenre)
            .containsOnly(Genre.TECHNOLOGY);
    }

    @Test
    void searchBooks_emptyRequest_returnsAllBooks() {
        // Arrange
        BookSearchRequest request = BookSearchRequest.builder().build();
        Pageable pageable = PageRequest.of(0, 20);
        BookEntity book1 = buildBookEntity("Book A", "Author A", Genre.FICTION);
        BookEntity book2 = buildBookEntity("Book B", "Author B", Genre.SCIENCE);
        when(bookRepository.searchBooks(eq(null), eq(null), eq(pageable)))
            .thenReturn(new PageImpl<>(List.of(book1, book2), pageable, 2));

        // Act
        Page<BookDto> result = bookService.searchBooks(request, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void getBookById_existingId_returnsBook() {
        // Arrange
        UUID id = UUID.randomUUID();
        BookEntity entity = buildBookEntity("Clean Code", "Robert Martin", Genre.TECHNOLOGY);
        entity = BookEntity.builder()
            .id(id)
            .title("Clean Code")
            .author("Robert Martin")
            .genre(Genre.TECHNOLOGY)
            .build();
        when(bookRepository.findById(id)).thenReturn(Optional.of(entity));

        // Act
        BookDto result = bookService.getBookById(id);

        // Assert
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getTitle()).isEqualTo("Clean Code");
    }

    @Test
    void getBookById_nonExistentId_throwsEntityNotFoundException() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(bookRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> bookService.getBookById(id))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Book not found: id=" + id);
    }

    private BookEntity buildBookEntity(String title, String author, Genre genre) {
        return BookEntity.builder()
            .id(UUID.randomUUID())
            .title(title)
            .author(author)
            .genre(genre)
            .build();
    }

}
