package br.com.smartdispatch.controller;

import br.com.smartdispatch.dto.ChamadoResponse;
import br.com.smartdispatch.service.ChamadoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.data.domain.Page;
@RestController
public class ChamadoFeedController {

    private final ChamadoService chamadoService;

    public ChamadoFeedController(
            ChamadoService chamadoService
    ) {
        this.chamadoService = chamadoService;
    }

    @GetMapping("/chamados")
    public Page<ChamadoResponse> listarFeed(
            @RequestParam(required = false) Long contratoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return chamadoService.listarFeed(
                contratoId,
                page,
                size,
                direction
        );
    }
}