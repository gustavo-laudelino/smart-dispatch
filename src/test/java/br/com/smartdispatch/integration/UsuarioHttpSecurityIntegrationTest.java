package br.com.smartdispatch.integration;

import br.com.smartdispatch.enums.PerfilUsuario;
import br.com.smartdispatch.model.Usuario;
import br.com.smartdispatch.repository.UsuarioRepository;
import br.com.smartdispatch.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsuarioHttpSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    void devePermitirCriarUsuarioComoAdmin() throws Exception {
        Usuario admin = persistirUsuario(
                "Admin Ator",
                "admin.ator.001@teste.local",
                PerfilUsuario.ADMIN
        );
        String token = tokenService.gerarToken(admin);

        mockMvc.perform(
                        post("/usuarios")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(
                                        "{\"nome\":\"Novo Usuario\","
                                                + "\"email\":\"novo.usuario.001@teste.local\","
                                                + "\"perfil\":\"CTO\"}"
                                )
                )
                .andExpect(status().isCreated());
    }

    @Test
    @Transactional
    void deveBloquearCriarUsuarioComoCto() throws Exception {
        Usuario cto = persistirUsuario(
                "Cto Ator",
                "cto.ator.002@teste.local",
                PerfilUsuario.CTO
        );
        String token = tokenService.gerarToken(cto);

        mockMvc.perform(
                        post("/usuarios")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(
                                        "{\"nome\":\"Novo Usuario\","
                                                + "\"email\":\"novo.usuario.002@teste.local\","
                                                + "\"perfil\":\"CTO\"}"
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirBuscarUsuarioPorIdComoCto() throws Exception {
        Usuario alvo = persistirUsuario(
                "Usuario Alvo",
                "usuario.alvo.003@teste.local",
                PerfilUsuario.ADMIN
        );
        Usuario cto = persistirUsuario(
                "Cto Ator",
                "cto.ator.003@teste.local",
                PerfilUsuario.CTO
        );
        String token = tokenService.gerarToken(cto);

        mockMvc.perform(
                        get("/usuarios/{usuarioId}", alvo.getId())
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearBuscarUsuarioPorIdComoTecnico() throws Exception {
        Usuario alvo = persistirUsuario(
                "Usuario Alvo",
                "usuario.alvo.004@teste.local",
                PerfilUsuario.ADMIN
        );
        Usuario tecnico = persistirUsuario(
                "Tecnico Ator",
                "tecnico.ator.004@teste.local",
                PerfilUsuario.TECNICO
        );
        String token = tokenService.gerarToken(tecnico);

        mockMvc.perform(
                        get("/usuarios/{usuarioId}", alvo.getId())
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirAtualizarUsuarioComoAdmin() throws Exception {
        Usuario alvo = persistirUsuario(
                "Usuario Alvo",
                "usuario.alvo.005@teste.local",
                PerfilUsuario.ADMIN
        );
        Usuario admin = persistirUsuario(
                "Admin Ator",
                "admin.ator.005@teste.local",
                PerfilUsuario.ADMIN
        );
        String token = tokenService.gerarToken(admin);

        mockMvc.perform(
                        put("/usuarios/{usuarioId}", alvo.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(
                                        "{\"nome\":\"Usuario Alvo Atualizado\","
                                                + "\"email\":\"usuario.alvo.005@teste.local\","
                                                + "\"perfil\":\"ADMIN\"}"
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearAtualizarUsuarioComoCto() throws Exception {
        Usuario alvo = persistirUsuario(
                "Usuario Alvo",
                "usuario.alvo.006@teste.local",
                PerfilUsuario.ADMIN
        );
        Usuario cto = persistirUsuario(
                "Cto Ator",
                "cto.ator.006@teste.local",
                PerfilUsuario.CTO
        );
        String token = tokenService.gerarToken(cto);

        mockMvc.perform(
                        put("/usuarios/{usuarioId}", alvo.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(
                                        "{\"nome\":\"Usuario Alvo Atualizado\","
                                                + "\"email\":\"usuario.alvo.006@teste.local\","
                                                + "\"perfil\":\"ADMIN\"}"
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirAtualizarStatusUsuarioComoAdmin() throws Exception {
        Usuario alvo = persistirUsuario(
                "Usuario Alvo",
                "usuario.alvo.007@teste.local",
                PerfilUsuario.ADMIN
        );
        Usuario admin = persistirUsuario(
                "Admin Ator",
                "admin.ator.007@teste.local",
                PerfilUsuario.ADMIN
        );
        String token = tokenService.gerarToken(admin);

        mockMvc.perform(
                        patch("/usuarios/{usuarioId}/status", alvo.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content("{\"ativo\":false}")
                )
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void deveBloquearAtualizarStatusUsuarioComoCto() throws Exception {
        Usuario alvo = persistirUsuario(
                "Usuario Alvo",
                "usuario.alvo.008@teste.local",
                PerfilUsuario.ADMIN
        );
        Usuario cto = persistirUsuario(
                "Cto Ator",
                "cto.ator.008@teste.local",
                PerfilUsuario.CTO
        );
        String token = tokenService.gerarToken(cto);

        mockMvc.perform(
                        patch("/usuarios/{usuarioId}/status", alvo.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content("{\"ativo\":false}")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirResetarSenhaComoAdmin() throws Exception {
        Usuario alvo = persistirUsuario(
                "Usuario Alvo",
                "usuario.alvo.009@teste.local",
                PerfilUsuario.ADMIN
        );
        Usuario admin = persistirUsuario(
                "Admin Ator",
                "admin.ator.009@teste.local",
                PerfilUsuario.ADMIN
        );
        String token = tokenService.gerarToken(admin);

        mockMvc.perform(
                        post(
                                "/usuarios/{usuarioId}/reset-senha",
                                alvo.getId()
                        )
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isNoContent());
    }

    @Test
    @Transactional
    void deveBloquearResetarSenhaComoCto() throws Exception {
        Usuario alvo = persistirUsuario(
                "Usuario Alvo",
                "usuario.alvo.010@teste.local",
                PerfilUsuario.ADMIN
        );
        Usuario cto = persistirUsuario(
                "Cto Ator",
                "cto.ator.010@teste.local",
                PerfilUsuario.CTO
        );
        String token = tokenService.gerarToken(cto);

        mockMvc.perform(
                        post(
                                "/usuarios/{usuarioId}/reset-senha",
                                alvo.getId()
                        )
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void devePermitirAlterarPropriaSenhaComoTecnicoAutenticado()
            throws Exception {
        Usuario tecnico = new Usuario();
        tecnico.setNome("Tecnico Senha");
        tecnico.setEmail("tecnico.senha.011@teste.local");
        tecnico.setPerfil(PerfilUsuario.TECNICO);
        tecnico.setSenha(passwordEncoder.encode("senha-atual-011"));
        tecnico.setAtivo(true);
        usuarioRepository.save(tecnico);

        String token = tokenService.gerarToken(tecnico);

        mockMvc.perform(
                        patch("/usuarios/me/senha")
                                .header("Authorization", "Bearer " + token)
                                .contentType("application/json")
                                .content(
                                        "{\"senhaAtual\":\"senha-atual-011\","
                                                + "\"novaSenha\":\"senha-nova-011\","
                                                + "\"confirmacaoSenha\":\"senha-nova-011\"}"
                                )
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void deveBloquearAlterarPropriaSenhaSemToken() throws Exception {
        mockMvc.perform(
                        patch("/usuarios/me/senha")
                                .contentType("application/json")
                                .content("{}")
                )
                .andExpect(status().isUnauthorized());
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
