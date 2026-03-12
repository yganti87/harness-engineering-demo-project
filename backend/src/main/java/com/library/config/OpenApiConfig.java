package com.library.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI configuration.
 *
 * <p>Swagger UI available at: http://localhost:8080/swagger-ui.html
 * OpenAPI JSON spec at: http://localhost:8080/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI libraryOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Library Catalog API")
                .description("REST API for the library catalog application. "
                    + "Supports anonymous book search and browsing.")
                .version("0.1.0")
                .contact(new Contact()
                    .name("Library Team")
                    .url("http://localhost:8501")));
    }

}
