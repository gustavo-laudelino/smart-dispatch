package br.com.smartdispatch.controller;

import br.com.smartdispatch.dto.AtualizarTecnicoRequest;
import br.com.smartdispatch.dto.CriarTecnicoRequest;
import br.com.smartdispatch.dto.TecnicoResponse;
import br.com.smartdispatch.service.TecnicoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contratos/{contratoId}/bases/{baseId}/tecnicos")
public class TecnicoController {

    private final TecnicoService tecnicoService;

    public TecnicoController(TecnicoService tecnicoService) {
        this.tecnicoService = tecnicoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TecnicoResponse criar(
            @PathVariable Long contratoId,
            @PathVariable Long baseId,
            @RequestBody CriarTecnicoRequest request
    ) {
        return tecnicoService.criar(
                contratoId,
                baseId,
                request
        );
    }

    @GetMapping
    public List<TecnicoResponse> listar(
            @PathVariable Long contratoId,
            @PathVariable Long baseId
    ) {
        return tecnicoService.listar(
                contratoId,
                baseId
        );
    }

    @GetMapping("/{tecnicoId}")
    public TecnicoResponse buscarPorId(
            @PathVariable Long contratoId,
            @PathVariable Long baseId,
            @PathVariable Long tecnicoId
    ) {
        return tecnicoService.buscarPorId(
                contratoId,
                baseId,
                tecnicoId
        );
    }

    @PutMapping("/{tecnicoId}")
    public TecnicoResponse atualizar(
            @PathVariable Long contratoId,
            @PathVariable Long baseId,
            @PathVariable Long tecnicoId,
            @RequestBody AtualizarTecnicoRequest request
    ) {
        return tecnicoService.atualizar(
                contratoId,
                baseId,
                tecnicoId,
                request
        );
    }
}