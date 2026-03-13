package com.library.types.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard API response envelope.
 *
 * <p>Every controller method must return {@code ResponseEntity<ApiResponse<T>>}.
 * Never return raw DTOs from controllers.
 *
 * <p>Success: {@code ApiResponse.success(data)}
 * Error: {@code ApiResponse.error(statusCode, message)}
 *
 * <p>See docs/PATTERNS.md for usage examples.
 *
 * @param <T> type of the data payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private int status;
    private T data;
    private String error;
    private Instant timestamp;

    /**
     * Creates a successful 200 response wrapping the given data.
     *
     * @param data the response payload
     * @param <T> type of the payload
     * @return a 200 ApiResponse
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .status(200)
            .data(data)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Creates a successful 201 Created response.
     *
     * @param data the response payload
     * @param <T>  type of the payload
     * @return a 201 ApiResponse
     */
    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
            .status(201)
            .data(data)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Creates an error response with the given HTTP status code and message.
     *
     * @param statusCode HTTP status code (e.g., 404, 400, 500)
     * @param message human-readable error description
     * @param <T> type parameter (unused for errors)
     * @return an error ApiResponse
     */
    public static <T> ApiResponse<T> error(int statusCode, String message) {
        return ApiResponse.<T>builder()
            .status(statusCode)
            .error(message)
            .timestamp(Instant.now())
            .build();
    }

}
