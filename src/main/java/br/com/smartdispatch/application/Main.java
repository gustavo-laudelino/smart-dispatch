package br.com.smartdispatch.application;

import br.com.smartdispatch.enums.CategoriaChamado;
import br.com.smartdispatch.enums.PerfilUsuario;
import br.com.smartdispatch.enums.Prioridade;
import br.com.smartdispatch.enums.TipoChamado;
import br.com.smartdispatch.model.*;
import br.com.smartdispatch.service.ChamadoService;

public class Main {

    public static void main(String[] args) {

        // CONTRATO
        Contrato contratoCampinas = new Contrato();
        contratoCampinas.setId(1L);
        contratoCampinas.setCidade("Campinas");
        contratoCampinas.setSlaHoras(24);

        // BASE OPERACIONAL
        BaseOperacional baseCentro = new BaseOperacional();
        baseCentro.setId(1L);
        baseCentro.setNome("Base Centro");
        baseCentro.setEndereco("Rua Central, 100");
        baseCentro.setContrato(contratoCampinas);

        // USUÁRIO DO TÉCNICO
        Usuario usuarioGustavo = new Usuario();
        usuarioGustavo.setId(1L);
        usuarioGustavo.setNome("Gustavo");
        usuarioGustavo.setEmail("gustavo@email.com");
        usuarioGustavo.setTelefone("(19) 99999-9999");
        usuarioGustavo.setPerfil(PerfilUsuario.TECNICO);

        // TÉCNICO
        Tecnico gustavo = new Tecnico();
        gustavo.setId(1L);
        gustavo.setUsuario(usuarioGustavo);
        gustavo.setContrato(contratoCampinas);
        gustavo.setBaseOperacional(baseCentro);
        gustavo.setAtivo(true);

        // USUÁRIO INTERNO QUE CADASTROU O CHAMADO
        Usuario usuarioInterno = new Usuario();
        usuarioInterno.setId(2L);
        usuarioInterno.setNome("João Interno");
        usuarioInterno.setPerfil(PerfilUsuario.TECNICO_INTERNO);

        // USUÁRIO DO CTO QUE ATRIBUIRÁ O TÉCNICO
        Usuario usuarioCto = new Usuario();
        usuarioCto.setId(3L);
        usuarioCto.setNome("Maria CTO");
        usuarioCto.setPerfil(PerfilUsuario.CTO);

        // UNIDADE
        Unidade unidade = new Unidade();
        unidade.setId(1L);
        unidade.setNome("EMEB Maria Monteiro");
        unidade.setEndereco("Rua Exemplo, 123");
        unidade.setBairro("Centro");
        unidade.setCidade("Campinas");
        unidade.setContrato(contratoCampinas);

        // PATRIMÔNIO
        Patrimonio patrimonio = new Patrimonio();
        patrimonio.setNumeroPatrimonio("123456");

        // SOLICITANTE
        Solicitante solicitante = new Solicitante();
        solicitante.setNome("Ana Silva");
        solicitante.setEmail("ana@email.com");
        solicitante.setTelefone("(19) 98888-8888");
        solicitante.setMatricula("MAT001");

        // CHAMADO
        Chamado chamado = new Chamado();
        chamado.setId(1L);
        chamado.setNumeroChamado("10179");
        chamado.setContrato(contratoCampinas);
        chamado.setUnidade(unidade);
        chamado.setPatrimonio(patrimonio);
        chamado.setSolicitante(solicitante);
        chamado.setTipo(TipoChamado.INCIDENTE);
        chamado.setCategoria(CategoriaChamado.COMPUTADOR_COM_DEFEITO);
        chamado.setPrioridade(Prioridade.MEDIA);
        chamado.setDescricao("Computador da secretaria não liga.");

        // SERVIÇO
        ChamadoService chamadoService = new ChamadoService();

        // Registra que o usuário interno criou o chamado
        chamadoService.registrarCriacao(chamado, usuarioInterno);

        // O CTO atribui o chamado ao Gustavo
        chamadoService.atribuirTecnico(
                chamado,
                gustavo,
                usuarioCto
        );

        chamadoService.iniciarAtendimento(
                chamado,
                usuarioGustavo
        );

        chamadoService.finalizarAtendimento(
                chamado,
                usuarioGustavo
        );

        // MOSTRA O ESTADO ATUAL DO CHAMADO
        System.out.println("Número: " + chamado.getNumeroChamado());
        System.out.println("Status: " + chamado.getStatus());
        System.out.println(
                "Técnico: "
                        + chamado.getTecnicoResponsavel()
                        .getUsuario()
                        .getNome()
        );

        System.out.println("\nHISTÓRICO:");

        // Percorre cada objeto HistoricoChamado guardado na lista
        for (HistoricoChamado historico : chamadoService.getHistoricos()) {

            System.out.println(
                    historico.getDataHora()
                            + " | "
                            + historico.getUsuario().getNome()
                            + " | "
                            + historico.getTipoEvento()
                            + " | "
                            + historico.getMensagem()
            );
        }


    }
}