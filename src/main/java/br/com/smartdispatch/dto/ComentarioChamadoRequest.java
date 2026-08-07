package br.com.smartdispatch.dto;

public class ComentarioChamadoRequest {

    private Long ordemServicoId;
    private String texto;

    public ComentarioChamadoRequest() {
    }

    public Long getOrdemServicoId() {
        return ordemServicoId;
    }

    public void setOrdemServicoId(Long ordemServicoId) {
        this.ordemServicoId = ordemServicoId;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }
}