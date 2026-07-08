package br.com.smartdispatch.controller;

import br.com.smartdispatch.dto.ComentarioChamadoRequest;
import br.com.smartdispatch.dto.ComentarioChamadoResponse;
import br.com.smartdispatch.service.ComentarioChamadoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComentarioChamadoResponse criar(
            @PathVariable Long contratoId,
            @PathVariable Long chamadoId,
            @RequestBody ComentarioChamadoRequest request
    ) {
        return comentarioChamadoService.criar(
                contratoId,
                chamadoId,
                request
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