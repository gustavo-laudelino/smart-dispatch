package br.com.smartdispatch.controller;

import br.com.smartdispatch.dto.CriarUsuarioRequest;
import br.com.smartdispatch.dto.UsuarioResponse;
import br.com.smartdispatch.service.UsuarioService;
import br.com.smartdispatch.dto.AtualizarUsuarioRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import br.com.smartdispatch.dto.AtualizarStatusUsuarioRequest;
import br.com.smartdispatch.dto.AlterarSenhaRequest;
import org.springframework.security.core.Authentication;
import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse criar(
            @RequestBody CriarUsuarioRequest request
    ) {
        return usuarioService.criar(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CTO')")
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/{usuarioId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CTO')")
    public UsuarioResponse buscarPorId(
            @PathVariable Long usuarioId
    ) {
        return usuarioService.buscarPorId(usuarioId);
    }

    @PutMapping("/{usuarioId}")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse atualizar(
            @PathVariable Long usuarioId,
            @RequestBody AtualizarUsuarioRequest request
    ) {
        return usuarioService.atualizar(
                usuarioId,
                request
        );
    }

    @PatchMapping("/{usuarioId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse atualizarStatus(
            @PathVariable Long usuarioId,
            @RequestBody AtualizarStatusUsuarioRequest request
    ) {
        return usuarioService.atualizarStatus(
                usuarioId,
                request
        );
    }

    @PostMapping("/{usuarioId}/reset-senha")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetarSenha(
            @PathVariable Long usuarioId
    ) {
        usuarioService.resetarSenha(usuarioId);
    }

    @PatchMapping("/me/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarPropriaSenha(
            @RequestBody AlterarSenhaRequest request,
            Authentication authentication
    ) {
        usuarioService.alterarPropriaSenha(
                request,
                authentication
        );
    }
}