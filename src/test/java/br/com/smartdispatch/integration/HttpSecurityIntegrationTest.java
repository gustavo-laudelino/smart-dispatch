package br.com.smartdispatch.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import br.com.smartdispatch.enums.PerfilUsuario;
import br.com.smartdispatch.model.Usuario;
import br.com.smartdispatch.repository.UsuarioRepository;
import br.com.smartdispatch.service.TokenService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HttpSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenService tokenService;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void deveBloquearRotaProtegidaSemToken() throws Exception {

        mockMvc.perform(
                        get("/usuarios")
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void devePermitirAcessoAoLoginSemToken() throws Exception {

        mockMvc.perform(
                        post("/auth/login")
                                .contentType("application/json")
                                .content("{}")
                )
                .andExpect(
                        status().isBadRequest()
                );
    }

    @Test
    void deveBloquearTokenInvalido() throws Exception {

        mockMvc.perform(
                        get("/usuarios")
                                .header(
                                        "Authorization",
                                        "Bearer token-invalido"
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    @Transactional
    void devePermitirAcessoComTokenValidoDeAdmin() throws Exception {

        Usuario usuario = new Usuario();
        usuario.setNome("Admin Teste");
        usuario.setEmail("admin.http@teste.local");
        usuario.setPerfil(PerfilUsuario.ADMIN);
        usuario.setSenha("senha-teste");
        usuario.setAtivo(true);

        usuarioRepository.save(usuario);

        String token = tokenService.gerarToken(usuario);

        mockMvc.perform(
                        get("/usuarios")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    @Transactional
    void devePermitirAcessoComTokenValidoDeCto() throws Exception {

        Usuario usuario = persistirUsuario(
                "Cto Teste",
                "cto.http@teste.local",
                PerfilUsuario.CTO
        );

        String token = tokenService.gerarToken(usuario);

        mockMvc.perform(
                        get("/usuarios")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    @Transactional
    void deveBloquearAcessoComTokenValidoDeTecnico() throws Exception {

        Usuario usuario = persistirUsuario(
                "Tecnico Teste",
                "tecnico.http@teste.local",
                PerfilUsuario.TECNICO
        );

        String token = tokenService.gerarToken(usuario);

        mockMvc.perform(
                        get("/usuarios")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    @Transactional
    void deveUsarPerfilAtualDoBancoAoInvesDoPerfilDoToken() throws Exception {

        Usuario usuario = persistirUsuario(
                "Admin Rebaixado Teste",
                "admin.rebaixado.http@teste.local",
                PerfilUsuario.ADMIN
        );

        String token = tokenService.gerarToken(usuario);

        usuario.setPerfil(PerfilUsuario.TECNICO);
        usuarioRepository.save(usuario);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(
                        get("/usuarios")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
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
        return usuarioRepository.save(usuario);
    }
}