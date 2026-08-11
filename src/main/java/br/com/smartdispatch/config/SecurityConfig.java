package br.com.smartdispatch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import br.com.smartdispatch.repository.UsuarioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@EnableMethodSecurity
@Configuration
public class SecurityConfig {


    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(
            UsuarioRepository usuarioRepository
    ) {
        JwtAuthenticationConverter authenticationConverter =
                new JwtAuthenticationConverter();

        authenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {

            Number usuarioId = jwt.getClaim("usuarioId");

            if (usuarioId == null) {
                return List.of();
            }

            return usuarioRepository
                    .findById(usuarioId.longValue())
                    .map(usuario ->
                            List.<GrantedAuthority>of(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + usuario.getPerfil().name()
                                    )
                            )
                    )
                    .orElse(List.of());
        });

        return authenticationConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter authenticationConverter
    ) throws Exception {

        http
                .csrf(csrf ->
                        csrf.disable()
                )
                .httpBasic(httpBasic ->
                        httpBasic.disable()
                )
                .formLogin(formLogin ->
                        formLogin.disable()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers(
                                        "/auth/login",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**",
                                        "/error"
                                )
                                .permitAll()
                                .anyRequest()
                                .authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        authenticationConverter
                                )
                        )
                );


        return http.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}