package br.com.smartdispatch.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class OrdemServicoRequest {

    private Long chamadoId;
    private Long tecnicoId;
    private Long unidadeAtendimentoId;
    private String descricao;
    private String numeroPatrimonio;
    private LocalDate data;
    private LocalTime hora;

    public OrdemServicoRequest() {
    }

    public Long getChamadoId() {
        return chamadoId;
    }

    public void setChamadoId(Long chamadoId) {
        this.chamadoId = chamadoId;
    }

    public Long getTecnicoId() {
        return tecnicoId;
    }

    public void setTecnicoId(Long tecnicoId) {
        this.tecnicoId = tecnicoId;
    }

    public Long getUnidadeAtendimentoId() {
        return unidadeAtendimentoId;
    }

    public void setUnidadeAtendimentoId(Long unidadeAtendimentoId) {
        this.unidadeAtendimentoId = unidadeAtendimentoId;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getNumeroPatrimonio() {
        return numeroPatrimonio;
    }

    public void setNumeroPatrimonio(String numeroPatrimonio) {
        this.numeroPatrimonio = numeroPatrimonio;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }
}
