package br.com.smartdispatch.dto;

public class AtualizarStatusUsuarioRequest {

    private Boolean ativo;

    public AtualizarStatusUsuarioRequest() {
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }
}