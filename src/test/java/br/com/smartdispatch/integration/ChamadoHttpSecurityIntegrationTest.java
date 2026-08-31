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
import br.com.smartdispatch.repository.ChamadoRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChamadoHttpSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ChamadoRepository chamadoRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @Transactional
    void devePermitirListarChamadosDoContratoComoAdmin() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(
                                "/contratos/{contratoId}/chamados",
                                fixture.contratoAId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + fixture.tokenAdmin()
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void devePermitirListarChamadosDoContratoComoCto() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(
                                "/contratos/{contratoId}/chamados",
                                fixture.contratoAId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + fixture.tokenCto()
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void devePermitirListarChamadosDoContratoComoTecnicoVinculado()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(
                                "/contratos/{contratoId}/chamados",
                                fixture.contratoAId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + fixture.tokenTecnicoA()
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearTecnicoAcessandoChamadosDeOutroContrato()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(
                                "/contratos/{contratoId}/chamados",
                                fixture.contratoBId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + fixture.tokenTecnicoA()
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirListarChamadosDoContratoComoTecnicoInternoVinculado()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(
                                "/contratos/{contratoId}/chamados",
                                fixture.contratoAId()
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
    void deveBloquearTecnicoInternoAcessandoChamadosDeOutroContrato()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(
                                "/contratos/{contratoId}/chamados",
                                fixture.contratoBId()
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
    void devePermitirGestorBuscarChamadoPorId() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        get(
                                "/contratos/{contratoId}/chamados/{chamadoId}",
                                fixture.contratoAId(),
                                fixture.chamadoAId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + fixture.tokenAdmin()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fixture.chamadoAId()))
                .andExpect(jsonPath("$.numeroChamado").value("CH-HTTP-001"));
    }

    @Test
    @Transactional
    void devePermitirGestorAtualizarChamadoExistente() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        put(
                                "/contratos/{contratoId}/chamados/{chamadoId}",
                                fixture.contratoAId(),
                                fixture.chamadoAId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + fixture.tokenAdmin()
                                )
                                .contentType("application/json")
                                .content(
                                        "{"
                                                + "\"numeroChamado\":\"CH-HTTP-001\","
                                                + "\"linkChamadoOsti\":\"https://teste.local/chamado/CH-HTTP-001\","
                                                + "\"unidadeId\":" + fixture.unidadeAId() + ","
                                                + "\"tipo\":\"INCIDENTE\","
                                                + "\"categoria\":\"OUTROS\","
                                                + "\"prioridade\":\"ALTA\","
                                                + "\"descricao\":\"Descrição atualizada via teste HTTP\""
                                                + "}"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prioridade").value("ALTA"))
                .andExpect(jsonPath("$.descricao").value("Descrição atualizada via teste HTTP"));

        entityManager.flush();
        entityManager.clear();

        Chamado chamadoPersistido = chamadoRepository
                .findById(fixture.chamadoAId())
                .orElseThrow();

        assertEquals(PrioridadeChamado.ALTA, chamadoPersistido.getPrioridade());
        assertEquals(
                "Descrição atualizada via teste HTTP",
                chamadoPersistido.getDescricao()
        );
    }

    @Test
    @Transactional
    void deveBloquearTecnicoTentandoFinalizarChamadoDoProprioContrato()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        patch(
                                "/contratos/{contratoId}/chamados/{chamadoId}/status",
                                fixture.contratoAId(),
                                fixture.chamadoAId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + fixture.tokenTecnicoA()
                                )
                                .contentType("application/json")
                                .content("{\"status\":\"FINALIZADO\"}")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirTecnicoAlterarStatusParaTransicaoValida()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        patch(
                                "/contratos/{contratoId}/chamados/{chamadoId}/status",
                                fixture.contratoAId(),
                                fixture.chamadoAId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + fixture.tokenTecnicoA()
                                )
                                .contentType("application/json")
                                .content(
                                        "{\"status\":\"AGUARDANDO_ANALISE\"}"
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void devePermitirAdminFinalizarChamado() throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        patch(
                                "/contratos/{contratoId}/chamados/{chamadoId}/status",
                                fixture.contratoAId(),
                                fixture.chamadoAId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + fixture.tokenAdmin()
                                )
                                .contentType("application/json")
                                .content("{\"status\":\"FINALIZADO\"}")
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearTecnicoDeOutroContratoAlterandoStatus()
            throws Exception {
        Fixture fixture = criarFixture();

        mockMvc.perform(
                        patch(
                                "/contratos/{contratoId}/chamados/{chamadoId}/status",
                                fixture.contratoAId(),
                                fixture.chamadoAId()
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + fixture.tokenTecnicoB()
                                )
                                .contentType("application/json")
                                .content(
                                        "{\"status\":\"AGUARDANDO_ANALISE\"}"
                                )
                )
                .andExpect(status().isForbidden());
    }

    private record Fixture(
            Long contratoAId,
            Long contratoBId,
            Long chamadoAId,
            String tokenAdmin,
            String tokenCto,
            String tokenTecnicoA,
            String tokenTecnicoInternoA,
            String tokenTecnicoB,
            Long unidadeAId
    ) {
    }

    private Fixture criarFixture() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();

        BaseOperacional baseA = persistirBaseOperacional(contratoA);
        BaseOperacional baseB = persistirBaseOperacional(contratoB);

        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado(
                "CH-HTTP-001",
                unidadeA
        );

        Usuario admin = persistirUsuario(
                "Admin Ator",
                "admin.ator.chamado@teste.local",
                PerfilUsuario.ADMIN
        );
        Usuario cto = persistirUsuario(
                "Cto Ator",
                "cto.ator.chamado@teste.local",
                PerfilUsuario.CTO
        );

        Usuario usuarioTecnicoA = persistirUsuario(
                "Tecnico Contrato A",
                "tecnico.a.chamado@teste.local",
                PerfilUsuario.TECNICO
        );
        persistirTecnico(usuarioTecnicoA, baseA);

        Usuario usuarioTecnicoInternoA = persistirUsuario(
                "Tecnico Interno Contrato A",
                "tecnico.interno.a.chamado@teste.local",
                PerfilUsuario.TECNICO_INTERNO
        );
        persistirTecnico(usuarioTecnicoInternoA, baseA);

        Usuario usuarioTecnicoB = persistirUsuario(
                "Tecnico Contrato B",
                "tecnico.b.chamado@teste.local",
                PerfilUsuario.TECNICO
        );
        persistirTecnico(usuarioTecnicoB, baseB);

        return new Fixture(
                contratoA.getId(),
                contratoB.getId(),
                chamadoA.getId(),
                tokenService.gerarToken(admin),
                tokenService.gerarToken(cto),
                tokenService.gerarToken(usuarioTecnicoA),
                tokenService.gerarToken(usuarioTecnicoInternoA),
                tokenService.gerarToken(usuarioTecnicoB),
                unidadeA.getId()
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
        chamado.setDataAbertura(
                LocalDateTime.of(2026, 8, 23, 10, 0)
        );
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
