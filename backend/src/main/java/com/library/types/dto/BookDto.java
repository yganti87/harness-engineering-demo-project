package com.library.types.dto;

import com.library.types.enums.Genre;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object representing a book in the catalog.
 *
 * <p>Used in API responses. Mapped from {@code BookEntity} in service layer.
 * See docs/PATTERNS.md for the mapping pattern.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Book catalog entry")
public class BookDto {

    @Schema(description = "Unique book identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Book title", example = "Spring in Action")
    private String title;

    @Schema(description = "Author name(s)", example = "Craig Walls")
    private String author;

    @Schema(description = "ISBN-10 or ISBN-13", example = "9781617294945")
    private String isbn;

    @Schema(description = "Book genre", example = "TECHNOLOGY")
    private Genre genre;

    @Schema(description = "Year of publication", example = "2022")
    private Integer publicationYear;

    @Schema(description = "Book description / synopsis")
    private String description;

}
