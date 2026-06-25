package br.com.smartdispatch.controller;

import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.service.ContratoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contratos")
public class ContratoController {

    private final ContratoService contratoService;

    public ContratoController(ContratoService contratoService) {
        this.contratoService = contratoService;
    }

    @PostMapping
    public Contrato criar(@RequestBody Contrato contrato) {
        return contratoService.criar(contrato);
    }

    @GetMapping
    public List<Contrato> listar() {
        return contratoService.listar();
    }

    @GetMapping("/{id}")
    public Contrato buscarPorId(@PathVariable Long id) {
        return contratoService.buscarPorId(id);
    }

    @DeleteMapping("/{id}")
    public void excluir(@PathVariable Long id) {
        contratoService.excluir(id);
    }
}