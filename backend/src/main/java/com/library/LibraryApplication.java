package com.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Library catalog application entry point.
 *
 * <p>Start the application:
 * <ul>
 *   <li>Docker: {@code ./scripts/start.sh}</li>
 *   <li>Local: {@code cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local}</li>
 * </ul>
 */
@SpringBootApplication
public class LibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }

}
