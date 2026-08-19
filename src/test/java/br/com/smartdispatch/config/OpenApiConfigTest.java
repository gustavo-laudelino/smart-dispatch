package br.com.smartdispatch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiConfigTest {

    @Test
    void deveConfigurarOpenApiComBearerJwt() {

        // Arrange
        OpenApiConfig config = new OpenApiConfig();

        // Act
        OpenAPI openAPI = config.smartDispatchOpenAPI();

        // Assert
        assertEquals("Smart Dispatch API", openAPI.getInfo().getTitle());
        assertEquals(
                "API para gestão e distribuição inteligente de chamados técnicos.",
                openAPI.getInfo().getDescription()
        );
        assertEquals("v1", openAPI.getInfo().getVersion());

        SecurityScheme securityScheme =
                openAPI.getComponents().getSecuritySchemes().get("bearerAuth");

        assertNotNull(securityScheme);
        assertEquals(SecurityScheme.Type.HTTP, securityScheme.getType());
        assertEquals("bearer", securityScheme.getScheme());
        assertEquals("JWT", securityScheme.getBearerFormat());

        assertNotNull(openAPI.getSecurity());
        assertTrue(
                openAPI.getSecurity().stream()
                        .anyMatch(requisito -> requisito.containsKey("bearerAuth"))
        );
    }
}
