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

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobertura HTTP/security da rota canônica {@code /contratos/{contratoId}/ordens-servico}
 * (OS com ou sem Chamado vinculado). As rotas antigas, aninhadas em Chamado, já são
 * cobertas por {@link OrdemServicoHttpSecurityIntegrationTest}; este arquivo foca no que
 * é específico da rota canônica: OS avulsa, filtros de listagem e autorização das novas
 * rotas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrdemServicoContratoHttpSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String BASE_URL = "/contratos/{contratoId}/ordens-servico";

    // ================= Criação =================

    @Test
    @Transactional
    void devePermitirCriarOrdemServicoVinculadaAChamadoComoAdmin() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        post(BASE_URL, fixture.contratoAId())
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                                .contentType("application/json")
                                .content("{\"chamadoId\": " + fixture.chamadoAId() + "}")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chamadoId").value(fixture.chamadoAId()));
    }

    @Test
    @Transactional
    void devePermitirCriarOrdemServicoAvulsaComoAdmin() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        post(BASE_URL, fixture.contratoAId())
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                                .contentType("application/json")
                                .content(
                                        "{\"unidadeAtendimentoId\": "
                                                + fixture.unidadeA1Id()
                                                + ", \"descricao\": \"Manutenção avulsa\"}"
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chamadoId").doesNotExist())
                .andExpect(jsonPath("$.unidadeAtendimentoId").value(fixture.unidadeA1Id()));
    }

    @Test
    @Transactional
    void devePermitirCriarOrdemServicoComoCto() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        post(BASE_URL, fixture.contratoAId())
                                .header("Authorization", "Bearer " + fixture.tokenCto())
                                .contentType("application/json")
                                .content("{\"chamadoId\": " + fixture.chamadoAId() + "}")
                )
                .andExpect(status().isCreated());
    }

    @Test
    @Transactional
    void deveBloquearCriarOrdemServicoComoTecnico() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        post(BASE_URL, fixture.contratoAId())
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                                .contentType("application/json")
                                .content("{\"chamadoId\": " + fixture.chamadoAId() + "}")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void deveRejeitarCriarOrdemServicoAvulsaSemUnidade() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        post(BASE_URL, fixture.contratoAId())
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                                .contentType("application/json")
                                .content("{}")
                )
                .andExpect(status().isBadRequest());
    }

    // ================= Listagem =================

    @Test
    @Transactional
    void devePermitirListarOrdemServicoComoAdmin() throws Exception {
        Fixture fixture = criarFixture();
        persistirOrdemServicoAvulsa(
                101,
                fixture.unidadeA1(),
                null,
                LocalDate.of(2026, 8, 20)
        );

        mockMvc.perform(
                        get(BASE_URL, fixture.contratoAId())
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void devePermitirListarOrdemServicoComoTecnicoMesmoContrato() throws Exception {
        Fixture fixture = criarFixture();
        persistirOrdemServicoAvulsa(
                102,
                fixture.unidadeA1(),
                null,
                LocalDate.of(2026, 8, 20)
        );

        mockMvc.perform(
                        get(BASE_URL, fixture.contratoAId())
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearListarOrdemServicoComoTecnicoDeOutroContrato() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(BASE_URL, fixture.contratoAId())
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoB())
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void deveFiltrarListagemPorData() throws Exception {
        Fixture fixture = criarFixture();
        persistirOrdemServicoAvulsa(
                103,
                fixture.unidadeA1(),
                null,
                LocalDate.of(2026, 8, 20)
        );
        persistirOrdemServicoAvulsa(
                104,
                fixture.unidadeA1(),
                null,
                LocalDate.of(2026, 8, 21)
        );

        mockMvc.perform(
                        get(BASE_URL, fixture.contratoAId())
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                                .param("data", "2026-08-20")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numeroOrdemServico").value(103));
    }

    @Test
    @Transactional
    void deveFiltrarListagemPorPeriodo() throws Exception {
        Fixture fixture = criarFixture();
        persistirOrdemServicoAvulsa(
                105,
                fixture.unidadeA1(),
                null,
                LocalDate.of(2026, 8, 10)
        );
        persistirOrdemServicoAvulsa(
                106,
                fixture.unidadeA1(),
                null,
                LocalDate.of(2026, 8, 20)
        );
        persistirOrdemServicoAvulsa(
                107,
                fixture.unidadeA1(),
                null,
                LocalDate.of(2026, 8, 30)
        );

        mockMvc.perform(
                        get(BASE_URL, fixture.contratoAId())
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                                .param("dataInicio", "2026-08-15")
                                .param("dataFim", "2026-08-25")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numeroOrdemServico").value(106));
    }

    @Test
    @Transactional
    void deveFiltrarListagemPorTecnico() throws Exception {
        Fixture fixture = criarFixture();
        persistirOrdemServicoAvulsa(
                108,
                fixture.unidadeA1(),
                fixture.tecnicoA(),
                LocalDate.of(2026, 8, 20)
        );
        persistirOrdemServicoAvulsa(
                109,
                fixture.unidadeA1(),
                fixture.tecnicoInternoA(),
                LocalDate.of(2026, 8, 20)
        );

        mockMvc.perform(
                        get(BASE_URL, fixture.contratoAId())
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                                .param("tecnicoId", String.valueOf(fixture.tecnicoA().getId()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numeroOrdemServico").value(108));
    }

    @Test
    @Transactional
    void deveFiltrarListagemComMeusComoTecnico() throws Exception {
        Fixture fixture = criarFixture();
        persistirOrdemServicoAvulsa(
                110,
                fixture.unidadeA1(),
                fixture.tecnicoA(),
                LocalDate.of(2026, 8, 20)
        );
        persistirOrdemServicoAvulsa(
                111,
                fixture.unidadeA1(),
                fixture.tecnicoInternoA(),
                LocalDate.of(2026, 8, 20)
        );

        mockMvc.perform(
                        get(BASE_URL, fixture.contratoAId())
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                                .param("meus", "true")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numeroOrdemServico").value(110));
    }

    // ================= Busca por id =================

    @Test
    @Transactional
    void devePermitirBuscarOrdemServicoPorIdComoAdmin() throws Exception {
        Fixture fixture = criarFixture();
        OrdemServico os = persistirOrdemServicoAvulsa(
                112,
                fixture.unidadeA1(),
                null,
                LocalDate.of(2026, 8, 20)
        );

        mockMvc.perform(
                        get(BASE_URL + "/{ordemServicoId}", fixture.contratoAId(), os.getId())
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(os.getId()));
    }

    @Test
    @Transactional
    void deveBloquearBuscarOrdemServicoPorIdComoTecnicoDeOutroContrato() throws Exception {
        Fixture fixture = criarFixture();
        OrdemServico os = persistirOrdemServicoAvulsa(
                113,
                fixture.unidadeA1(),
                null,
                LocalDate.of(2026, 8, 20)
        );

        mockMvc.perform(
                        get(BASE_URL + "/{ordemServicoId}", fixture.contratoAId(), os.getId())
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoB())
                )
                .andExpect(status().isForbidden());
    }

    // ================= Atualização =================

    @Test
    @Transactional
    void devePermitirAtualizarOrdemServicoComoAdmin() throws Exception {
        Fixture fixture = criarFixture();
        OrdemServico os = persistirOrdemServicoAvulsa(
                114,
                fixture.unidadeA1(),
                null,
                LocalDate.of(2026, 8, 20)
        );

        mockMvc.perform(
                        put(BASE_URL + "/{ordemServicoId}", fixture.contratoAId(), os.getId())
                                .header("Authorization", "Bearer " + fixture.tokenAdmin())
                                .contentType("application/json")
                                .content(
                                        "{\"unidadeAtendimentoId\": "
                                                + fixture.unidadeA1Id()
                                                + ", \"descricao\": \"Atualizada\"}"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Atualizada"));
    }

    @Test
    @Transactional
    void deveBloquearAtualizarOrdemServicoComoTecnico() throws Exception {
        Fixture fixture = criarFixture();
        OrdemServico os = persistirOrdemServicoAvulsa(
                115,
                fixture.unidadeA1(),
                null,
                LocalDate.of(2026, 8, 20)
        );

        mockMvc.perform(
                        put(BASE_URL + "/{ordemServicoId}", fixture.contratoAId(), os.getId())
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                                .contentType("application/json")
                                .content("{\"unidadeAtendimentoId\": " + fixture.unidadeA1Id() + "}")
                )
                .andExpect(status().isForbidden());
    }

    // ================= Check-in / Check-out =================

    @Test
    @Transactional
    void devePermitirCheckInDoProprioTecnicoAtribuido() throws Exception {
        Fixture fixture = criarFixture();
        OrdemServico os = persistirOrdemServicoAvulsa(
                116,
                fixture.unidadeA1(),
                fixture.tecnicoA(),
                LocalDate.of(2026, 8, 20)
        );

        mockMvc.perform(
                        post(
                                BASE_URL + "/{ordemServicoId}/check-in",
                                fixture.contratoAId(),
                                os.getId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                                .contentType("application/json")
                                .content("{}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataCheckIn").exists());
    }

    @Test
    @Transactional
    void deveBloquearCheckInDeTecnicoNaoAtribuido() throws Exception {
        Fixture fixture = criarFixture();
        OrdemServico os = persistirOrdemServicoAvulsa(
                117,
                fixture.unidadeA1(),
                fixture.tecnicoA(),
                LocalDate.of(2026, 8, 20)
        );

        mockMvc.perform(
                        post(
                                BASE_URL + "/{ordemServicoId}/check-in",
                                fixture.contratoAId(),
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
    void devePermitirCheckOutDoProprioTecnicoAtribuido() throws Exception {
        Fixture fixture = criarFixture();
        OrdemServico os = persistirOrdemServicoAvulsa(
                118,
                fixture.unidadeA1(),
                fixture.tecnicoA(),
                LocalDate.of(2026, 8, 20)
        );
        os.setDataCheckIn(LocalDateTime.of(2026, 8, 20, 9, 0));
        os.setDataAtribuicaoTecnico(LocalDateTime.of(2026, 8, 20, 8, 30));

        mockMvc.perform(
                        post(
                                BASE_URL + "/{ordemServicoId}/check-out",
                                fixture.contratoAId(),
                                os.getId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataCheckOut").exists());
    }

    @Test
    @Transactional
    void devePermitirCheckInEmOrdemServicoAvulsaSemChamado() throws Exception {
        Fixture fixture = criarFixture();
        OrdemServico os = persistirOrdemServicoAvulsa(
                119,
                fixture.unidadeA1(),
                fixture.tecnicoA(),
                LocalDate.of(2026, 8, 20)
        );

        mockMvc.perform(
                        post(
                                BASE_URL + "/{ordemServicoId}/check-in",
                                fixture.contratoAId(),
                                os.getId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                                .contentType("application/json")
                                .content("{}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chamadoId").doesNotExist());
    }

    // ================= Sugestões de técnicos =================

    @Test
    @Transactional
    void devePermitirListarSugestoesTecnicosComoAdmin() throws Exception {
        Fixture fixture = criarFixture();
        OrdemServico os = persistirOrdemServicoAvulsa(
                120,
                fixture.unidadeA1(),
                null,
                LocalDate.of(2026, 8, 20)
        );

        mockMvc.perform(
                        get(
                                BASE_URL + "/{ordemServicoId}/sugestoes-tecnicos",
                                fixture.contratoAId(),
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
        OrdemServico os = persistirOrdemServicoAvulsa(
                121,
                fixture.unidadeA1(),
                null,
                LocalDate.of(2026, 8, 20)
        );

        mockMvc.perform(
                        get(
                                BASE_URL + "/{ordemServicoId}/sugestoes-tecnicos",
                                fixture.contratoAId(),
                                os.getId()
                        )
                                .header("Authorization", "Bearer " + fixture.tokenTecnicoA())
                )
                .andExpect(status().isForbidden());
    }

    // ================= Fixture =================

    private record Fixture(
            Long contratoAId,
            Unidade unidadeA1,
            Long unidadeA1Id,
            Chamado chamadoA,
            Long chamadoAId,
            Tecnico tecnicoA,
            Tecnico tecnicoInternoA,
            String tokenAdmin,
            String tokenCto,
            String tokenTecnicoA,
            String tokenTecnicoInternoA,
            String tokenTecnicoB
    ) {
    }

    private Fixture criarFixture() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();

        BaseOperacional baseA = persistirBaseOperacional(contratoA, -23.55, -46.63);
        BaseOperacional baseB = persistirBaseOperacional(contratoB, -22.90, -43.20);

        Unidade unidadeA1 = persistirUnidade(contratoA, -23.50, -46.60);
        Chamado chamadoA = persistirChamado("CH-HTTP-OSC-001", unidadeA1);

        Usuario admin = persistirUsuario(
                "Admin Ator Canonico",
                "admin.ator.osc@teste.local",
                PerfilUsuario.ADMIN
        );
        Usuario cto = persistirUsuario(
                "Cto Ator Canonico",
                "cto.ator.osc@teste.local",
                PerfilUsuario.CTO
        );

        Usuario usuarioTecnicoA = persistirUsuario(
                "Tecnico Contrato A Canonico",
                "tecnico.a.osc@teste.local",
                PerfilUsuario.TECNICO
        );
        Tecnico tecnicoA = persistirTecnico(usuarioTecnicoA, baseA);

        Usuario usuarioTecnicoInternoA = persistirUsuario(
                "Tecnico Interno Contrato A Canonico",
                "tecnico.interno.a.osc@teste.local",
                PerfilUsuario.TECNICO_INTERNO
        );
        Tecnico tecnicoInternoA = persistirTecnico(usuarioTecnicoInternoA, baseA);

        Usuario usuarioTecnicoB = persistirUsuario(
                "Tecnico Contrato B Canonico",
                "tecnico.b.osc@teste.local",
                PerfilUsuario.TECNICO
        );
        persistirTecnico(usuarioTecnicoB, baseB);

        return new Fixture(
                contratoA.getId(),
                unidadeA1,
                unidadeA1.getId(),
                chamadoA,
                chamadoA.getId(),
                tecnicoA,
                tecnicoInternoA,
                tokenService.gerarToken(admin),
                tokenService.gerarToken(cto),
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

    private long proximoNumeroChamadoInterno = 100_000;

    private Chamado persistirChamado(String numeroChamado, Unidade unidade) {
        Chamado chamado = new Chamado();
        chamado.setNumeroChamado(numeroChamado);
        chamado.setLinkChamadoOsti("https://teste.local/chamado/" + numeroChamado);
        chamado.setUnidade(unidade);
        chamado.setContrato(unidade.getContrato());
        chamado.setNumeroChamadoInterno(proximoNumeroChamadoInterno++);
        chamado.setTipo(TipoChamado.INCIDENTE);
        chamado.setCategoria(CategoriaChamado.OUTROS);
        chamado.setPrioridade(PrioridadeChamado.MEDIA);
        chamado.setStatus(StatusChamado.ABERTO);
        chamado.setDescricao("Chamado para teste de integração HTTP canônico");
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

    private Tecnico persistirTecnico(Usuario usuario, BaseOperacional baseOperacional) {
        Tecnico tecnico = new Tecnico();
        tecnico.setUsuario(usuario);
        tecnico.setBaseOperacional(baseOperacional);
        tecnico.setAtivo(true);
        entityManager.persist(tecnico);
        return tecnico;
    }

    private long proximoNumeroOrdemServico = 200_000;

    private OrdemServico persistirOrdemServicoAvulsa(
            long numeroOrdemServico,
            Unidade unidadeAtendimento,
            Tecnico tecnico,
            LocalDate data
    ) {
        OrdemServico ordemServico = new OrdemServico();
        ordemServico.setNumeroOrdemServico(numeroOrdemServico);
        ordemServico.setUnidadeAtendimento(unidadeAtendimento);
        ordemServico.setTecnico(tecnico);
        ordemServico.setData(data);
        entityManager.persist(ordemServico);
        return ordemServico;
    }
}
