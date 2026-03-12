package com.library.unit.types.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.library.types.dto.BookSearchRequest;
import com.library.types.enums.Genre;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BookSearchRequest} validation constraints.
 */
class BookSearchRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validate_validQueryAndGenre_hasNoViolations() {
        BookSearchRequest request = BookSearchRequest.builder()
            .q("spring")
            .genre(Genre.TECHNOLOGY)
            .build();

        Set<ConstraintViolation<BookSearchRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validate_validQueryWithAllowedCharacters_hasNoViolations() {
        BookSearchRequest request = BookSearchRequest.builder()
            .q("word-with-dash, dot. apostrophe's \"quoted\" (parens) & !?")
            .build();

        Set<ConstraintViolation<BookSearchRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validate_nullQuery_hasNoViolations() {
        BookSearchRequest request = BookSearchRequest.builder()
            .genre(Genre.FICTION)
            .build();

        Set<ConstraintViolation<BookSearchRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validate_queryExceeds200Chars_hasViolation() {
        String longQuery = "a".repeat(201);
        BookSearchRequest request = BookSearchRequest.builder()
            .q(longQuery)
            .build();

        Set<ConstraintViolation<BookSearchRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .contains("200");
    }

    @Test
    void validate_queryExactly200Chars_hasNoViolations() {
        String maxQuery = "a".repeat(200);
        BookSearchRequest request = BookSearchRequest.builder()
            .q(maxQuery)
            .build();

        Set<ConstraintViolation<BookSearchRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void validate_queryWithInvalidCharacters_hasViolation() {
        BookSearchRequest request = BookSearchRequest.builder()
            .q("query@invalid#chars$")
            .build();

        Set<ConstraintViolation<BookSearchRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .contains("invalid characters");
    }
}
