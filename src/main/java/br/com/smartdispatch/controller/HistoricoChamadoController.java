package br.com.smartdispatch.controller;

import br.com.smartdispatch.dto.HistoricoChamadoResponse;
import br.com.smartdispatch.service.HistoricoChamadoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(
        "/contratos/{contratoId}/chamados/{chamadoId}/historico"
)
public class HistoricoChamadoController {

    private final HistoricoChamadoService historicoChamadoService;

    public HistoricoChamadoController(
            HistoricoChamadoService historicoChamadoService
    ) {
        this.historicoChamadoService =
                historicoChamadoService;
    }

    @GetMapping
    public List<HistoricoChamadoResponse> listarPorChamado(
            @PathVariable Long contratoId,
            @PathVariable Long chamadoId
    ) {
        return historicoChamadoService
                .listarPorChamado(
                        contratoId,
                        chamadoId
                );
    }
}