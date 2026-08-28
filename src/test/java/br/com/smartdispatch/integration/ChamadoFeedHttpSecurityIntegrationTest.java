package br.com.smartdispatch.integration;

import br.com.smartdispatch.enums.CategoriaChamado;
import br.com.smartdispatch.enums.PerfilUsuario;
import br.com.smartdispatch.enums.PrioridadeChamado;
import br.com.smartdispatch.enums.StatusChamado;
import br.com.smartdispatch.enums.TipoChamado;
import br.com.smartdispatch.model.BaseOperacional;
import br.com.smartdispatch.model.Chamado;
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

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChamadoFeedHttpSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @Transactional
    void devePermitirListarFeedComoAdminSemContratoId() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get("/chamados")
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void devePermitirListarFeedComoCtoComContratoId() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get("/chamados")
                                .param(
                                        "contratoId",
                                        fixture.contratoAId().toString()
                                )
                                .header("Authorization", "Bearer " + fixture.tokenCto())
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void devePermitirListarFeedComoTecnicoVinculadoSemContratoId()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get("/chamados")
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(
                        jsonPath("$.content[0].numeroChamado")
                                .value("CH-FEED-A")
                );
    }

    @Test
    @Transactional
    void devePermitirListarFeedComoTecnicoComContratoIdPermitido()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get("/chamados")
                                .param(
                                        "contratoId",
                                        fixture.contratoAId().toString()
                                )
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearListarFeedComoTecnicoComContratoIdDeOutroContrato()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get("/chamados")
                                .param(
                                        "contratoId",
                                        fixture.contratoBId().toString()
                                )
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void deveBloquearListarFeedComoTecnicoSemVinculo() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get("/chamados")
                                .header(
                                        "Authorization",
                                        "Bearer " + fixture.tokenTecnicoSemVinculo()
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirListarFeedComoTecnicoInternoVinculadoSemContratoId()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get("/chamados")
                                .header(
                                        "Authorization",
                                        "Bearer " + fixture.tokenTecnicoInternoA()
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    void deveBloquearListarFeedSemToken() throws Exception {
        mockMvc.perform(get("/chamados"))
                .andExpect(status().isUnauthorized());
    }

    private record Fixture(
            Long contratoAId,
            Long contratoBId,
            String tokenAdmin,
            String tokenCto,
            String tokenTecnicoA,
            String tokenTecnicoInternoA,
            String tokenTecnicoSemVinculo
    ) {
    }

    private Fixture criarFixture() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();

        BaseOperacional baseA = persistirBaseOperacional(contratoA);

        Unidade unidadeA = persistirUnidade(contratoA);
        Unidade unidadeB = persistirUnidade(contratoB);

        persistirChamado("CH-FEED-A", unidadeA);
        persistirChamado("CH-FEED-B", unidadeB);

        Usuario admin = persistirUsuario(
                "Admin Ator",
                "admin.ator.feed@teste.local",
                PerfilUsuario.ADMIN
        );
        Usuario cto = persistirUsuario(
                "Cto Ator",
                "cto.ator.feed@teste.local",
                PerfilUsuario.CTO
        );

        Usuario usuarioTecnicoA = persistirUsuario(
                "Tecnico Contrato A",
                "tecnico.a.feed@teste.local",
                PerfilUsuario.TECNICO
        );
        persistirTecnico(usuarioTecnicoA, baseA);

        Usuario usuarioTecnicoInternoA = persistirUsuario(
                "Tecnico Interno Contrato A",
                "tecnico.interno.a.feed@teste.local",
                PerfilUsuario.TECNICO_INTERNO
        );
        persistirTecnico(usuarioTecnicoInternoA, baseA);

        Usuario usuarioTecnicoSemVinculo = persistirUsuario(
                "Tecnico Sem Vinculo",
                "tecnico.sem.vinculo.feed@teste.local",
                PerfilUsuario.TECNICO
        );

        return new Fixture(
                contratoA.getId(),
                contratoB.getId(),
                tokenService.gerarToken(admin),
                tokenService.gerarToken(cto),
                tokenService.gerarToken(usuarioTecnicoA),
                tokenService.gerarToken(usuarioTecnicoInternoA),
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

    private Unidade persistirUnidade(Contrato contrato) {
        Unidade unidade = new Unidade();
        unidade.setContrato(contrato);
        entityManager.persist(unidade);
        return unidade;
    }

    private Chamado persistirChamado(String numeroChamado, Unidade unidade) {
        Chamado chamado = new Chamado();
        chamado.setNumeroChamado(numeroChamado);
        chamado.setLinkChamadoOsti(
                "https://teste.local/chamado/" + numeroChamado
        );
        chamado.setUnidade(unidade);
        chamado.setTipo(TipoChamado.INCIDENTE);
        chamado.setCategoria(CategoriaChamado.OUTROS);
        chamado.setPrioridade(PrioridadeChamado.MEDIA);
        chamado.setStatus(StatusChamado.ABERTO);
        chamado.setDescricao("Chamado para teste de integração HTTP");
        chamado.setDataAbertura(LocalDateTime.of(2026, 8, 23, 8, 0));
        entityManager.persist(chamado);
        return chamado;
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
