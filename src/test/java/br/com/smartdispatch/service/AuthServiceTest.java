package br.com.smartdispatch.service;

import br.com.smartdispatch.dto.LoginRequest;
import br.com.smartdispatch.dto.LoginResponse;
import br.com.smartdispatch.enums.PerfilUsuario;
import br.com.smartdispatch.model.Usuario;
import br.com.smartdispatch.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void deveLancarBadRequestQuandoEmailOuSenhaNaoForemInformados() {

        // Arrange
        LoginRequest requestEmailNulo = new LoginRequest();
        requestEmailNulo.setEmail(null);
        requestEmailNulo.setSenha("senha123");

        LoginRequest requestEmailEmBranco = new LoginRequest();
        requestEmailEmBranco.setEmail("   ");
        requestEmailEmBranco.setSenha("senha123");

        LoginRequest requestSenhaNula = new LoginRequest();
        requestSenhaNula.setEmail("usuario@empresa.com");
        requestSenhaNula.setSenha(null);

        LoginRequest requestSenhaEmBranco = new LoginRequest();
        requestSenhaEmBranco.setEmail("usuario@empresa.com");
        requestSenhaEmBranco.setSenha("   ");

        // Act & Assert
        assertBadRequestEmailSenhaObrigatorios(requestEmailNulo);
        assertBadRequestEmailSenhaObrigatorios(requestEmailEmBranco);
        assertBadRequestEmailSenhaObrigatorios(requestSenhaNula);
        assertBadRequestEmailSenhaObrigatorios(requestSenhaEmBranco);

        verifyNoInteractions(usuarioRepository);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(tokenService);
    }

    @Test
    void deveLancarUnauthorizedQuandoEmailNaoForEncontrado() {

        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("  Usuario@Empresa.com  ");
        request.setSenha("senha123");

        when(usuarioRepository.findByEmailIgnoreCase("Usuario@Empresa.com"))
                .thenReturn(Optional.empty());

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.autenticar(request)
        );

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("E-mail ou senha inválidos.", exception.getReason());

        verify(usuarioRepository).findByEmailIgnoreCase("Usuario@Empresa.com");
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(tokenService);
    }

    @Test
    void deveLancarForbiddenQuandoUsuarioEstiverInativo() {

        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("usuario@empresa.com");
        request.setSenha("senha123");

        Usuario usuario = criarUsuario(
                1L, "Usuário Teste", "usuario@empresa.com", "hash-atual", PerfilUsuario.CTO, false
        );

        when(usuarioRepository.findByEmailIgnoreCase("usuario@empresa.com"))
                .thenReturn(Optional.of(usuario));

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.autenticar(request)
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Usuário inativo.", exception.getReason());

        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(tokenService);
    }

    @Test
    void deveLancarUnauthorizedQuandoSenhaEstiverIncorreta() {

        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("usuario@empresa.com");
        request.setSenha("senhaErrada");

        Usuario usuario = criarUsuario(
                1L, "Usuário Teste", "usuario@empresa.com", "hash-atual", PerfilUsuario.CTO, true
        );

        when(usuarioRepository.findByEmailIgnoreCase("usuario@empresa.com"))
                .thenReturn(Optional.of(usuario));

        when(passwordEncoder.matches("senhaErrada", "hash-atual"))
                .thenReturn(false);

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.autenticar(request)
        );

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("E-mail ou senha inválidos.", exception.getReason());

        verify(passwordEncoder).matches("senhaErrada", "hash-atual");
        verifyNoInteractions(tokenService);
    }

    @Test
    void deveAutenticarUsuarioAtivoComCredenciaisValidas() {

        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("  usuario@empresa.com  ");
        request.setSenha("senhaCorreta");

        Usuario usuario = criarUsuario(
                1L, "Usuário Teste", "usuario@empresa.com", "hash-atual",
                PerfilUsuario.TECNICO_INTERNO, true
        );

        when(usuarioRepository.findByEmailIgnoreCase("usuario@empresa.com"))
                .thenReturn(Optional.of(usuario));

        when(passwordEncoder.matches("senhaCorreta", "hash-atual"))
                .thenReturn(true);

        when(tokenService.gerarToken(usuario))
                .thenReturn("jwt-gerado");

        when(tokenService.getExpirationSeconds())
                .thenReturn(43200L);

        // Act
        LoginResponse response = authService.autenticar(request);

        // Assert
        assertEquals("jwt-gerado", response.getToken());
        assertEquals("Bearer", response.getTipo());
        assertEquals(43200L, response.getExpiraEmSegundos());
        assertEquals(usuario.getId(), response.getUsuarioId());
        assertEquals(usuario.getNome(), response.getNome());
        assertEquals(usuario.getEmail(), response.getEmail());
        assertEquals(usuario.getPerfil(), response.getPerfil());

        verify(usuarioRepository).findByEmailIgnoreCase("usuario@empresa.com");
        verify(passwordEncoder).matches("senhaCorreta", "hash-atual");
        verify(tokenService).gerarToken(usuario);
        verify(tokenService).getExpirationSeconds();
    }

    // ---------------------------------------------------------------
    // Fixtures auxiliares
    // ---------------------------------------------------------------

    private Usuario criarUsuario(
            Long id,
            String nome,
            String email,
            String senhaHash,
            PerfilUsuario perfil,
            boolean ativo
    ) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha(senhaHash);
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);
        return usuario;
    }

    private void assertBadRequestEmailSenhaObrigatorios(LoginRequest request) {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.autenticar(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("E-mail e senha são obrigatórios.", exception.getReason());
    }
}
