package br.com.smartdispatch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartDispatchOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Smart Dispatch API")
                                .description(
                                        "API para gestão e distribuição inteligente de chamados técnicos."
                                )
                                .version("v1")
                );
    }
}