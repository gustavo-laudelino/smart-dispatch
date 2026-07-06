package br.com.smartdispatch.dto;

public class OrdemServicoRequest {

    private String numeroOrdemServico;
    private Long tecnicoId;
    private Long unidadeAtendimentoId;

    public OrdemServicoRequest() {
    }

    public String getNumeroOrdemServico() {
        return numeroOrdemServico;
    }

    public void setNumeroOrdemServico(String numeroOrdemServico) {
        this.numeroOrdemServico = numeroOrdemServico;
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

}