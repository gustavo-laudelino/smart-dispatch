package br.com.smartdispatch.integration;

import br.com.smartdispatch.enums.PerfilUsuario;
import br.com.smartdispatch.model.BaseOperacional;
import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.model.Tecnico;
import br.com.smartdispatch.model.Usuario;
import br.com.smartdispatch.service.TokenService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContratoHttpSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @Transactional
    void devePermitirCriarContratoComoAdmin() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        post("/contratos")
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                                .contentType("application/json")
                                .content("{\"cidade\":\"Cidade Nova Admin\"}")
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void devePermitirCriarContratoComoCto() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        post("/contratos")
                                .header("Authorization", "Bearer " + fixture.tokenCto())
                                .contentType("application/json")
                                .content("{\"cidade\":\"Cidade Nova Cto\"}")
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearCriarContratoComoTecnico() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        post("/contratos")
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                                .contentType("application/json")
                                .content("{\"cidade\":\"Cidade Nova Tecnico\"}")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirListarContratosComoAdmin() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get("/contratos")
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void devePermitirListarContratosComoTecnicoVinculado() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get("/contratos")
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(
                        jsonPath("$[0].id")
                                .value(fixture.contratoAId())
                );
    }

    @Test
    @Transactional
    void deveBloquearListarContratosComoTecnicoSemVinculo() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get("/contratos")
                                .header(
                                        "Authorization",
                                        "Bearer " + fixture.tokenTecnicoSemVinculo()
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirBuscarContratoPorIdComoTecnicoVinculado()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(
                                "/contratos/{id}",
                                fixture.contratoAId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearBuscarContratoPorIdDeOutroContrato() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(
                                "/contratos/{id}",
                                fixture.contratoBId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirAtualizarContratoComoCto() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        put(
                                "/contratos/{id}",
                                fixture.contratoAId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenCto())
                                .contentType("application/json")
                                .content("{\"cidade\":\"Cidade Atualizada\"}")
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearAtualizarContratoComoTecnico() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        put(
                                "/contratos/{id}",
                                fixture.contratoAId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                                .contentType("application/json")
                                .content("{\"cidade\":\"Cidade Atualizada\"}")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirExcluirContratoComoAdmin() throws Exception {
        Fixture fixture = criarFixture();
        Contrato contratoDescartavel = persistirContrato();

        mockMvc.perform(
                        delete(
                                "/contratos/{id}",
                                contratoDescartavel.getId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                )
                .andExpect(status().isOk());
    }

    private record Fixture(
            Long contratoAId,
            Long contratoBId,
            String tokenAdmin,
            String tokenCto,
            String tokenTecnicoA,
            String tokenTecnicoSemVinculo
    ) {
    }

    private Fixture criarFixture() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();

        BaseOperacional baseA = persistirBaseOperacional(contratoA);

        Usuario admin = persistirUsuario(
                "Admin Ator",
                "admin.ator.contrato@teste.local",
                PerfilUsuario.ADMIN
        );
        Usuario cto = persistirUsuario(
                "Cto Ator",
                "cto.ator.contrato@teste.local",
                PerfilUsuario.CTO
        );

        Usuario usuarioTecnicoA = persistirUsuario(
                "Tecnico Contrato A",
                "tecnico.a.contrato@teste.local",
                PerfilUsuario.TECNICO
        );
        persistirTecnico(usuarioTecnicoA, baseA);

        Usuario usuarioTecnicoSemVinculo = persistirUsuario(
                "Tecnico Sem Vinculo",
                "tecnico.sem.vinculo.contrato@teste.local",
                PerfilUsuario.TECNICO
        );

        return new Fixture(
                contratoA.getId(),
                contratoB.getId(),
                tokenService.gerarToken(admin),
                tokenService.gerarToken(cto),
                tokenService.gerarToken(usuarioTecnicoA),
                tokenService.gerarToken(usuarioTecnicoSemVinculo)
        );
    }

    private Contrato persistirContrato() {
        Contrato contrato = new Contrato();
        entityManager.persist(contrato);
        return contrato;
    }

    private BaseOperacional persistirBaseOperacional(Contrato contrato) {
        BaseOperacional baseOperacional = new BaseOperacional();
        baseOperacional.setContrato(contrato);
        entityManager.persist(baseOperacional);
        return baseOperacional;
    }

    private Usuario persistirUsuario(
            String nome,
            String email,
            PerfilUsuario perfil
    ) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setPerfil(perfil);
        usuario.setSenha("senha-teste");
        usuario.setAtivo(true);
        entityManager.persist(usuario);
        return usuario;
    }

    private Tecnico persistirTecnico(
            Usuario usuario,
            BaseOperacional baseOperacional
    ) {
        Tecnico tecnico = new Tecnico();
        tecnico.setUsuario(usuario);
        tecnico.setBaseOperacional(baseOperacional);
        tecnico.setAtivo(true);
        entityManager.persist(tecnico);
        return tecnico;
    }
}
