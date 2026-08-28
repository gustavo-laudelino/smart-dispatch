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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComentarioChamadoHttpSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String BASE_URL =
            "/contratos/{contratoId}/chamados/{chamadoId}/comentarios";

    @Test
    @Transactional
    void devePermitirCriarComentarioComoAdmin() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        post(BASE_URL, fixture.contratoAId(), fixture.chamadoAId())
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                                .contentType("application/json")
                                .content("{\"texto\":\"Comentario do admin\"}")
                )
                .andExpect(status().isCreated());
    }

    @Test
    @Transactional
    void devePermitirCriarComentarioComoTecnicoVinculado() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        post(BASE_URL, fixture.contratoAId(), fixture.chamadoAId())
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                                .contentType("application/json")
                                .content("{\"texto\":\"Comentario do tecnico\"}")
                )
                .andExpect(status().isCreated());
    }

    @Test
    @Transactional
    void deveBloquearCriarComentarioComoTecnicoDeOutroContrato()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        post(BASE_URL, fixture.contratoAId(), fixture.chamadoAId())
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoB())
                                .contentType("application/json")
                                .content("{\"texto\":\"Comentario indevido\"}")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirCriarComentarioComoTecnicoInternoVinculado()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        post(BASE_URL, fixture.contratoAId(), fixture.chamadoAId())
                                .header(
                                        "Authorization",
                                        "Bearer " + fixture.tokenTecnicoInternoA()
                                )
                                .contentType("application/json")
                                .content("{\"texto\":\"Comentario do tecnico interno\"}")
                )
                .andExpect(status().isCreated());
    }

    @Test
    @Transactional
    void devePermitirListarComentariosComoAdmin() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(BASE_URL, fixture.contratoAId(), fixture.chamadoAId())
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void devePermitirListarComentariosComoTecnicoVinculado()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(BASE_URL, fixture.contratoAId(), fixture.chamadoAId())
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearListarComentariosComoTecnicoDeOutroContrato()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(BASE_URL, fixture.contratoAId(), fixture.chamadoAId())
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoB())
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void deveBloquearListarComentariosSemToken() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(BASE_URL, fixture.contratoAId(), fixture.chamadoAId())
                )
                .andExpect(status().isUnauthorized());
    }

    private record Fixture(
            Long contratoAId,
            Long chamadoAId,
            String tokenAdmin,
            String tokenTecnicoA,
            String tokenTecnicoInternoA,
            String tokenTecnicoB
    ) {
    }

    private Fixture criarFixture() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();

        BaseOperacional baseA = persistirBaseOperacional(contratoA);
        BaseOperacional baseB = persistirBaseOperacional(contratoB);

        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado("CH-COM-001", unidadeA);

        Usuario admin = persistirUsuario(
                "Admin Ator",
                "admin.ator.comentario@teste.local",
                PerfilUsuario.ADMIN
        );
        Usuario usuarioTecnicoA = persistirUsuario(
                "Tecnico Contrato A",
                "tecnico.a.comentario@teste.local",
                PerfilUsuario.TECNICO
        );
        persistirTecnico(usuarioTecnicoA, baseA);

        Usuario usuarioTecnicoInternoA = persistirUsuario(
                "Tecnico Interno Contrato A",
                "tecnico.interno.a.comentario@teste.local",
                PerfilUsuario.TECNICO_INTERNO
        );
        persistirTecnico(usuarioTecnicoInternoA, baseA);

        Usuario usuarioTecnicoB = persistirUsuario(
                "Tecnico Contrato B",
                "tecnico.b.comentario@teste.local",
                PerfilUsuario.TECNICO
        );
        persistirTecnico(usuarioTecnicoB, baseB);

        return new Fixture(
                contratoA.getId(),
                chamadoA.getId(),
                tokenService.gerarToken(admin),
                tokenService.gerarToken(usuarioTecnicoA),
                tokenService.gerarToken(usuarioTecnicoInternoA),
                tokenService.gerarToken(usuarioTecnicoB)
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
