package br.com.smartdispatch.controller;

import br.com.smartdispatch.model.Unidade;
import br.com.smartdispatch.service.UnidadeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize(
        "hasAnyRole('ADMIN', 'CTO') or " +
                "(hasAnyRole('TECNICO', 'TECNICO_INTERNO') and " +
                "@autorizacaoService.tecnicoPertenceAoContrato(authentication, #contratoId))"
)
@RestController
@RequestMapping("/contratos/{contratoId}/unidades")
public class UnidadeController {

    private final UnidadeService unidadeService;

    public UnidadeController(UnidadeService unidadeService) {
        this.unidadeService = unidadeService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CTO')")
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

    @PreAuthorize("hasAnyRole('ADMIN', 'CTO')")
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

    @PreAuthorize("hasAnyRole('ADMIN', 'CTO')")
    @DeleteMapping("/{unidadeId}")
    public void excluir(
            @PathVariable Long contratoId,
            @PathVariable Long unidadeId
    ) {
        unidadeService.excluir(contratoId, unidadeId);
    }
}