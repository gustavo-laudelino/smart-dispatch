package br.com.smartdispatch.application;
import br.com.smartdispatch.enums.CategoriaChamado;
import br.com.smartdispatch.enums.Prioridade;
import br.com.smartdispatch.enums.StatusChamado;
import br.com.smartdispatch.enums.TipoChamado;
import br.com.smartdispatch.model.*;

import java.time.LocalDateTime;

public class Main {

    public static void main(String[] args) {

        Contrato contratoCampinas = new Contrato();
        contratoCampinas.setId(1L);
        contratoCampinas.setCidade("Campinas");
        contratoCampinas.setSecretarioResponsavel("Secretário Municipal");
        contratoCampinas.setSlaHoras(24);

        BaseOperacional baseCentro = new BaseOperacional();
        baseCentro.setId(1L);
        baseCentro.setNome("Base Centro");
        baseCentro.setEndereco("Rua Central, 100");
        baseCentro.setContrato(contratoCampinas);

        Tecnico gustavo = new Tecnico();
        gustavo.setId(1L);
        gustavo.setNome("Gustavo");
        gustavo.setAtivo(true);
        gustavo.setContrato(contratoCampinas);
        gustavo.setBaseOperacional(baseCentro);

        Unidade emebMaria = new Unidade();
        emebMaria.setId(1L);
        emebMaria.setNome("EMEB Maria Monteiro");
        emebMaria.setEndereco("Rua Exemplo, 123");
        emebMaria.setBairro("Centro");
        emebMaria.setCidade("Campinas");
        emebMaria.setLatitude(-22.9000);
        emebMaria.setLongitude(-47.0600);
        emebMaria.setContrato(contratoCampinas);

        Patrimonio patrimonio = new Patrimonio();
        patrimonio.setNumeroPatrimonio("123456");

        Solicitante solicitante = new Solicitante();
        solicitante.setNome("Ana");
        solicitante.setEmail("ana@exemplo.com");
        solicitante.setTelefone("(19) 99999-9999");
        solicitante.setMatricula("98765");

        Chamado chamado = new Chamado();
        chamado.setId(1L);
        chamado.setNumeroChamado("10179");
        chamado.setDataAbertura(LocalDateTime.now());

        chamado.setContrato(contratoCampinas);
        chamado.setUnidade(emebMaria);
        chamado.setSolicitante(solicitante);
        chamado.setPatrimonio(patrimonio);

        chamado.setTipo(TipoChamado.INCIDENTE);
        chamado.setCategoria(CategoriaChamado.COMPUTADOR_COM_DEFEITO);
        chamado.setStatus(StatusChamado.ABERTO);
        chamado.setPrioridade(Prioridade.MEDIA);

        chamado.setDescricao("Computador da secretaria não liga.");

        System.out.println("Chamado criado: " + chamado.getNumeroChamado());
        System.out.println("Status: " + chamado.getStatus());
        System.out.println("Técnico responsável: " + chamado.getTecnicoResponsavel());

        chamado.setTecnicoResponsavel(gustavo);
        chamado.setStatus(StatusChamado.ATRIBUIDO);

        System.out.println("Técnico responsável: " + chamado.getTecnicoResponsavel().getNome());
        System.out.println("Status atualizado: " + chamado.getStatus());
    }
}