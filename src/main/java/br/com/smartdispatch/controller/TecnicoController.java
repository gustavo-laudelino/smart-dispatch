package br.com.smartdispatch.controller;

import br.com.smartdispatch.dto.CriarTecnicoRequest;
import br.com.smartdispatch.model.Tecnico;
import br.com.smartdispatch.service.TecnicoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/contratos/{contratoId}/bases/{baseId}/tecnicos")
public class TecnicoController {

    private final TecnicoService tecnicoService;

    public TecnicoController(TecnicoService tecnicoService) {
        this.tecnicoService = tecnicoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Tecnico criar(
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
}