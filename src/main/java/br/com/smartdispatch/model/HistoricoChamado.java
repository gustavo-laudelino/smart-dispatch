package br.com.smartdispatch.model;

import br.com.smartdispatch.enums.TipoEventoHistorico;

import java.time.LocalDateTime;

public class HistoricoChamado {

    private Long id;
    private Chamado chamado;
    private Usuario usuario;
    private TipoEventoHistorico tipoEvento;
    private String mensagem;
    private LocalDateTime dataHora;

    public HistoricoChamado() {
        this.dataHora = LocalDateTime.now();
    }

    public HistoricoChamado(
            Chamado chamado,
            Usuario usuario,
            TipoEventoHistorico tipoEvento,
            String mensagem
    ) {
        if (chamado == null) {
            throw new IllegalArgumentException("O chamado não pode ser nulo.");
        }

        if (usuario == null) {
            throw new IllegalArgumentException("O usuário não pode ser nulo.");
        }

        if (tipoEvento == null) {
            throw new IllegalArgumentException("O tipo do evento não pode ser nulo.");
        }

        if (mensagem == null || mensagem.isBlank()) {
            throw new IllegalArgumentException("A mensagem não pode estar vazia.");
        }

        this.chamado = chamado;
        this.usuario = usuario;
        this.tipoEvento = tipoEvento;
        this.mensagem = mensagem;
        this.dataHora = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Chamado getChamado() {
        return chamado;
    }

    public void setChamado(Chamado chamado) {
        this.chamado = chamado;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public TipoEventoHistorico getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(TipoEventoHistorico tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }
}