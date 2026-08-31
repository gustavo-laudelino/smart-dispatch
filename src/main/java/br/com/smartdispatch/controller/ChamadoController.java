package br.com.smartdispatch.controller;

import br.com.smartdispatch.dto.ChamadoRequest;
import br.com.smartdispatch.dto.ChamadoResponse;
import br.com.smartdispatch.service.ChamadoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import br.com.smartdispatch.dto.StatusChamadoRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import org.springframework.security.core.Authentication;

@PreAuthorize(
        "hasAnyRole('ADMIN', 'CTO') or " +
                "(hasAnyRole('TECNICO', 'TECNICO_INTERNO') and " +
                "@autorizacaoService.tecnicoPertenceAoContrato(authentication, #contratoId))"
)
@RestController
@RequestMapping("/contratos/{contratoId}/chamados")
public class ChamadoController {

    private final ChamadoService chamadoService;

    public ChamadoController(ChamadoService chamadoService) {
        this.chamadoService = chamadoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChamadoResponse criar(
            @PathVariable Long contratoId,
            @Valid @RequestBody ChamadoRequest request
    ) {
        return chamadoService.criar(
                contratoId,
                request
        );
    }

    @GetMapping
    public List<ChamadoResponse> listarPorContrato(
            @PathVariable Long contratoId
    ) {
        return chamadoService.listarPorContrato(contratoId);
    }

    @GetMapping("/{chamadoId}")
    public ChamadoResponse buscarPorId(
            @PathVariable Long contratoId,
            @PathVariable Long chamadoId
    ) {
        return chamadoService.buscarPorId(
                contratoId,
                chamadoId
        );
    }

    @PutMapping("/{chamadoId}")
    public ChamadoResponse atualizar(
            @PathVariable Long contratoId,
            @PathVariable Long chamadoId,
            @Valid @RequestBody ChamadoRequest request,
            Authentication authentication
    ) {
        return chamadoService.atualizar(
                contratoId,
                chamadoId,
                request,
                authentication
        );
    }

    @PatchMapping("/{chamadoId}/status")
    @PreAuthorize(
            "@autorizacaoService.podeAlterarStatusChamado(authentication, #contratoId, #request)"
    )
    public ChamadoResponse atualizarStatus(
            @PathVariable Long contratoId,
            @PathVariable Long chamadoId,
            @RequestBody StatusChamadoRequest request,
            Authentication authentication
    ) {
        return chamadoService.atualizarStatus(
                contratoId,
                chamadoId,
                request,
                authentication
        );
    }
}

