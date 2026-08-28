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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BaseOperacionalHttpSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String BASE_URL = "/contratos/{contratoId}/bases";

    @Test
    @Transactional
    void devePermitirCriarBaseComoAdmin() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        post(BASE_URL, fixture.contratoAId())
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                                .contentType("application/json")
                                .content("{\"nome\":\"Base Nova Admin\"}")
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void devePermitirCriarBaseComoCto() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        post(BASE_URL, fixture.contratoAId())
                                .header("Authorization", "Bearer " + fixture.tokenCto())
                                .contentType("application/json")
                                .content("{\"nome\":\"Base Nova Cto\"}")
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearCriarBaseComoTecnicoVinculado() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        post(BASE_URL, fixture.contratoAId())
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                                .contentType("application/json")
                                .content("{\"nome\":\"Base Nova Tecnico\"}")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirListarBasesComoTecnicoMesmoContrato() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(BASE_URL, fixture.contratoAId())
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearListarBasesComoTecnicoDeOutroContrato() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(BASE_URL, fixture.contratoAId())
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoB())
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirBuscarBasePorIdComoTecnicoMesmoContrato()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(
                                BASE_URL + "/{baseId}",
                                fixture.contratoAId(),
                                fixture.baseAId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void devePermitirBuscarBasePorIdComoTecnicoInternoMesmoContrato()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(
                                BASE_URL + "/{baseId}",
                                fixture.contratoAId(),
                                fixture.baseAId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + fixture.tokenTecnicoInternoA()
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearBuscarBasePorIdComoTecnicoInternoDeOutroContrato()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(
                                BASE_URL + "/{baseId}",
                                fixture.contratoAId(),
                                fixture.baseAId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + fixture.tokenTecnicoInternoB()
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirAtualizarBaseComoCto() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        put(
                                BASE_URL + "/{baseId}",
                                fixture.contratoAId(),
                                fixture.baseAId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenCto())
                                .contentType("application/json")
                                .content("{\"nome\":\"Base Atualizada\"}")
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearAtualizarBaseComoTecnicoDoProprioContrato()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        put(
                                BASE_URL + "/{baseId}",
                                fixture.contratoAId(),
                                fixture.baseAId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                                .contentType("application/json")
                                .content("{\"nome\":\"Base Atualizada\"}")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirExcluirBaseComoAdmin() throws Exception {
        Fixture fixture = criarFixture();
        BaseOperacional baseDescartavel =
                persistirBaseOperacional(fixture.contratoA());

        mockMvc.perform(
                        delete(
                                BASE_URL + "/{baseId}",
                                fixture.contratoAId(),
                                baseDescartavel.getId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                )
                .andExpect(status().isOk());
    }

    private record Fixture(
            Contrato contratoA,
            Long contratoAId,
            Long baseAId,
            String tokenAdmin,
            String tokenCto,
            String tokenTecnicoA,
            String tokenTecnicoInternoA,
            String tokenTecnicoB,
            String tokenTecnicoInternoB
    ) {
    }

    private Fixture criarFixture() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();

        BaseOperacional baseA = persistirBaseOperacional(contratoA);
        BaseOperacional baseB = persistirBaseOperacional(contratoB);

        Usuario admin = persistirUsuario(
                "Admin Ator",
                "admin.ator.base@teste.local",
                PerfilUsuario.ADMIN
        );
        Usuario cto = persistirUsuario(
                "Cto Ator",
                "cto.ator.base@teste.local",
                PerfilUsuario.CTO
        );

        Usuario usuarioTecnicoA = persistirUsuario(
                "Tecnico Contrato A",
                "tecnico.a.base@teste.local",
                PerfilUsuario.TECNICO
        );
        persistirTecnico(usuarioTecnicoA, baseA);

        Usuario usuarioTecnicoInternoA = persistirUsuario(
                "Tecnico Interno Contrato A",
                "tecnico.interno.a.base@teste.local",
                PerfilUsuario.TECNICO_INTERNO
        );
        persistirTecnico(usuarioTecnicoInternoA, baseA);

        Usuario usuarioTecnicoB = persistirUsuario(
                "Tecnico Contrato B",
                "tecnico.b.base@teste.local",
                PerfilUsuario.TECNICO
        );
        persistirTecnico(usuarioTecnicoB, baseB);

        Usuario usuarioTecnicoInternoB = persistirUsuario(
                "Tecnico Interno Contrato B",
                "tecnico.interno.b.base@teste.local",
                PerfilUsuario.TECNICO_INTERNO
        );
        persistirTecnico(usuarioTecnicoInternoB, baseB);

        return new Fixture(
                contratoA,
                contratoA.getId(),
                baseA.getId(),
                tokenService.gerarToken(admin),
                tokenService.gerarToken(cto),
                tokenService.gerarToken(usuarioTecnicoA),
                tokenService.gerarToken(usuarioTecnicoInternoA),
                tokenService.gerarToken(usuarioTecnicoB),
                tokenService.gerarToken(usuarioTecnicoInternoB)
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
