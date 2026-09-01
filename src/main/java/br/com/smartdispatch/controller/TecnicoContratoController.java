package br.com.smartdispatch.controller;

import br.com.smartdispatch.dto.TecnicoResponse;
import br.com.smartdispatch.service.TecnicoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@PreAuthorize("hasAnyRole('ADMIN', 'CTO')")
@RestController
public class TecnicoContratoController {

    private final TecnicoService tecnicoService;

    public TecnicoContratoController(TecnicoService tecnicoService) {
        this.tecnicoService = tecnicoService;
    }

    @GetMapping("/contratos/{contratoId}/tecnicos")
    public List<TecnicoResponse> listarPorContrato(
            @PathVariable Long contratoId
    ) {
        return tecnicoService.listarPorContrato(contratoId);
    }
}
