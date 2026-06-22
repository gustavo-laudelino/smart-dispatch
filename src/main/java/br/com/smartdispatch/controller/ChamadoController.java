package br.com.smartdispatch.controller;

import br.com.smartdispatch.enums.CategoriaChamado;
import br.com.smartdispatch.enums.Prioridade;
import br.com.smartdispatch.enums.TipoChamado;
import br.com.smartdispatch.model.Chamado;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/chamados")
public class ChamadoController {

    @GetMapping("/exemplo")
    public Chamado buscarExemplo() {
        Chamado chamado = new Chamado();

        chamado.setId(1L);
        chamado.setNumeroChamado("10179");
        chamado.setTipo(TipoChamado.INCIDENTE);
        chamado.setCategoria(CategoriaChamado.COMPUTADOR_COM_DEFEITO);
        chamado.setPrioridade(Prioridade.MEDIA);
        chamado.setDescricao("Computador da secretaria não liga.");

        return chamado;
    }
    @PostMapping
    public Chamado criarChamado(@RequestBody Chamado chamado) {
        return chamado;
    }
}