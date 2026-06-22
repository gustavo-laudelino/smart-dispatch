package br.com.smartdispatch.service;

import br.com.smartdispatch.enums.StatusChamado;
import br.com.smartdispatch.enums.TipoEventoHistorico;
import br.com.smartdispatch.model.Chamado;
import br.com.smartdispatch.model.HistoricoChamado;
import br.com.smartdispatch.model.Tecnico;
import br.com.smartdispatch.model.Usuario;

import java.util.ArrayList;
import java.util.List;

public class ChamadoService {

    private final List<HistoricoChamado> historicos = new ArrayList<>();

    public void registrarCriacao(Chamado chamado, Usuario usuario) {
        registrarHistorico(
                chamado,
                usuario,
                TipoEventoHistorico.CHAMADO_CRIADO,
                "Chamado criado no Smart Dispatch."
        );
    }

    public void atribuirTecnico(
            Chamado chamado,
            Tecnico tecnico,
            Usuario usuarioResponsavel
    ) {
        chamado.atribuirTecnico(tecnico);

        registrarHistorico(
                chamado,
                usuarioResponsavel,
                TipoEventoHistorico.TECNICO_ATRIBUIDO,
                "Técnico " + tecnico.getUsuario().getNome()
                        + " atribuído ao chamado."
        );
    }

    private void registrarHistorico(
            Chamado chamado,
            Usuario usuario,
            TipoEventoHistorico tipoEvento,
            String mensagem
    ) {
        HistoricoChamado historico = new HistoricoChamado(
                chamado,
                usuario,
                tipoEvento,
                mensagem
        );

        historicos.add(historico);
    }

    public List<HistoricoChamado> getHistoricos() {
        return List.copyOf(historicos);
    }

    public void iniciarAtendimento(
            Chamado chamado,
            Usuario usuarioResponsavel
    ) {
        StatusChamado statusAnterior = chamado.getStatus();

        chamado.iniciarAtendimento();

        registrarAlteracaoStatus(
                chamado,
                usuarioResponsavel,
                statusAnterior
        );
    }

    public void finalizarAtendimento(
            Chamado chamado,
            Usuario usuarioResponsavel
    ) {
        StatusChamado statusAnterior = chamado.getStatus();

        chamado.finalizar();

        registrarAlteracaoStatus(
                chamado,
                usuarioResponsavel,
                statusAnterior
        );
    }

    private void registrarAlteracaoStatus(
            Chamado chamado,
            Usuario usuarioResponsavel,
            StatusChamado statusAnterior
    ) {
        registrarHistorico(
                chamado,
                usuarioResponsavel,
                TipoEventoHistorico.STATUS_ALTERADO,
                "Status alterado de "
                        + statusAnterior
                        + " para "
                        + chamado.getStatus()
                        + "."
        );
    }
}