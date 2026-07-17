package br.com.smartdispatch.controller;

import br.com.smartdispatch.dto.OrdemServicoRequest;
import br.com.smartdispatch.dto.OrdemServicoResponse;
import br.com.smartdispatch.service.OrdemServicoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import br.com.smartdispatch.dto.CheckInRequest;

import java.util.List;
import br.com.smartdispatch.dto.SugestaoTecnicoResponse;
import br.com.smartdispatch.service.SugestaoTecnicoService;

@RestController
@RequestMapping(
        "/contratos/{contratoId}/chamados/{chamadoId}/ordens-servico"
)
public class OrdemServicoController {

    private final SugestaoTecnicoService sugestaoTecnicoService;
    private final OrdemServicoService ordemServicoService;

    public OrdemServicoController(
            OrdemServicoService ordemServicoService,
            SugestaoTecnicoService sugestaoTecnicoService
    ) {
        this.ordemServicoService = ordemServicoService;
        this.sugestaoTecnicoService = sugestaoTecnicoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrdemServicoResponse criar(
            @PathVariable Long contratoId,
            @PathVariable Long chamadoId,
            @RequestBody OrdemServicoRequest request
    ) {
        return ordemServicoService.criar(
                contratoId,
                chamadoId,
                request
        );
    }

    @GetMapping
    public List<OrdemServicoResponse> listarPorChamado(
            @PathVariable Long contratoId,
            @PathVariable Long chamadoId
    ) {
        return ordemServicoService.listarPorChamado(
                contratoId,
                chamadoId
        );
    }

    @PutMapping("/{ordemServicoId}")
    public OrdemServicoResponse atualizar(
            @PathVariable Long contratoId,
            @PathVariable Long chamadoId,
            @PathVariable Long ordemServicoId,
            @RequestBody OrdemServicoRequest request
    ) {
        return ordemServicoService.atualizar(
                contratoId,
                chamadoId,
                ordemServicoId,
                request
        );
    }

    @PostMapping("/{ordemServicoId}/check-in")
    public OrdemServicoResponse realizarCheckIn(
            @PathVariable Long contratoId,
            @PathVariable Long chamadoId,
            @PathVariable Long ordemServicoId,
            @RequestBody CheckInRequest request
    ) {
        return ordemServicoService.realizarCheckIn(
                contratoId,
                chamadoId,
                ordemServicoId,
                request
        );
    }

    @PostMapping("/{ordemServicoId}/check-out")
    public OrdemServicoResponse realizarCheckOut(
            @PathVariable Long contratoId,
            @PathVariable Long chamadoId,
            @PathVariable Long ordemServicoId
    ) {
        return ordemServicoService.realizarCheckOut(
                contratoId,
                chamadoId,
                ordemServicoId
        );
    }

    @GetMapping("/{ordemServicoId}/sugestoes-tecnicos")
    public List<SugestaoTecnicoResponse> listarSugestoesTecnicos(
            @PathVariable Long contratoId,
            @PathVariable Long chamadoId,
            @PathVariable Long ordemServicoId
    ) {
        return sugestaoTecnicoService.listarSugestoes(
                contratoId,
                chamadoId,
                ordemServicoId
        );
    }
}