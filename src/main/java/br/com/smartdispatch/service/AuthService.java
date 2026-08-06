package br.com.smartdispatch.service;

import br.com.smartdispatch.dto.LoginRequest;
import br.com.smartdispatch.dto.LoginResponse;
import br.com.smartdispatch.model.Usuario;
import br.com.smartdispatch.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public LoginResponse autenticar(LoginRequest request) {
        String email = request.getEmail();
        String senha = request.getSenha();

        if (
                email == null ||
                        email.isBlank() ||
                        senha == null ||
                        senha.isBlank()
        ) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "E-mail e senha são obrigatórios."
            );
        }

        Usuario usuario = usuarioRepository
                .findByEmailIgnoreCase(email.trim())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "E-mail ou senha inválidos."
                        )
                );

        if (!usuario.isAtivo()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Usuário inativo."
            );
        }

        boolean senhaCorreta = passwordEncoder.matches(
                senha,
                usuario.getSenha()
        );

        if (!senhaCorreta) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "E-mail ou senha inválidos."
            );
        }

        String token = tokenService.gerarToken(usuario);

        return new LoginResponse(
                token,
                "Bearer",
                tokenService.getExpirationSeconds(),
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil()
        );
    }
}