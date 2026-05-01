package com.transit.transactionservice.config;

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
                        .title("Transaction Service API")
                        .description("Microservice for recording and retrieving transit card transactions. Communicates with card-service to update card balances on TOPUP and PAYMENT operations.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Transit API Team")
                                .email("api-support@transit.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8082").description("Local development"),
                        new Server().url("http://transaction-service:8082").description("Docker internal")
                ));
    }
}
