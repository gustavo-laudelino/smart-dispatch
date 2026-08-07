package br.com.smartdispatch.controller;

import br.com.smartdispatch.dto.TecnicoResponse;
import br.com.smartdispatch.service.TecnicoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAnyRole('ADMIN', 'CTO')")
@RestController
@RequestMapping("/contratos/{contratoId}/bases/{baseId}/tecnicos")
public class TecnicoController {

    private final TecnicoService tecnicoService;

    public TecnicoController(TecnicoService tecnicoService) {
        this.tecnicoService = tecnicoService;
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
}