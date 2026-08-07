package br.com.smartdispatch.controller;

import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.service.ContratoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import br.com.smartdispatch.service.AutorizacaoService;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/contratos")
public class ContratoController {

    private final ContratoService contratoService;
    private final AutorizacaoService autorizacaoService;

    public ContratoController(
            ContratoService contratoService,
            AutorizacaoService autorizacaoService
    ) {
        this.contratoService = contratoService;
        this.autorizacaoService = autorizacaoService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CTO')")
    @PostMapping
    public Contrato criar(@RequestBody Contrato contrato) {
        return contratoService.criar(contrato);
    }

    @GetMapping
    public List<Contrato> listar(Authentication authentication) {

        boolean gestor = authentication
                .getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")
                                || authority.getAuthority().equals("ROLE_CTO")
                );

        if (gestor) {
            return contratoService.listar();
        }

        Long contratoId = autorizacaoService
                .obterContratoIdTecnico(authentication);

        if (contratoId == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Usuário não possui contrato vinculado"
            );
        }

        return List.of(
                contratoService.buscarPorId(contratoId)
        );
    }

    @PreAuthorize(
            "hasAnyRole('ADMIN', 'CTO') or " +
                    "(hasAnyRole('TECNICO', 'TECNICO_INTERNO') and " +
                    "@autorizacaoService.tecnicoPertenceAoContrato(authentication, #id))"
    )
    @GetMapping("/{id}")
    public Contrato buscarPorId(@PathVariable Long id) {
        return contratoService.buscarPorId(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CTO')")
    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id) {
        contratoService.excluir(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CTO')")
    @PutMapping("/{id}")
    public Contrato atualizar(
            @PathVariable Long id,
            @RequestBody Contrato novosDados
    ) {
        return contratoService.atualizar(id, novosDados);
    }
}