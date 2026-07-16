package br.com.smartdispatch.controller;

import br.com.smartdispatch.dto.ChamadoRequest;
import br.com.smartdispatch.dto.ChamadoResponse;
import br.com.smartdispatch.service.ChamadoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import br.com.smartdispatch.dto.StatusChamadoRequest;

import java.util.List;

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
            @RequestBody ChamadoRequest request
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
            @RequestBody ChamadoRequest request
    ) {
        return chamadoService.atualizar(
                contratoId,
                chamadoId,
                request
        );
    }

    @PatchMapping("/{chamadoId}/status")
    public ChamadoResponse atualizarStatus(
            @PathVariable Long contratoId,
            @PathVariable Long chamadoId,
            @RequestBody StatusChamadoRequest request
    ) {
        return chamadoService.atualizarStatus(
                contratoId,
                chamadoId,
                request
        );
    }
}

