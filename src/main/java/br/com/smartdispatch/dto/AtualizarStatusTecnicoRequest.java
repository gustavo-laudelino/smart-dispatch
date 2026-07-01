package br.com.smartdispatch.dto;

public class AtualizarStatusTecnicoRequest {

    private Boolean ativo;

    public AtualizarStatusTecnicoRequest() {
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
}