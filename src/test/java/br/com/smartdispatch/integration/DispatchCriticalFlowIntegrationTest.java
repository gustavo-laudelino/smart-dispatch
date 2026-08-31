package br.com.smartdispatch.integration;

import br.com.smartdispatch.enums.PerfilUsuario;
import br.com.smartdispatch.model.Chamado;
import br.com.smartdispatch.model.OrdemServico;
import br.com.smartdispatch.model.Tecnico;
import br.com.smartdispatch.model.Usuario;
import br.com.smartdispatch.repository.ChamadoRepository;
import br.com.smartdispatch.repository.OrdemServicoRepository;
import br.com.smartdispatch.repository.TecnicoRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DispatchCriticalFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private OrdemServicoRepository ordemServicoRepository;

    @Autowired
    private TecnicoRepository tecnicoRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String SENHA_INICIAL_APLICACAO = "cto";

    @Test
    @Transactional
    void jornada1_cicloCompletoDeDespacho() throws Exception {
        Contexto contexto = bootstrapContratoComTecnico("J1");

        Long chamadoId = criarChamado(
                contexto.tokenAdmin(),
                contexto.contratoId(),
                contexto.unidadeId(),
                "CH-J1-001"
        );

        Chamado chamadoRecemCriado = chamadoRepository
                .findByIdAndUnidadeContratoId(chamadoId, contexto.contratoId())
                .orElseThrow();
        assertThat(chamadoRecemCriado.getStatus().name()).isEqualTo("ABERTO");

        Long ordemServicoId = criarOrdemServico(
                contexto.tokenAdmin(),
                contexto.contratoId(),
                chamadoId,
                "OS-J1-001",
                contexto.tecnicoId()
        );

        entityManager.flush();
        entityManager.clear();

        Chamado chamadoAposAtribuicao = chamadoRepository
                .findByIdAndUnidadeContratoId(chamadoId, contexto.contratoId())
                .orElseThrow();
        assertThat(chamadoAposAtribuicao.getStatus().name())
                .isEqualTo("ATRIBUIDO");

        mockMvc.perform(
                        post(
                                "/contratos/{contratoId}/chamados/{chamadoId}/ordens-servico/{ordemServicoId}/check-in",
                                contexto.contratoId(),
                                chamadoId,
                                ordemServicoId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + contexto.tokenTecnico()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        Chamado chamadoEmAtendimento = chamadoRepository
                .findByIdAndUnidadeContratoId(chamadoId, contexto.contratoId())
                .orElseThrow();
        assertThat(chamadoEmAtendimento.getStatus().name())
                .isEqualTo("EM_ATENDIMENTO");

        OrdemServico ordemServicoAposCheckIn = ordemServicoRepository
                .findById(ordemServicoId)
                .orElseThrow();
        assertThat(ordemServicoAposCheckIn.getDataCheckIn()).isNotNull();
        assertThat(ordemServicoAposCheckIn.getDataCheckOut()).isNull();

        mockMvc.perform(
                        post(
                                "/contratos/{contratoId}/chamados/{chamadoId}/ordens-servico/{ordemServicoId}/check-out",
                                contexto.contratoId(),
                                chamadoId,
                                ordemServicoId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + contexto.tokenTecnico()
                                )
                )
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        OrdemServico ordemServicoAposCheckOut = ordemServicoRepository
                .findById(ordemServicoId)
                .orElseThrow();
        assertThat(ordemServicoAposCheckOut.getDataCheckOut()).isNotNull();

        Chamado chamadoFinal = chamadoRepository
                .findByIdAndUnidadeContratoId(chamadoId, contexto.contratoId())
                .orElseThrow();
        assertThat(chamadoFinal.getStatus().name())
                .isEqualTo("AGUARDANDO_ANALISE");

        mockMvc.perform(
                        get(
                                "/contratos/{contratoId}/chamados/{chamadoId}/historico",
                                contexto.contratoId(),
                                chamadoId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + contexto.tokenAdmin()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[*].tipoEvento", hasItem("CHAMADO_CRIADO"))
                )
                .andExpect(
                        jsonPath(
                                "$[*].tipoEvento",
                                hasItem("ORDEM_SERVICO_CRIADA")
                        )
                )
                .andExpect(
                        jsonPath(
                                "$[*].tipoEvento",
                                hasItem("TECNICO_ATRIBUIDO")
                        )
                )
                .andExpect(
                        jsonPath(
                                "$[*].tipoEvento",
                                hasItem("ATENDIMENTO_INICIADO")
                        )
                )
                .andExpect(
                        jsonPath(
                                "$[*].tipoEvento",
                                hasItem("ATENDIMENTO_FINALIZADO")
                        )
                );
    }

    @Test
    @Transactional
    void jornada2_checkInConcorrenteEncerramentoAutomatico() throws Exception {
        Contexto contexto = bootstrapContratoComTecnico("J2");

        Long chamado1Id = criarChamado(
                contexto.tokenAdmin(),
                contexto.contratoId(),
                contexto.unidadeId(),
                "CH-J2-001"
        );
        Long ordemServico1Id = criarOrdemServico(
                contexto.tokenAdmin(),
                contexto.contratoId(),
                chamado1Id,
                "OS-J2-001",
                contexto.tecnicoId()
        );

        Long chamado2Id = criarChamado(
                contexto.tokenAdmin(),
                contexto.contratoId(),
                contexto.unidadeId(),
                "CH-J2-002"
        );
        Long ordemServico2Id = criarOrdemServico(
                contexto.tokenAdmin(),
                contexto.contratoId(),
                chamado2Id,
                "OS-J2-002",
                contexto.tecnicoId()
        );

        mockMvc.perform(
                        post(
                                "/contratos/{contratoId}/chamados/{chamadoId}/ordens-servico/{ordemServicoId}/check-in",
                                contexto.contratoId(),
                                chamado1Id,
                                ordemServico1Id
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + contexto.tokenTecnico()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        OrdemServico os1AposPrimeiroCheckIn = ordemServicoRepository
                .findById(ordemServico1Id)
                .orElseThrow();
        assertThat(os1AposPrimeiroCheckIn.getDataCheckIn()).isNotNull();
        assertThat(os1AposPrimeiroCheckIn.getDataCheckOut()).isNull();

        mockMvc.perform(
                        post(
                                "/contratos/{contratoId}/chamados/{chamadoId}/ordens-servico/{ordemServicoId}/check-in",
                                contexto.contratoId(),
                                chamado2Id,
                                ordemServico2Id
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + contexto.tokenTecnico()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isConflict());

        entityManager.flush();
        entityManager.clear();

        OrdemServico os1AposTentativaConflitante = ordemServicoRepository
                .findById(ordemServico1Id)
                .orElseThrow();
        assertThat(os1AposTentativaConflitante.getDataCheckOut()).isNull();

        OrdemServico os2AposTentativaConflitante = ordemServicoRepository
                .findById(ordemServico2Id)
                .orElseThrow();
        assertThat(os2AposTentativaConflitante.getDataCheckIn()).isNull();

        mockMvc.perform(
                        post(
                                "/contratos/{contratoId}/chamados/{chamadoId}/ordens-servico/{ordemServicoId}/check-in",
                                contexto.contratoId(),
                                chamado2Id,
                                ordemServico2Id
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + contexto.tokenTecnico()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"encerrarCheckInAnterior\":true}")
                )
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        OrdemServico os1Final = ordemServicoRepository
                .findById(ordemServico1Id)
                .orElseThrow();
        assertThat(os1Final.getDataCheckOut()).isNotNull();

        OrdemServico os2Final = ordemServicoRepository
                .findById(ordemServico2Id)
                .orElseThrow();
        assertThat(os2Final.getDataCheckIn()).isNotNull();
        assertThat(os2Final.getDataCheckOut()).isNull();

        Chamado chamado1Final = chamadoRepository
                .findByIdAndUnidadeContratoId(chamado1Id, contexto.contratoId())
                .orElseThrow();
        assertThat(chamado1Final.getStatus().name())
                .isEqualTo("AGUARDANDO_ANALISE");

        Chamado chamado2Final = chamadoRepository
                .findByIdAndUnidadeContratoId(chamado2Id, contexto.contratoId())
                .orElseThrow();
        assertThat(chamado2Final.getStatus().name())
                .isEqualTo("EM_ATENDIMENTO");

        mockMvc.perform(
                        get(
                                "/contratos/{contratoId}/chamados/{chamadoId}/historico",
                                contexto.contratoId(),
                                chamado1Id
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + contexto.tokenAdmin()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$[*].tipoEvento",
                                hasItem("ATENDIMENTO_FINALIZADO_AUTOMATICAMENTE")
                        )
                );
    }

    @Test
    @Transactional
    void jornada3_isolamentoOperacionalDePontaAPonta() throws Exception {
        Contexto contextoA = bootstrapContratoComTecnico("J3A");

        Long chamadoAId = criarChamado(
                contextoA.tokenAdmin(),
                contextoA.contratoId(),
                contextoA.unidadeId(),
                "CH-J3-A"
        );

        Long contratoBId = criarContrato(
                contextoA.tokenAdmin(),
                "Cidade J3B"
        );
        Long unidadeBId = criarUnidade(
                contextoA.tokenAdmin(),
                contratoBId,
                "Unidade J3B"
        );
        criarChamado(
                contextoA.tokenAdmin(),
                contratoBId,
                unidadeBId,
                "CH-J3-B"
        );

        mockMvc.perform(
                        get("/contratos")
                                .header(
                                        "Authorization",
                                        "Bearer " + contextoA.tokenTecnico()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(
                        jsonPath("$[0].id")
                                .value(contextoA.contratoId())
                );

        mockMvc.perform(
                        get("/chamados")
                                .header(
                                        "Authorization",
                                        "Bearer " + contextoA.tokenTecnico()
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(
                        jsonPath("$.content[0].numeroChamado")
                                .value("CH-J3-A")
                );

        mockMvc.perform(
                        get(
                                "/contratos/{contratoId}/chamados",
                                contratoBId
                        )
                                .header(
                                        "Authorization",
                                        "Bearer " + contextoA.tokenTecnico()
                                )
                )
                .andExpect(status().isForbidden());
    }

    private record Contexto(
            String tokenAdmin,
            String tokenTecnico,
            Long contratoId,
            Long unidadeId,
            Long tecnicoId
    ) {
    }

    private Contexto bootstrapContratoComTecnico(String sufixo)
            throws Exception {
        Usuario admin = persistirUsuarioBootstrap(
                "Admin " + sufixo,
                "admin." + sufixo.toLowerCase() + "@teste.local",
                "senha-admin-" + sufixo
        );

        String tokenAdmin = loginReal(
                admin.getEmail(),
                "senha-admin-" + sufixo
        );

        Long contratoId = criarContrato(
                tokenAdmin,
                "Cidade " + sufixo
        );
        Long baseId = criarBase(
                tokenAdmin,
                contratoId,
                "Base " + sufixo
        );
        Long unidadeId = criarUnidade(
                tokenAdmin,
                contratoId,
                "Unidade " + sufixo
        );

        String emailTecnico = "tecnico." + sufixo.toLowerCase() + "@teste.local";

        Long usuarioIdTecnico = criarUsuarioTecnico(
                tokenAdmin,
                "Tecnico " + sufixo,
                emailTecnico,
                contratoId,
                baseId
        );

        String tokenTecnico = loginReal(
                emailTecnico,
                SENHA_INICIAL_APLICACAO
        );

        Optional<Tecnico> tecnico =
                tecnicoRepository.findByUsuarioId(usuarioIdTecnico);

        assertThat(tecnico).isPresent();

        return new Contexto(
                tokenAdmin,
                tokenTecnico,
                contratoId,
                unidadeId,
                tecnico.get().getId()
        );
    }

    private String loginReal(String email, String senha) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"" + email + "\","
                                                + "\"senha\":\"" + senha + "\"}"
                                )
                )
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.token"
        );
    }

    private Long criarContrato(String tokenAdmin, String cidade)
            throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/contratos")
                                .header("Authorization", "Bearer " + tokenAdmin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cidade\":\"" + cidade + "\"}")
                )
                .andExpect(status().isOk())
                .andReturn();

        Number id = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.id"
        );
        return id.longValue();
    }

    private Long criarBase(String tokenAdmin, Long contratoId, String nome)
            throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/contratos/{contratoId}/bases", contratoId)
                                .header("Authorization", "Bearer " + tokenAdmin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nome\":\"" + nome + "\"}")
                )
                .andExpect(status().isOk())
                .andReturn();

        Number id = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.id"
        );
        return id.longValue();
    }

    private Long criarUnidade(String tokenAdmin, Long contratoId, String nome)
            throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/contratos/{contratoId}/unidades", contratoId)
                                .header("Authorization", "Bearer " + tokenAdmin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nome\":\"" + nome + "\"}")
                )
                .andExpect(status().isOk())
                .andReturn();

        Number id = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.id"
        );
        return id.longValue();
    }

    private Long criarUsuarioTecnico(
            String tokenAdmin,
            String nome,
            String email,
            Long contratoId,
            Long baseId
    ) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/usuarios")
                                .header("Authorization", "Bearer " + tokenAdmin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"nome\":\"" + nome + "\","
                                                + "\"email\":\"" + email + "\","
                                                + "\"perfil\":\"TECNICO\","
                                                + "\"contratoId\":" + contratoId + ","
                                                + "\"baseOperacionalId\":" + baseId
                                                + "}"
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        Number id = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.id"
        );
        return id.longValue();
    }

    private Long criarChamado(
            String tokenAdmin,
            Long contratoId,
            Long unidadeId,
            String numeroChamado
    ) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/contratos/{contratoId}/chamados", contratoId)
                                .header("Authorization", "Bearer " + tokenAdmin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"numeroChamado\":\"" + numeroChamado + "\","
                                                + "\"linkChamadoOsti\":\"https://teste.local/chamado/"
                                                + numeroChamado + "\","
                                                + "\"unidadeId\":" + unidadeId + ","
                                                + "\"tipo\":\"INCIDENTE\","
                                                + "\"categoria\":\"OUTROS\","
                                                + "\"prioridade\":\"MEDIA\","
                                                + "\"descricao\":\"Chamado da jornada critica\"}"
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        Number id = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.id"
        );
        return id.longValue();
    }

    private Long criarOrdemServico(
            String tokenAdmin,
            Long contratoId,
            Long chamadoId,
            String numeroOrdemServico,
            Long tecnicoId
    ) throws Exception {
        MvcResult result = mockMvc.perform(
                        post(
                                "/contratos/{contratoId}/chamados/{chamadoId}/ordens-servico",
                                contratoId,
                                chamadoId
                        )
                                .header("Authorization", "Bearer " + tokenAdmin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"numeroOrdemServico\":\""
                                                + numeroOrdemServico + "\","
                                                + "\"tecnicoId\":" + tecnicoId + "}"
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        Number id = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.id"
        );
        return id.longValue();
    }

    private Usuario persistirUsuarioBootstrap(
            String nome,
            String email,
            String senhaPlana
    ) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setPerfil(PerfilUsuario.ADMIN);
        usuario.setSenha(passwordEncoder.encode(senhaPlana));
        usuario.setAtivo(true);
        entityManager.persist(usuario);
        return usuario;
    }
}
