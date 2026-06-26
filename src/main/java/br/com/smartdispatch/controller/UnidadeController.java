package br.com.smartdispatch.controller;

import br.com.smartdispatch.model.Unidade;
import br.com.smartdispatch.service.UnidadeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contratos/{contratoId}/unidades")
public class UnidadeController {

    private final UnidadeService unidadeService;

    public UnidadeController(UnidadeService unidadeService) {
        this.unidadeService = unidadeService;
    }

    @PostMapping
    public Unidade criar(
            @PathVariable Long contratoId,
            @RequestBody Unidade unidade
    ) {
        return unidadeService.criar(contratoId, unidade);
    }

    @GetMapping
    public List<Unidade> listarPorContrato(
            @PathVariable Long contratoId
    ) {
        return unidadeService.listarPorContrato(contratoId);
    }

    @GetMapping("/{unidadeId}")
    public Unidade buscarPorId(
            @PathVariable Long contratoId,
            @PathVariable Long unidadeId
    ) {
        return unidadeService.buscarPorId(contratoId, unidadeId);
    }

    @PutMapping("/{unidadeId}")
    public Unidade atualizar(
            @PathVariable Long contratoId,
            @PathVariable Long unidadeId,
            @RequestBody Unidade novosDados
    ) {
        return unidadeService.atualizar(
                contratoId,
                unidadeId,
                novosDados
        );
    }

    @DeleteMapping("/{unidadeId}")
    public void excluir(
            @PathVariable Long contratoId,
            @PathVariable Long unidadeId
    ) {
        unidadeService.excluir(contratoId, unidadeId);
    }
}