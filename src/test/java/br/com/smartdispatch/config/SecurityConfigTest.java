package br.com.smartdispatch.config;

import br.com.smartdispatch.enums.PerfilUsuario;
import br.com.smartdispatch.model.Usuario;
import br.com.smartdispatch.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Test
    void deveRetornarSemAuthoritiesQuandoJwtNaoPossuirUsuarioId() {

        // Arrange
        SecurityConfig securityConfig = new SecurityConfig();
        JwtAuthenticationConverter converter =
                securityConfig.jwtAuthenticationConverter(usuarioRepository);

        Jwt jwt = criarJwt(null, "ADMIN");

        // Act
        AbstractAuthenticationToken authentication = converter.convert(jwt);

        // Assert
        assertNotNull(authentication);
        assertTrue(extrairRoles(authentication.getAuthorities()).isEmpty());

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void deveRetornarSemAuthoritiesQuandoUsuarioDoJwtNaoExistir() {

        // Arrange
        SecurityConfig securityConfig = new SecurityConfig();
        JwtAuthenticationConverter converter =
                securityConfig.jwtAuthenticationConverter(usuarioRepository);

        Jwt jwt = criarJwt(10L, "ADMIN");

        when(usuarioRepository.findById(10L))
                .thenReturn(Optional.empty());

        // Act
        AbstractAuthenticationToken authentication = converter.convert(jwt);

        // Assert
        assertTrue(extrairRoles(authentication.getAuthorities()).isEmpty());

        verify(usuarioRepository).findById(10L);
    }

    @ParameterizedTest
    @EnumSource(PerfilUsuario.class)
    void deveUsarPerfilAtualDoUsuarioParaDefinirAuthority(PerfilUsuario perfil) {

        // Arrange
        SecurityConfig securityConfig = new SecurityConfig();
        JwtAuthenticationConverter converter =
                securityConfig.jwtAuthenticationConverter(usuarioRepository);

        Jwt jwt = criarJwt(10L, "PERFIL_QUE_NAO_DEVE_SER_USADO");

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setPerfil(perfil);

        when(usuarioRepository.findById(10L))
                .thenReturn(Optional.of(usuario));

        // Act
        AbstractAuthenticationToken authentication = converter.convert(jwt);

        // Assert
        List<String> roles = extrairRoles(authentication.getAuthorities());

        assertEquals(1, roles.size());
        assertEquals("ROLE_" + perfil.name(), roles.get(0));

        verify(usuarioRepository).findById(10L);
    }

    // ---------------------------------------------------------------
    // Fixtures auxiliares
    // ---------------------------------------------------------------

    /**
     * Isola as authorities ROLE_* concedidas pelo jwtGrantedAuthoritiesConverter
     * customizado, ignorando authorities de infraestrutura que o próprio
     * Spring Security adiciona a todo JwtAuthenticationToken (ex.: o marcador
     * de fator de autenticação "FACTOR_BEARER"), que não fazem parte da regra
     * de negócio testada aqui.
     */
    private List<String> extrairRoles(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .toList();
    }

    private Jwt criarJwt(Long usuarioId, String perfilClaim) {
        Jwt.Builder builder = Jwt.withTokenValue("token-teste")
                .header("alg", "HS256")
                .claim("sub", "usuario@teste.com")
                .claim("perfil", perfilClaim)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));

        if (usuarioId != null) {
            builder.claim("usuarioId", usuarioId);
        }

        return builder.build();
    }
}
