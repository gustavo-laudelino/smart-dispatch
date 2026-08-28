package br.com.smartdispatch.integration;

import br.com.smartdispatch.enums.PerfilUsuario;
import br.com.smartdispatch.model.BaseOperacional;
import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.model.Tecnico;
import br.com.smartdispatch.model.Unidade;
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
class UnidadeHttpSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String BASE_URL = "/contratos/{contratoId}/unidades";

    @Test
    @Transactional
    void devePermitirCriarUnidadeComoAdmin() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        post(BASE_URL, fixture.contratoAId())
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                                .contentType("application/json")
                                .content("{\"nome\":\"Unidade Nova Admin\"}")
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void devePermitirCriarUnidadeComoCto() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        post(BASE_URL, fixture.contratoAId())
                                .header("Authorization", "Bearer " + fixture.tokenCto())
                                .contentType("application/json")
                                .content("{\"nome\":\"Unidade Nova Cto\"}")
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearCriarUnidadeComoTecnicoVinculado() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        post(BASE_URL, fixture.contratoAId())
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                                .contentType("application/json")
                                .content("{\"nome\":\"Unidade Nova Tecnico\"}")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirListarUnidadesComoTecnicoMesmoContrato()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(BASE_URL, fixture.contratoAId())
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearListarUnidadesComoTecnicoDeOutroContrato()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(BASE_URL, fixture.contratoAId())
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoB())
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirBuscarUnidadePorIdComoTecnicoMesmoContrato()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(
                                BASE_URL + "/{unidadeId}",
                                fixture.contratoAId(),
                                fixture.unidadeAId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void devePermitirBuscarUnidadePorIdComoTecnicoInternoMesmoContrato()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(
                                BASE_URL + "/{unidadeId}",
                                fixture.contratoAId(),
                                fixture.unidadeAId()
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
    void deveBloquearBuscarUnidadePorIdComoTecnicoInternoDeOutroContrato()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(
                                BASE_URL + "/{unidadeId}",
                                fixture.contratoAId(),
                                fixture.unidadeAId()
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
    void devePermitirAtualizarUnidadeComoCto() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        put(
                                BASE_URL + "/{unidadeId}",
                                fixture.contratoAId(),
                                fixture.unidadeAId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenCto())
                                .contentType("application/json")
                                .content("{\"nome\":\"Unidade Atualizada\"}")
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearAtualizarUnidadeComoTecnicoDoProprioContrato()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        put(
                                BASE_URL + "/{unidadeId}",
                                fixture.contratoAId(),
                                fixture.unidadeAId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                                .contentType("application/json")
                                .content("{\"nome\":\"Unidade Atualizada\"}")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirExcluirUnidadeComoAdmin() throws Exception {
        Fixture fixture = criarFixture();
        Unidade unidadeDescartavel =
                persistirUnidade(fixture.contratoA());

        mockMvc.perform(
                        delete(
                                BASE_URL + "/{unidadeId}",
                                fixture.contratoAId(),
                                unidadeDescartavel.getId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                )
                .andExpect(status().isOk());
    }

    private record Fixture(
            Contrato contratoA,
            Long contratoAId,
            Long unidadeAId,
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

        Unidade unidadeA = persistirUnidade(contratoA);

        Usuario admin = persistirUsuario(
                "Admin Ator",
                "admin.ator.unidade@teste.local",
                PerfilUsuario.ADMIN
        );
        Usuario cto = persistirUsuario(
                "Cto Ator",
                "cto.ator.unidade@teste.local",
                PerfilUsuario.CTO
        );

        Usuario usuarioTecnicoA = persistirUsuario(
                "Tecnico Contrato A",
                "tecnico.a.unidade@teste.local",
                PerfilUsuario.TECNICO
        );
        persistirTecnico(usuarioTecnicoA, baseA);

        Usuario usuarioTecnicoInternoA = persistirUsuario(
                "Tecnico Interno Contrato A",
                "tecnico.interno.a.unidade@teste.local",
                PerfilUsuario.TECNICO_INTERNO
        );
        persistirTecnico(usuarioTecnicoInternoA, baseA);

        Usuario usuarioTecnicoB = persistirUsuario(
                "Tecnico Contrato B",
                "tecnico.b.unidade@teste.local",
                PerfilUsuario.TECNICO
        );
        persistirTecnico(usuarioTecnicoB, baseB);

        Usuario usuarioTecnicoInternoB = persistirUsuario(
                "Tecnico Interno Contrato B",
                "tecnico.interno.b.unidade@teste.local",
                PerfilUsuario.TECNICO_INTERNO
        );
        persistirTecnico(usuarioTecnicoInternoB, baseB);

        return new Fixture(
                contratoA,
                contratoA.getId(),
                unidadeA.getId(),
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

    private Unidade persistirUnidade(Contrato contrato) {
        Unidade unidade = new Unidade();
        unidade.setContrato(contrato);
        entityManager.persist(unidade);
        return unidade;
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
