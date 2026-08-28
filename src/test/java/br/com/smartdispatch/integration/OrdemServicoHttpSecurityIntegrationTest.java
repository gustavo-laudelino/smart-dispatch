package br.com.smartdispatch.integration;

import br.com.smartdispatch.enums.CategoriaChamado;
import br.com.smartdispatch.enums.PerfilUsuario;
import br.com.smartdispatch.enums.PrioridadeChamado;
import br.com.smartdispatch.enums.StatusChamado;
import br.com.smartdispatch.enums.TipoChamado;
import br.com.smartdispatch.model.BaseOperacional;
import br.com.smartdispatch.model.Chamado;
import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.model.OrdemServico;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrdemServicoHttpSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String BASE_URL =
            "/contratos/{contratoId}/chamados/{chamadoId}/ordens-servico";

    @Test
    @Transactional
    void devePermitirCriarOrdemServicoComoAdmin() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        post(BASE_URL, fixture.contratoAId(), fixture.chamadoAId())
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                                .contentType("application/json")
                                .content("{\"numeroOrdemServico\":\"OS-HTTP-001\"}")
                )
                .andExpect(status().isCreated());
    }

    @Test
    @Transactional
    void devePermitirCriarOrdemServicoComoCto() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        post(BASE_URL, fixture.contratoAId(), fixture.chamadoAId())
                                .header("Authorization", "Bearer " + fixture.tokenCto())
                                .contentType("application/json")
                                .content("{\"numeroOrdemServico\":\"OS-HTTP-002\"}")
                )
                .andExpect(status().isCreated());
    }

    @Test
    @Transactional
    void deveBloquearCriarOrdemServicoComoTecnico() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        post(BASE_URL, fixture.contratoAId(), fixture.chamadoAId())
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                                .contentType("application/json")
                                .content("{\"numeroOrdemServico\":\"OS-HTTP-003\"}")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirListarOrdemServicoComoAdmin() throws Exception {
        Fixture fixture = criarFixture();
        persistirOrdemServico(
                "OS-HTTP-004",
                fixture.chamadoA(),
                fixture.unidadeA(),
                null,
                null,
                null
        );

        mockMvc.perform(
                        get(BASE_URL, fixture.contratoAId(), fixture.chamadoAId())
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void devePermitirListarOrdemServicoComoCto() throws Exception {
        Fixture fixture = criarFixture();
        persistirOrdemServico(
                "OS-HTTP-005",
                fixture.chamadoA(),
                fixture.unidadeA(),
                null,
                null,
                null
        );

        mockMvc.perform(
                        get(BASE_URL, fixture.contratoAId(), fixture.chamadoAId())
                                .header("Authorization", "Bearer " + fixture.tokenCto())
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void devePermitirListarOrdemServicoComoTecnicoMesmoContrato()
            throws Exception {
        Fixture fixture = criarFixture();
        persistirOrdemServico(
                "OS-HTTP-006",
                fixture.chamadoA(),
                fixture.unidadeA(),
                null,
                null,
                null
        );

        mockMvc.perform(
                        get(BASE_URL, fixture.contratoAId(), fixture.chamadoAId())
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearListarOrdemServicoComoTecnicoDeOutroContrato()
            throws Exception {
        Fixture fixture = criarFixture();
        persistirOrdemServico(
                "OS-HTTP-007",
                fixture.chamadoA(),
                fixture.unidadeA(),
                null,
                null,
                null
        );

        mockMvc.perform(
                        get(BASE_URL, fixture.contratoAId(), fixture.chamadoAId())
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoB())
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirListarOrdemServicoComoTecnicoInternoMesmoContrato()
            throws Exception {
        Fixture fixture = criarFixture();
        persistirOrdemServico(
                "OS-HTTP-008",
                fixture.chamadoA(),
                fixture.unidadeA(),
                null,
                null,
                null
        );

        mockMvc.perform(
                        get(BASE_URL, fixture.contratoAId(), fixture.chamadoAId())
                                .header(
                                        "Authorization",
                                        "Bearer " + fixture.tokenTecnicoInternoA()
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearListarOrdemServicoComoTecnicoInternoDeOutroContrato()
            throws Exception {
        Fixture fixture = criarFixture();
        persistirOrdemServico(
                "OS-HTTP-009",
                fixture.chamadoA(),
                fixture.unidadeA(),
                null,
                null,
                null
        );

        mockMvc.perform(
                        get(BASE_URL, fixture.contratoAId(), fixture.chamadoAId())
                                .header(
                                        "Authorization",
                                        "Bearer " + fixture.tokenTecnicoInternoB()
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirAtualizarOrdemServicoComoAdmin() throws Exception {
        Fixture fixture = criarFixture();
        OrdemServico os = persistirOrdemServico(
                "OS-HTTP-010",
                fixture.chamadoA(),
                fixture.unidadeA(),
                null,
                null,
                null
        );

        mockMvc.perform(
                        put(
                                BASE_URL + "/{ordemServicoId}",
                                fixture.contratoAId(),
                                fixture.chamadoAId(),
                                os.getId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                                .contentType("application/json")
                                .content(
                                        "{\"numeroOrdemServico\":\"OS-HTTP-010-UPD\"}"
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void devePermitirAtualizarOrdemServicoComoCto() throws Exception {
        Fixture fixture = criarFixture();
        OrdemServico os = persistirOrdemServico(
                "OS-HTTP-011",
                fixture.chamadoA(),
                fixture.unidadeA(),
                null,
                null,
                null
        );

        mockMvc.perform(
                        put(
                                BASE_URL + "/{ordemServicoId}",
                                fixture.contratoAId(),
                                fixture.chamadoAId(),
                                os.getId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenCto())
                                .contentType("application/json")
                                .content(
                                        "{\"numeroOrdemServico\":\"OS-HTTP-011-UPD\"}"
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearAtualizarOrdemServicoComoTecnicoVinculado()
            throws Exception {
        Fixture fixture = criarFixture();
        OrdemServico os = persistirOrdemServico(
                "OS-HTTP-012",
                fixture.chamadoA(),
                fixture.unidadeA(),
                null,
                null,
                null
        );

        mockMvc.perform(
                        put(
                                BASE_URL + "/{ordemServicoId}",
                                fixture.contratoAId(),
                                fixture.chamadoAId(),
                                os.getId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                                .contentType("application/json")
                                .content(
                                        "{\"numeroOrdemServico\":\"OS-HTTP-012-UPD\"}"
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirCheckInDoProprioTecnicoAtribuido() throws Exception {
        Fixture fixture = criarFixture();
        OrdemServico os = persistirOrdemServico(
                "OS-HTTP-013",
                fixture.chamadoA(),
                fixture.unidadeA(),
                fixture.tecnicoA(),
                null,
                null
        );

        mockMvc.perform(
                        post(
                                BASE_URL + "/{ordemServicoId}/check-in",
                                fixture.contratoAId(),
                                fixture.chamadoAId(),
                                os.getId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                                .contentType("application/json")
                                .content("{}")
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearCheckInDeTecnicoNaoAtribuidoNoMesmoContrato()
            throws Exception {
        Fixture fixture = criarFixture();
        OrdemServico os = persistirOrdemServico(
                "OS-HTTP-014",
                fixture.chamadoA(),
                fixture.unidadeA(),
                fixture.tecnicoA(),
                null,
                null
        );

        mockMvc.perform(
                        post(
                                BASE_URL + "/{ordemServicoId}/check-in",
                                fixture.contratoAId(),
                                fixture.chamadoAId(),
                                os.getId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + fixture.tokenTecnicoInternoA()
                                )
                                .contentType("application/json")
                                .content("{}")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void deveBloquearCheckInDeTecnicoDeOutroContrato() throws Exception {
        Fixture fixture = criarFixture();
        OrdemServico os = persistirOrdemServico(
                "OS-HTTP-015",
                fixture.chamadoA(),
                fixture.unidadeA(),
                fixture.tecnicoA(),
                null,
                null
        );

        mockMvc.perform(
                        post(
                                BASE_URL + "/{ordemServicoId}/check-in",
                                fixture.contratoAId(),
                                fixture.chamadoAId(),
                                os.getId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoB())
                                .contentType("application/json")
                                .content("{}")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirCheckInDoTecnicoInternoAtribuido() throws Exception {
        Fixture fixture = criarFixture();
        OrdemServico os = persistirOrdemServico(
                "OS-HTTP-016",
                fixture.chamadoA(),
                fixture.unidadeA(),
                fixture.tecnicoInternoA(),
                null,
                null
        );

        mockMvc.perform(
                        post(
                                BASE_URL + "/{ordemServicoId}/check-in",
                                fixture.contratoAId(),
                                fixture.chamadoAId(),
                                os.getId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + fixture.tokenTecnicoInternoA()
                                )
                                .contentType("application/json")
                                .content("{}")
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void devePermitirCheckOutDoProprioTecnicoAtribuido() throws Exception {
        Fixture fixture = criarFixture();
        OrdemServico os = persistirOrdemServico(
                "OS-HTTP-017",
                fixture.chamadoA(),
                fixture.unidadeA(),
                fixture.tecnicoA(),
                LocalDateTime.of(2026, 8, 23, 9, 0),
                null
        );
        os.setDataAtribuicaoTecnico(
                LocalDateTime.of(2026, 8, 23, 8, 30)
        );
        fixture.chamadoA().setStatus(StatusChamado.EM_ATENDIMENTO);

        mockMvc.perform(
                        post(
                                BASE_URL + "/{ordemServicoId}/check-out",
                                fixture.contratoAId(),
                                fixture.chamadoAId(),
                                os.getId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearCheckOutDeTecnicoNaoAtribuido() throws Exception {
        Fixture fixture = criarFixture();
        OrdemServico os = persistirOrdemServico(
                "OS-HTTP-018",
                fixture.chamadoA(),
                fixture.unidadeA(),
                fixture.tecnicoA(),
                LocalDateTime.of(2026, 8, 23, 9, 0),
                null
        );

        mockMvc.perform(
                        post(
                                BASE_URL + "/{ordemServicoId}/check-out",
                                fixture.contratoAId(),
                                fixture.chamadoAId(),
                                os.getId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + fixture.tokenTecnicoInternoA()
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirListarSugestoesTecnicosComoAdmin() throws Exception {
        Fixture fixture = criarFixture();
        OrdemServico os = persistirOrdemServico(
                "OS-HTTP-019",
                fixture.chamadoA(),
                fixture.unidadeA(),
                null,
                null,
                null
        );

        mockMvc.perform(
                        get(
                                BASE_URL + "/{ordemServicoId}/sugestoes-tecnicos",
                                fixture.contratoAId(),
                                fixture.chamadoAId(),
                                os.getId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearListarSugestoesTecnicosComoTecnico() throws Exception {
        Fixture fixture = criarFixture();
        OrdemServico os = persistirOrdemServico(
                "OS-HTTP-020",
                fixture.chamadoA(),
                fixture.unidadeA(),
                null,
                null,
                null
        );

        mockMvc.perform(
                        get(
                                BASE_URL + "/{ordemServicoId}/sugestoes-tecnicos",
                                fixture.contratoAId(),
                                fixture.chamadoAId(),
                                os.getId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                )
                .andExpect(status().isForbidden());
    }

    private record Fixture(
            Long contratoAId,
            Unidade unidadeA,
            Chamado chamadoA,
            Long chamadoAId,
            Tecnico tecnicoA,
            Tecnico tecnicoInternoA,
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

        BaseOperacional baseA = persistirBaseOperacional(
                contratoA,
                -23.55,
                -46.63
        );
        BaseOperacional baseB = persistirBaseOperacional(
                contratoB,
                -22.90,
                -43.20
        );

        Unidade unidadeA = persistirUnidade(contratoA, -23.50, -46.60);
        Chamado chamadoA = persistirChamado("CH-HTTP-OS-001", unidadeA);

        Usuario admin = persistirUsuario(
                "Admin Ator",
                "admin.ator.os@teste.local",
                PerfilUsuario.ADMIN
        );
        Usuario cto = persistirUsuario(
                "Cto Ator",
                "cto.ator.os@teste.local",
                PerfilUsuario.CTO
        );

        Usuario usuarioTecnicoA = persistirUsuario(
                "Tecnico Contrato A",
                "tecnico.a.os@teste.local",
                PerfilUsuario.TECNICO
        );
        Tecnico tecnicoA = persistirTecnico(usuarioTecnicoA, baseA);

        Usuario usuarioTecnicoInternoA = persistirUsuario(
                "Tecnico Interno Contrato A",
                "tecnico.interno.a.os@teste.local",
                PerfilUsuario.TECNICO_INTERNO
        );
        Tecnico tecnicoInternoA = persistirTecnico(usuarioTecnicoInternoA, baseA);

        Usuario usuarioTecnicoB = persistirUsuario(
                "Tecnico Contrato B",
                "tecnico.b.os@teste.local",
                PerfilUsuario.TECNICO
        );
        persistirTecnico(usuarioTecnicoB, baseB);

        Usuario usuarioTecnicoInternoB = persistirUsuario(
                "Tecnico Interno Contrato B",
                "tecnico.interno.b.os@teste.local",
                PerfilUsuario.TECNICO_INTERNO
        );
        persistirTecnico(usuarioTecnicoInternoB, baseB);

        return new Fixture(
                contratoA.getId(),
                unidadeA,
                chamadoA,
                chamadoA.getId(),
                tecnicoA,
                tecnicoInternoA,
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

    private BaseOperacional persistirBaseOperacional(
            Contrato contrato,
            Double latitude,
            Double longitude
    ) {
        BaseOperacional baseOperacional = new BaseOperacional();
        baseOperacional.setContrato(contrato);
        baseOperacional.setLatitude(latitude);
        baseOperacional.setLongitude(longitude);
        entityManager.persist(baseOperacional);
        return baseOperacional;
    }

    private Unidade persistirUnidade(
            Contrato contrato,
            Double latitude,
            Double longitude
    ) {
        Unidade unidade = new Unidade();
        unidade.setContrato(contrato);
        unidade.setLatitude(latitude);
        unidade.setLongitude(longitude);
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

    private OrdemServico persistirOrdemServico(
            String numeroOrdemServico,
            Chamado chamado,
            Unidade unidadeAtendimento,
            Tecnico tecnico,
            LocalDateTime dataCheckIn,
            LocalDateTime dataCheckOut
    ) {
        OrdemServico ordemServico = new OrdemServico();
        ordemServico.setNumeroOrdemServico(numeroOrdemServico);
        ordemServico.setChamado(chamado);
        ordemServico.setUnidadeAtendimento(unidadeAtendimento);
        ordemServico.setTecnico(tecnico);
        ordemServico.setDataCheckIn(dataCheckIn);
        ordemServico.setDataCheckOut(dataCheckOut);
        entityManager.persist(ordemServico);
        return ordemServico;
    }
}
