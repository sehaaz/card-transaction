package com.transit.cardservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Card Service API")
                        .description("Microservice for managing transit cards — create cards, retrieve card details, and top up balances.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Transit API Team")
                                .email("api-support@transit.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Local development"),
                        new Server().url("http://card-service:8081").description("Docker internal")
                ));
    }
}
