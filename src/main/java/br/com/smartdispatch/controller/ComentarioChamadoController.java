package br.com.smartdispatch.controller;

import br.com.smartdispatch.dto.ComentarioChamadoRequest;
import br.com.smartdispatch.dto.ComentarioChamadoResponse;
import br.com.smartdispatch.service.ComentarioChamadoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping(
        "/contratos/{contratoId}/chamados/{chamadoId}/comentarios"
)
public class ComentarioChamadoController {

    private final ComentarioChamadoService comentarioChamadoService;

    public ComentarioChamadoController(
            ComentarioChamadoService comentarioChamadoService
    ) {
        this.comentarioChamadoService = comentarioChamadoService;
    }

    @PreAuthorize(
            "hasAnyRole('ADMIN', 'CTO') or " +
                    "(hasAnyRole('TECNICO', 'TECNICO_INTERNO') and " +
                    "@autorizacaoService.tecnicoPertenceAoContrato(authentication, #contratoId))"
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComentarioChamadoResponse criar(
            @PathVariable Long contratoId,
            @PathVariable Long chamadoId,
            @RequestBody ComentarioChamadoRequest request,
            Authentication authentication
    ) {
        return comentarioChamadoService.criar(
                contratoId,
                chamadoId,
                request,
                authentication
        );
    }

    @GetMapping
    public List<ComentarioChamadoResponse> listarPorChamado(
            @PathVariable Long contratoId,
            @PathVariable Long chamadoId
    ) {
        return comentarioChamadoService.listarPorChamado(
                contratoId,
                chamadoId
        );
    }
}