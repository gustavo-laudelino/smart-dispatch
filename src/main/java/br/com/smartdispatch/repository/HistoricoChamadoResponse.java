package br.com.smartdispatch.dto;

import br.com.smartdispatch.enums.TipoEventoChamado;

import java.time.LocalDateTime;

public class HistoricoChamadoResponse {

    private Long id;
    private Long chamadoId;

    private Long ordemServicoId;
    private String numeroOrdemServico;

    private TipoEventoChamado tipoEvento;
    private String descricao;
    private LocalDateTime dataEvento;

    public HistoricoChamadoResponse() {
    }

    public HistoricoChamadoResponse(
            Long id,
            Long chamadoId,
            Long ordemServicoId,
            String numeroOrdemServico,
            TipoEventoChamado tipoEvento,
            String descricao,
            LocalDateTime dataEvento
    ) {
        this.id = id;
        this.chamadoId = chamadoId;
        this.ordemServicoId = ordemServicoId;
        this.numeroOrdemServico =
                numeroOrdemServico;
        this.tipoEvento = tipoEvento;
        this.descricao = descricao;
        this.dataEvento = dataEvento;
    }

    public Long getId() {
        return id;
    }

    public Long getChamadoId() {
        return chamadoId;
    }

    public Long getOrdemServicoId() {
        return ordemServicoId;
    }

    public String getNumeroOrdemServico() {
        return numeroOrdemServico;
    }

    public TipoEventoChamado getTipoEvento() {
        return tipoEvento;
    }

    public String getDescricao() {
        return descricao;
    }

    public LocalDateTime getDataEvento() {
        return dataEvento;
    }
}