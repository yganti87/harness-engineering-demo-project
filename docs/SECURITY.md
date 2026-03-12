# Security Guide

## Input Validation

All user input is validated at the controller boundary using `jakarta.validation`.

**Rule**: Never trust input. Validate on the DTO, not in service logic.

```java
public class BookSearchRequest {
    @Size(max = 200, message = "Query must not exceed 200 characters")
    @Pattern(regexp = "^[\\w\\s\\-\\.,'\"()&!?]*$",
             message = "Query contains invalid characters")
    private String q;

    // genre is validated via enum conversion — invalid value = 400
    private Genre genre;
}
```

Validation errors are caught by `GlobalExceptionHandler.handleValidation()` and returned as 400 with details.

## SQL Injection Prevention

**Rule**: Never concatenate user input into SQL strings.

Spring Data JPA and JPQL parameterized queries prevent SQL injection by default:

```java
// SAFE: parameterized query
@Query("SELECT b FROM BookEntity b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%'))")
Page<BookEntity> searchBooks(@Param("query") String query, Pageable pageable);

// UNSAFE: never do this
@Query("SELECT b FROM BookEntity b WHERE b.title LIKE '%" + userInput + "%'") // NEVER
```

## CORS Configuration

CORS is configured in `WebConfig.java`. Currently allows all origins for local development.

**Production rule**: Restrict `allowedOrigins` to the actual frontend URL only.

```java
// config/WebConfig.java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:8501")  // Streamlit frontend
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .maxAge(3600);
    }
}
```

## Actuator Security

Spring Actuator exposes operational endpoints. In this local setup all are exposed.

**Production rule**: Restrict actuator with Spring Security or expose only `/health` and `/info` publicly.

Currently exposed endpoints (see `application.yml`):
- `/actuator/health` — public, used by Docker health checks
- `/actuator/logfile` — exposes application logs (restrict in production)
- `/actuator/flyway` — exposes migration history (restrict in production)

## Data Sensitivity

Current data model (books catalog) contains no PII. As user authentication is added (F003), apply:
- Never log passwords, tokens, or user IDs at DEBUG level
- Hash passwords with BCrypt (strength ≥ 12)
- Use JWTs with short expiry (15 min access, 7 day refresh)
- Never include sensitive fields in error messages

## Dependency Vulnerabilities

Run `mvn dependency-check:check` periodically to scan for known CVEs in dependencies.
