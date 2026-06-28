package br.com.smartdispatch.controller;

import br.com.smartdispatch.model.BaseOperacional;
import br.com.smartdispatch.service.BaseOperacionalService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contratos/{contratoId}/bases")
public class BaseOperacionalController {

    private final BaseOperacionalService baseService;

    public BaseOperacionalController(
            BaseOperacionalService baseService
    ) {
        this.baseService = baseService;
    }

    @PostMapping
    public BaseOperacional criar(
            @PathVariable Long contratoId,
            @RequestBody BaseOperacional base
    ) {
        return baseService.criar(contratoId, base);
    }

    @GetMapping
    public List<BaseOperacional> listar(
            @PathVariable Long contratoId
    ) {
        return baseService.listarPorContrato(contratoId);
    }

    @GetMapping("/{baseId}")
    public BaseOperacional buscarPorId(
            @PathVariable Long contratoId,
            @PathVariable Long baseId
    ) {
        return baseService.buscarPorId(contratoId, baseId);
    }

    @PutMapping("/{baseId}")
    public BaseOperacional atualizar(
            @PathVariable Long contratoId,
            @PathVariable Long baseId,
            @RequestBody BaseOperacional novosDados
    ) {
        return baseService.atualizar(
                contratoId,
                baseId,
                novosDados
        );
    }

    @DeleteMapping("/{baseId}")
    public void excluir(
            @PathVariable Long contratoId,
            @PathVariable Long baseId
    ) {
        baseService.excluir(contratoId, baseId);
    }
}