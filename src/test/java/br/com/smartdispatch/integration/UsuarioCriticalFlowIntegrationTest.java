package br.com.smartdispatch.integration;

import br.com.smartdispatch.enums.PerfilUsuario;
import br.com.smartdispatch.model.Tecnico;
import br.com.smartdispatch.model.Usuario;
import br.com.smartdispatch.repository.TecnicoRepository;
import br.com.smartdispatch.repository.UsuarioRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsuarioCriticalFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TecnicoRepository tecnicoRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String SENHA_INICIAL_APLICACAO = "cto";

    @Test
    @Transactional
    void jornada4_alteracaoDePerfilEJwtAntigo() throws Exception {
        String tokenAdmin = bootstrapEloginAdmin("J4");

        Long contratoId = criarContrato(tokenAdmin, "Cidade J4");
        Long baseId = criarBase(tokenAdmin, contratoId, "Base J4");

        String emailTecnico = "tecnico.j4@teste.local";
        Long usuarioId = criarUsuarioTecnico(
                tokenAdmin,
                "Tecnico J4",
                emailTecnico,
                contratoId,
                baseId
        );

        String tokenTecnicoAntigo = loginReal(
                emailTecnico,
                SENHA_INICIAL_APLICACAO
        );

        mockMvc.perform(
                        get("/usuarios")
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenTecnicoAntigo
                                )
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        put("/usuarios/{usuarioId}", usuarioId)
                                .header("Authorization", "Bearer " + tokenAdmin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"nome\":\"Tecnico J4\","
                                                + "\"email\":\"" + emailTecnico + "\","
                                                + "\"perfil\":\"CTO\"}"
                                )
                )
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        Usuario usuarioAtualizado = usuarioRepository
                .findById(usuarioId)
                .orElseThrow();
        assertThat(usuarioAtualizado.getPerfil())
                .isEqualTo(PerfilUsuario.CTO);

        Tecnico tecnicoHistorico = tecnicoRepository
                .findByUsuarioId(usuarioId)
                .orElseThrow();
        assertThat(tecnicoHistorico.isAtivo()).isFalse();

        mockMvc.perform(
                        get("/usuarios")
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenTecnicoAntigo
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void jornada5_cicloCompletoDeSenha() throws Exception {
        String tokenAdmin = bootstrapEloginAdmin("J5");

        String emailGestor = "gestor.j5@teste.local";
        Long usuarioId = criarUsuarioGestor(
                tokenAdmin,
                "Gestor J5",
                emailGestor
        );

        String tokenGestor = loginReal(
                emailGestor,
                SENHA_INICIAL_APLICACAO
        );

        mockMvc.perform(
                        patch("/usuarios/me/senha")
                                .header("Authorization", "Bearer " + tokenGestor)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"senhaAtual\":\"" + SENHA_INICIAL_APLICACAO + "\","
                                                + "\"novaSenha\":\"nova-senha-j5\","
                                                + "\"confirmacaoSenha\":\"nova-senha-j5\"}"
                                )
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"" + emailGestor + "\","
                                                + "\"senha\":\"" + SENHA_INICIAL_APLICACAO + "\"}"
                                )
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"" + emailGestor + "\","
                                                + "\"senha\":\"nova-senha-j5\"}"
                                )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/usuarios/{usuarioId}/reset-senha", usuarioId)
                                .header("Authorization", "Bearer " + tokenAdmin)
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"" + emailGestor + "\","
                                                + "\"senha\":\"nova-senha-j5\"}"
                                )
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"" + emailGestor + "\","
                                                + "\"senha\":\"" + SENHA_INICIAL_APLICACAO + "\"}"
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void jornada6_gestorTecnicoGestor() throws Exception {
        String tokenAdmin = bootstrapEloginAdmin("J6");

        Long contratoId = criarContrato(tokenAdmin, "Cidade J6");
        Long baseId = criarBase(tokenAdmin, contratoId, "Base J6");

        String emailUsuario = "usuario.j6@teste.local";
        Long usuarioId = criarUsuarioGestor(
                tokenAdmin,
                "Usuario J6",
                emailUsuario
        );

        assertThat(tecnicoRepository.findByUsuarioId(usuarioId)).isEmpty();

        mockMvc.perform(
                        put("/usuarios/{usuarioId}", usuarioId)
                                .header("Authorization", "Bearer " + tokenAdmin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"nome\":\"Usuario J6\","
                                                + "\"email\":\"" + emailUsuario + "\","
                                                + "\"perfil\":\"TECNICO\","
                                                + "\"contratoId\":" + contratoId + ","
                                                + "\"baseOperacionalId\":" + baseId
                                                + "}"
                                )
                )
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        Usuario usuarioComoTecnico = usuarioRepository
                .findById(usuarioId)
                .orElseThrow();
        assertThat(usuarioComoTecnico.getPerfil())
                .isEqualTo(PerfilUsuario.TECNICO);

        Tecnico tecnicoCriado = tecnicoRepository
                .findByUsuarioId(usuarioId)
                .orElseThrow();
        assertThat(tecnicoCriado.getBaseOperacional().getId())
                .isEqualTo(baseId);
        assertThat(tecnicoCriado.isAtivo()).isTrue();

        Long tecnicoIdOriginal = tecnicoCriado.getId();

        String tokenTecnico = loginReal(
                emailUsuario,
                SENHA_INICIAL_APLICACAO
        );

        mockMvc.perform(
                        get("/contratos/{id}", contratoId)
                                .header("Authorization", "Bearer " + tokenTecnico)
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        put("/usuarios/{usuarioId}", usuarioId)
                                .header("Authorization", "Bearer " + tokenAdmin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"nome\":\"Usuario J6\","
                                                + "\"email\":\"" + emailUsuario + "\","
                                                + "\"perfil\":\"CTO\"}"
                                )
                )
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        Usuario usuarioComoCto = usuarioRepository
                .findById(usuarioId)
                .orElseThrow();
        assertThat(usuarioComoCto.getPerfil())
                .isEqualTo(PerfilUsuario.CTO);

        Tecnico tecnicoAposRebaixamento = tecnicoRepository
                .findByUsuarioId(usuarioId)
                .orElseThrow();
        assertThat(tecnicoAposRebaixamento.getId())
                .isEqualTo(tecnicoIdOriginal);
        assertThat(tecnicoAposRebaixamento.isAtivo()).isFalse();

        mockMvc.perform(
                        put("/usuarios/{usuarioId}", usuarioId)
                                .header("Authorization", "Bearer " + tokenAdmin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"nome\":\"Usuario J6\","
                                                + "\"email\":\"" + emailUsuario + "\","
                                                + "\"perfil\":\"TECNICO\","
                                                + "\"contratoId\":" + contratoId + ","
                                                + "\"baseOperacionalId\":" + baseId
                                                + "}"
                                )
                )
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        Optional<Tecnico> tecnicoReativado =
                tecnicoRepository.findByUsuarioId(usuarioId);

        assertThat(tecnicoReativado).isPresent();
        assertThat(tecnicoReativado.get().getId())
                .isEqualTo(tecnicoIdOriginal);
        assertThat(tecnicoReativado.get().isAtivo()).isTrue();
        assertThat(tecnicoReativado.get().getBaseOperacional().getId())
                .isEqualTo(baseId);
    }

    private String bootstrapEloginAdmin(String sufixo) throws Exception {
        Usuario admin = new Usuario();
        admin.setNome("Admin " + sufixo);
        admin.setEmail("admin." + sufixo.toLowerCase() + "@teste.local");
        admin.setPerfil(PerfilUsuario.ADMIN);
        admin.setSenha(passwordEncoder.encode("senha-admin-" + sufixo));
        admin.setAtivo(true);
        entityManager.persist(admin);

        return loginReal(admin.getEmail(), "senha-admin-" + sufixo);
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

    private Long criarUsuarioGestor(
            String tokenAdmin,
            String nome,
            String email
    ) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/usuarios")
                                .header("Authorization", "Bearer " + tokenAdmin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"nome\":\"" + nome + "\","
                                                + "\"email\":\"" + email + "\","
                                                + "\"perfil\":\"CTO\"}"
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
}
