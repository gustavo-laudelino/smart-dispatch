package br.com.smartdispatch.controller;

import br.com.smartdispatch.dto.CheckInRequest;
import br.com.smartdispatch.dto.OrdemServicoRequest;
import br.com.smartdispatch.dto.OrdemServicoResponse;
import br.com.smartdispatch.dto.SugestaoTecnicoResponse;
import br.com.smartdispatch.service.AutorizacaoService;
import br.com.smartdispatch.service.OrdemServicoService;
import br.com.smartdispatch.service.SugestaoTecnicoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * API canônica de Ordem de Serviço como recurso de primeira classe — OS com
 * ou sem Chamado. As rotas antigas em {@link OrdemServicoController}
 * (aninhadas em Chamado) continuam existindo e convergem para as mesmas
 * regras de {@link OrdemServicoService}.
 */
@RestController
@RequestMapping("/contratos/{contratoId}/ordens-servico")
public class OrdemServicoContratoController {

    private final OrdemServicoService ordemServicoService;
    private final SugestaoTecnicoService sugestaoTecnicoService;
    private final AutorizacaoService autorizacaoService;

    public OrdemServicoContratoController(
            OrdemServicoService ordemServicoService,
            SugestaoTecnicoService sugestaoTecnicoService,
            AutorizacaoService autorizacaoService
    ) {
        this.ordemServicoService = ordemServicoService;
        this.sugestaoTecnicoService = sugestaoTecnicoService;
        this.autorizacaoService = autorizacaoService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CTO')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrdemServicoResponse criar(
            @PathVariable Long contratoId,
            @RequestBody OrdemServicoRequest request
    ) {
        return ordemServicoService.criar(contratoId, request);
    }

    @PreAuthorize(
            "hasAnyRole('ADMIN', 'CTO') or "
                    + "(hasAnyRole('TECNICO', 'TECNICO_INTERNO') and "
                    + "@autorizacaoService.tecnicoPertenceAoContrato(authentication, #contratoId))"
    )
    @GetMapping
    public List<OrdemServicoResponse> listar(
            @PathVariable Long contratoId,
            @RequestParam(required = false) Long chamadoId,
            @RequestParam(required = false) Long tecnicoId,
            @RequestParam(defaultValue = "false") boolean meus,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            Authentication authentication
    ) {
        Long tecnicoIdEfetivo = autorizacaoService.resolverTecnicoIdEfetivo(
                authentication,
                tecnicoId,
                meus
        );

        return ordemServicoService.listarPorContrato(
                contratoId,
                chamadoId,
                tecnicoIdEfetivo,
                data,
                dataInicio,
                dataFim
        );
    }

    @PreAuthorize(
            "hasAnyRole('ADMIN', 'CTO') or "
                    + "(hasAnyRole('TECNICO', 'TECNICO_INTERNO') and "
                    + "@autorizacaoService.tecnicoPertenceAoContrato(authentication, #contratoId))"
    )
    @GetMapping("/{ordemServicoId}")
    public OrdemServicoResponse buscarPorId(
            @PathVariable Long contratoId,
            @PathVariable Long ordemServicoId
    ) {
        return ordemServicoService.buscarPorId(contratoId, ordemServicoId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CTO')")
    @PutMapping("/{ordemServicoId}")
    public OrdemServicoResponse atualizar(
            @PathVariable Long contratoId,
            @PathVariable Long ordemServicoId,
            @RequestBody OrdemServicoRequest request
    ) {
        return ordemServicoService.atualizar(contratoId, ordemServicoId, request);
    }

    @PreAuthorize(
            "hasAnyRole('ADMIN', 'CTO') or "
                    + "(hasAnyRole('TECNICO', 'TECNICO_INTERNO') and "
                    + "@autorizacaoService.tecnicoAtribuidoAOrdemServico("
                    + "authentication, #contratoId, #ordemServicoId))"
    )
    @PostMapping("/{ordemServicoId}/check-in")
    public OrdemServicoResponse realizarCheckIn(
            @PathVariable Long contratoId,
            @PathVariable Long ordemServicoId,
            @RequestBody CheckInRequest request
    ) {
        return ordemServicoService.realizarCheckIn(contratoId, ordemServicoId, request);
    }

    @PreAuthorize(
            "hasAnyRole('ADMIN', 'CTO') or "
                    + "(hasAnyRole('TECNICO', 'TECNICO_INTERNO') and "
                    + "@autorizacaoService.tecnicoAtribuidoAOrdemServico("
                    + "authentication, #contratoId, #ordemServicoId))"
    )
    @PostMapping("/{ordemServicoId}/check-out")
    public OrdemServicoResponse realizarCheckOut(
            @PathVariable Long contratoId,
            @PathVariable Long ordemServicoId
    ) {
        return ordemServicoService.realizarCheckOut(contratoId, ordemServicoId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CTO')")
    @GetMapping("/{ordemServicoId}/sugestoes-tecnicos")
    public List<SugestaoTecnicoResponse> listarSugestoesTecnicos(
            @PathVariable Long contratoId,
            @PathVariable Long ordemServicoId
    ) {
        return sugestaoTecnicoService.listarSugestoes(contratoId, ordemServicoId);
    }
}
