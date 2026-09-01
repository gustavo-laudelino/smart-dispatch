package br.com.smartdispatch.controller;

import br.com.smartdispatch.dto.ChamadoResponse;
import br.com.smartdispatch.service.ChamadoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.data.domain.Page;
import br.com.smartdispatch.service.AutorizacaoService;
import org.springframework.security.core.Authentication;

@RestController
public class ChamadoFeedController {

    private final ChamadoService chamadoService;
    private final AutorizacaoService autorizacaoService;

    public ChamadoFeedController(
            ChamadoService chamadoService,
            AutorizacaoService autorizacaoService
    ) {
        this.chamadoService = chamadoService;
        this.autorizacaoService = autorizacaoService;
    }


    @GetMapping("/chamados")
    public Page<ChamadoResponse> listarFeed(
            @RequestParam(required = false) Long contratoId,
            @RequestParam(required = false) Long tecnicoId,
            @RequestParam(defaultValue = "false") boolean meus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String direction,
            Authentication authentication
    ) {
        Long contratoIdPermitido =
                autorizacaoService.resolverContratoPermitido(
                        authentication,
                        contratoId
                );

        Long tecnicoIdEfetivo =
                autorizacaoService.resolverTecnicoIdEfetivo(
                        authentication,
                        tecnicoId,
                        meus
                );

        return chamadoService.listarFeed(
                contratoIdPermitido,
                tecnicoIdEfetivo,
                page,
                size,
                direction
        );
    }
}