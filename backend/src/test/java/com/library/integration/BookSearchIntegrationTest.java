package com.library.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.library.types.dto.ApiResponse;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for book search endpoints.
 *
 * <p>Uses Testcontainers to spin up a real PostgreSQL 15 instance.
 * Tests run against the Flyway-migrated schema with seed data from V1.
 *
 * <p>Requires Docker daemon running. Tag: integration
 * Run: {@code mvn test -Dgroups=integration}
 *
 * <p>Seed data (from V1 migration) includes:
 * - 4 TECHNOLOGY books (Spring in Action, Spring Boot in Practice, Clean Code,
 *   Designing Data-Intensive Applications)
 * - 1 HISTORY book (Sapiens)
 * - 1 FICTION book (The Great Gatsby)
 * - 1 SCIENCE book (Thinking Fast and Slow)
 * - 1 SCIENCE_FICTION book (Hitchhiker's Guide)
 * - 1 SELF_HELP book (Atomic Habits)
 * - 1 TECHNOLOGY book (Designing Data-Intensive Applications) = total 5 TECHNOLOGY
 * Total: 10 books
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class BookSearchIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("library_db")
            .withUsername("library_user")
            .withPassword("library_pass");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void search_emptyQuery_returns200WithAllBooks() {
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
            "/api/v1/books/search?page=0&size=20", ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(200);

        LinkedHashMap<?, ?> data = (LinkedHashMap<?, ?>) response.getBody().getData();
        assertThat(data).isNotNull();
        assertThat((Integer) data.get("totalElements")).isEqualTo(10);
        assertThat((List<?>) data.get("content")).hasSize(10);
    }

    @Test
    void search_withKeyword_returnsMatchingBooks() {
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
            "/api/v1/books/search?q=spring&page=0&size=20", ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        LinkedHashMap<?, ?> data = (LinkedHashMap<?, ?>) response.getBody().getData();
        List<?> content = (List<?>) data.get("content");

        // Should find "Spring in Action" and "Spring Boot in Practice"
        assertThat(content).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void search_withGenreFilter_returnsOnlyMatchingGenre() {
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
            "/api/v1/books/search?genre=HISTORY&page=0&size=20", ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        LinkedHashMap<?, ?> data = (LinkedHashMap<?, ?>) response.getBody().getData();
        List<?> content = (List<?>) data.get("content");

        // Should find "Sapiens" (1 HISTORY book in seed data)
        assertThat(content).hasSize(1);
    }

    @Test
    void search_withNonMatchingKeyword_returnsEmptyResults() {
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
            "/api/v1/books/search?q=xyznotexistingbook999&page=0&size=20", ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        LinkedHashMap<?, ?> data = (LinkedHashMap<?, ?>) response.getBody().getData();
        assertThat((Integer) data.get("totalElements")).isEqualTo(0);
        assertThat((List<?>) data.get("content")).isEmpty();
    }

    @Test
    void search_withPagination_returnCorrectPage() {
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
            "/api/v1/books/search?page=0&size=5", ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        LinkedHashMap<?, ?> data = (LinkedHashMap<?, ?>) response.getBody().getData();
        assertThat((List<?>) data.get("content")).hasSize(5);
        assertThat((Integer) data.get("totalElements")).isEqualTo(10);
        assertThat((Integer) data.get("totalPages")).isEqualTo(2);
        assertThat((Boolean) data.get("last")).isFalse();
    }

    @Test
    void getBookById_existingId_returnsBook() {
        // Use a known seed UUID from V1 migration
        String knownId = "11111111-1111-1111-1111-111111111111";
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
            "/api/v1/books/" + knownId, ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        LinkedHashMap<?, ?> data = (LinkedHashMap<?, ?>) response.getBody().getData();
        assertThat(data.get("title")).isEqualTo("Spring in Action");
        assertThat(data.get("author")).isEqualTo("Craig Walls");
    }

    @Test
    void getBookById_nonExistentId_returns404() {
        String nonExistentId = "00000000-0000-0000-0000-000000000000";
        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
            "/api/v1/books/" + nonExistentId, ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).contains("Book not found");
    }

}
