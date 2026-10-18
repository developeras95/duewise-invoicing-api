package com.duewise.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI duewiseOpenApi(@Value("${duewise.api.version:0.1.0}") String version) {
        return new OpenAPI().info(new Info()
                .title("Duewise API")
                .version(version)
                .description("""
                        Invoicing, payments and overdue reminders for freelancers and small businesses.
                        Manage clients, raise invoices, record payments and see what is overdue."""));
    }
}
