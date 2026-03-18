package com.library.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

import com.library.types.dto.ApiResponse;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.LinkedHashMap;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for auth endpoints (F007 — email-based auth with verification).
 *
 * <p>Uses Testcontainers for PostgreSQL. JavaMailSender is mocked so no real SMTP is needed.
 * Run: mvn verify -Dgroups=integration
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class AuthIntegrationTest {

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
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("spring.mail.port", () -> "1025");
    }

    @MockBean
    private JavaMailSender javaMailSender;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void stubMailSender() {
        // Return a real MimeMessage from createMimeMessage() so EmailServiceImpl can populate it
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        org.mockito.Mockito.when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(javaMailSender).send(any(MimeMessage.class));
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void register_validEmail_returns201() {
        String body = """
            {"email":"reg201@example.com","password":"password123","confirmPassword":"password123"}
            """;

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/register",
            new HttpEntity<>(body, jsonHeaders()),
            ApiResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(201);

        LinkedHashMap<?, ?> data = (LinkedHashMap<?, ?>) response.getBody().getData();
        assertThat(data).isNotNull();
        assertThat(data.get("email")).isEqualTo("reg201@example.com");
        assertThat(data.get("emailVerified")).isEqualTo(false);
        assertThat(data.get("id")).isNotNull();
    }

    @Test
    void register_duplicateEmail_returns409() {
        String body = """
            {"email":"dup409@example.com","password":"password123","confirmPassword":"password123"}
            """;

        restTemplate.postForEntity(
            "/api/v1/auth/register",
            new HttpEntity<>(body, jsonHeaders()),
            ApiResponse.class
        );

        ResponseEntity<ApiResponse> second = restTemplate.postForEntity(
            "/api/v1/auth/register",
            new HttpEntity<>(body, jsonHeaders()),
            ApiResponse.class
        );

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody().getStatus()).isEqualTo(409);
    }

    @Test
    void register_invalidEmailFormat_returns400() {
        String body = """
            {"email":"not-an-email","password":"password123","confirmPassword":"password123"}
            """;

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/register",
            new HttpEntity<>(body, jsonHeaders()),
            ApiResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }

    @Test
    void register_passwordMismatch_returns400() {
        String body = """
            {"email":"mismatch@example.com","password":"pass1","confirmPassword":"pass2"}
            """;

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/register",
            new HttpEntity<>(body, jsonHeaders()),
            ApiResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getStatus()).isEqualTo(400);
    }

    @Test
    void login_unverifiedUser_returns403() {
        // Register (email NOT verified)
        String registerBody = """
            {"email":"unverified403@example.com","password":"password123",
            "confirmPassword":"password123"}
            """;
        restTemplate.postForEntity(
            "/api/v1/auth/register",
            new HttpEntity<>(registerBody, jsonHeaders()),
            ApiResponse.class
        );

        // Try to login without verifying — expect 403
        String loginBody = """
            {"email":"unverified403@example.com","password":"password123"}
            """;

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/login",
            new HttpEntity<>(loginBody, jsonHeaders()),
            ApiResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getStatus()).isEqualTo(403);
    }

    @Test
    void login_invalidPassword_returns401() {
        // Register
        String registerBody = """
            {"email":"badpass401@example.com","password":"correctpass",
            "confirmPassword":"correctpass"}
            """;
        restTemplate.postForEntity(
            "/api/v1/auth/register",
            new HttpEntity<>(registerBody, jsonHeaders()),
            ApiResponse.class
        );

        // Try login with wrong password
        // TestRestTemplate throws ResourceAccessException on 401 due to
        // Java HttpURLConnection "cannot retry due to server authentication" behavior.
        String loginBody = """
            {"email":"badpass401@example.com","password":"wrongpassword"}
            """;

        assertThatThrownBy(() ->
            restTemplate.postForEntity(
                "/api/v1/auth/login",
                new HttpEntity<>(loginBody, jsonHeaders()),
                ApiResponse.class
            )
        ).isInstanceOf(org.springframework.web.client.ResourceAccessException.class);
    }

    @Test
    void login_nonexistentEmail_returns401() {
        // TestRestTemplate throws ResourceAccessException on 401.
        String loginBody = """
            {"email":"nobody@example.com","password":"anypassword"}
            """;

        assertThatThrownBy(() ->
            restTemplate.postForEntity(
                "/api/v1/auth/login",
                new HttpEntity<>(loginBody, jsonHeaders()),
                ApiResponse.class
            )
        ).isInstanceOf(org.springframework.web.client.ResourceAccessException.class);
    }

    @Test
    void verify_invalidToken_returnsErrorHtml() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "/api/v1/auth/verify?token=completely-invalid-token",
            String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Invalid Link");
    }

    @Test
    void resendVerification_returns200() {
        String body = """
            {"email":"anynoneexistent@example.com"}
            """;

        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
            "/api/v1/auth/resend-verification",
            new HttpEntity<>(body, jsonHeaders()),
            ApiResponse.class
        );

        // Should return 200 regardless (prevents email enumeration)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(200);
    }
}
