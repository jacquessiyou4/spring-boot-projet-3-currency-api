package com.kfokam48.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI / Swagger de l'API de conversion.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI currencyOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Currency API")
                        .description("API de Conversion de Devises avec WebClient et Cache Redis")
                        .version("1.0.0"));
    }
}
