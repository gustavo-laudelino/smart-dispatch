package br.com.smartdispatch.service;

import br.com.smartdispatch.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final long expirationSeconds;

    public TokenService(
            JwtEncoder jwtEncoder,
            @Value("${app.jwt.expiration-seconds}")
            long expirationSeconds
    ) {
        this.jwtEncoder = jwtEncoder;
        this.expirationSeconds = expirationSeconds;
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("smart-dispatch")
                .issuedAt(agora)
                .expiresAt(
                        agora.plusSeconds(expirationSeconds)
                )
                .subject(usuario.getEmail())
                .claim("usuarioId", usuario.getId())
                .claim("nome", usuario.getNome())
                .claim(
                        "perfil",
                        usuario.getPerfil().name()
                )
                .build();

        JwtEncoderParameters parametros =
                JwtEncoderParameters.from(claims);

        return jwtEncoder
                .encode(parametros)
                .getTokenValue();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}