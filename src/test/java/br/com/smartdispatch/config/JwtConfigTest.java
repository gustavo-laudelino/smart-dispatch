package br.com.smartdispatch.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtConfigTest {

    private final JwtConfig jwtConfig = new JwtConfig();

    @Test
    void deveCriarSecretKeyAPartirDeSecretBase64() {

        // Arrange
        byte[] bytes = "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8);
        String secret = Base64.getEncoder().encodeToString(bytes);

        // Act
        SecretKey key = jwtConfig.jwtSecretKey(secret);

        // Assert
        assertNotNull(key);
        assertEquals("HmacSHA256", key.getAlgorithm());
        assertArrayEquals(bytes, key.getEncoded());
    }

    @Test
    void deveCriarEncoderEDecoderCompativeisComHs256() {

        // Arrange
        byte[] bytes = "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8);
        String secret = Base64.getEncoder().encodeToString(bytes);

        SecretKey key = jwtConfig.jwtSecretKey(secret);
        JwtEncoder encoder = jwtConfig.jwtEncoder(key);
        JwtDecoder decoder = jwtConfig.jwtDecoder(key);

        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plusSeconds(3600);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("usuario@empresa.com")
                .claim("usuarioId", 10L)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();

        // Act
        Jwt jwtCodificado = encoder.encode(JwtEncoderParameters.from(claims));
        String tokenValue = jwtCodificado.getTokenValue();

        Jwt jwtDecodificado = decoder.decode(tokenValue);

        // Assert
        assertNotNull(tokenValue);
        assertFalse(tokenValue.isBlank());

        assertEquals("usuario@empresa.com", jwtDecodificado.getSubject());
        assertEquals(Long.valueOf(10L), jwtDecodificado.getClaim("usuarioId"));
        assertEquals(issuedAt, jwtDecodificado.getIssuedAt());
        assertEquals(expiresAt, jwtDecodificado.getExpiresAt());

        assertEquals(MacAlgorithm.HS256, jwtCodificado.getHeaders().get("alg"));
    }
}
