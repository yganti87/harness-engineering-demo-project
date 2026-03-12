package com.library.repository;

import com.library.repository.entity.BookEntity;
import com.library.types.enums.Genre;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link BookEntity}.
 *
 * <p>All queries use JPQL with parameterized inputs to prevent SQL injection.
 * See docs/SECURITY.md and docs/PATTERNS.md section "2. Repository Pattern".
 */
@Repository
public interface BookRepository extends JpaRepository<BookEntity, UUID> {

    /**
     * Searches books by optional keyword and/or genre with pagination.
     *
     * <p>The keyword matches title, author, or ISBN (case-insensitive LIKE).
     * Null or empty keyword matches all books.
     * Null genre matches all genres.
     *
     * @param query search keyword (nullable)
     * @param genre genre filter (nullable)
     * @param pageable pagination parameters
     * @return page of matching book entities
     */
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
