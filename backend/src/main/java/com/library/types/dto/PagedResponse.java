package com.library.types.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

/**
 * Paginated response wrapper for list endpoints.
 *
 * <p>Wraps Spring's {@link Page} to produce a consistent JSON structure.
 * Use {@link #of(Page)} to convert a Spring Page to PagedResponse.
 *
 * <p>See docs/PATTERNS.md section "6. Pagination Pattern".
 *
 * @param <T> element type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated response")
public class PagedResponse<T> {

    @Schema(description = "List of items on this page")
    private List<T> content;

    @Schema(description = "Current page number (0-indexed)", example = "0")
    private int page;

    @Schema(description = "Number of items per page", example = "20")
    private int size;

    @Schema(description = "Total number of matching items", example = "42")
    private long totalElements;

    @Schema(description = "Total number of pages", example = "3")
    private int totalPages;

    @Schema(description = "Whether this is the last page", example = "false")
    private boolean last;

    /**
     * Converts a Spring Data {@link Page} to a {@link PagedResponse}.
     *
     * @param page Spring Data page result
     * @param <T> element type
     * @return paged response
     */
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
