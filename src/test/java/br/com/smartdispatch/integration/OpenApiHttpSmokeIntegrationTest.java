package br.com.smartdispatch.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiHttpSmokeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveExporDocumentoOpenApiPublicoComInfoCorreto() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.info.title")
                                .value("Smart Dispatch API")
                )
                .andExpect(jsonPath("$.info.version").value("v1"));
    }

    @Test
    void deveExporSecuritySchemeBearerAuthCorreto() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.components.securitySchemes.bearerAuth")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.components.securitySchemes.bearerAuth.type")
                                .value("http")
                )
                .andExpect(
                        jsonPath(
                                "$.components.securitySchemes.bearerAuth.scheme"
                        ).value("bearer")
                )
                .andExpect(
                        jsonPath(
                                "$.components.securitySchemes.bearerAuth.bearerFormat"
                        ).value("JWT")
                );
    }

    @Test
    void deveDocumentarRotasCriticas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/usuarios']").exists())
                .andExpect(jsonPath("$.paths['/contratos']").exists())
                .andExpect(
                        jsonPath(
                                "$.paths['/contratos/{contratoId}/chamados']"
                        ).exists()
                )
                .andExpect(
                        jsonPath(
                                "$.paths['/contratos/{contratoId}/chamados/{chamadoId}/ordens-servico']"
                        ).exists()
                );
    }
}
