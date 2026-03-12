package com.library.types.dto;

import com.library.types.enums.Genre;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request object for book search queries.
 *
 * <p>Bound from HTTP query parameters in {@code BookController.searchBooks}.
 * Validation annotations prevent malformed or malicious input.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Book search parameters")
public class BookSearchRequest {

    @Size(max = 200, message = "Query must not exceed 200 characters")
    @Pattern(
        regexp = "^[\\w\\s\\-\\.,'\"/()&!?]*$",
        message = "Query contains invalid characters"
    )
    @Schema(description = "Search keyword (matches title, author, or ISBN)", example = "spring")
    private String q;

    @Schema(description = "Filter by genre", example = "TECHNOLOGY")
    private Genre genre;

}
