package br.com.smartdispatch.controller;

import br.com.smartdispatch.dto.ChamadoResponse;
import br.com.smartdispatch.service.ChamadoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ChamadoFeedController {

    private final ChamadoService chamadoService;

    public ChamadoFeedController(
            ChamadoService chamadoService
    ) {
        this.chamadoService = chamadoService;
    }

    @GetMapping("/chamados")
    public List<ChamadoResponse> listarFeed(
            @RequestParam(required = false) Long contratoId
    ) {
        return chamadoService.listarFeed(contratoId);
    }
}