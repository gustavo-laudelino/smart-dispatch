package br.com.smartdispatch.service;

import br.com.smartdispatch.enums.PerfilUsuario;
import br.com.smartdispatch.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Test
    void deveGerarTokenComClaimsEExpiracaoCorretas() {

        // Arrange
        long expirationSeconds = 43200L;
        TokenService tokenService = new TokenService(jwtEncoder, expirationSeconds);

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNome("Técnico Teste");
        usuario.setEmail("tecnico@empresa.com");
        usuario.setPerfil(PerfilUsuario.TECNICO_INTERNO);

        Jwt jwtGerado = Jwt.withTokenValue("jwt-gerado")
                .header("alg", "HS256")
                .claim("sub", "tecnico@empresa.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(expirationSeconds))
                .build();

        ArgumentCaptor<JwtEncoderParameters> parametrosCaptor =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);

        when(jwtEncoder.encode(parametrosCaptor.capture()))
                .thenReturn(jwtGerado);

        Instant antes = Instant.now();

        // Act
        String token = tokenService.gerarToken(usuario);

        Instant depois = Instant.now();

        // Assert
        assertEquals("jwt-gerado", token);

        JwtClaimsSet claims = parametrosCaptor.getValue().getClaims();

        assertEquals("smart-dispatch", claims.getClaimAsString("iss"));
        assertEquals(usuario.getEmail(), claims.getSubject());
        assertEquals(usuario.getId(), claims.getClaim("usuarioId"));
        assertEquals(usuario.getNome(), claims.getClaimAsString("nome"));
        assertEquals(usuario.getPerfil().name(), claims.getClaimAsString("perfil"));

        Instant issuedAt = claims.getIssuedAt();
        assertFalse(issuedAt.isBefore(antes));
        assertFalse(issuedAt.isAfter(depois));

        assertEquals(issuedAt.plusSeconds(expirationSeconds), claims.getExpiresAt());

        assertEquals(expirationSeconds, tokenService.getExpirationSeconds());
    }
}
