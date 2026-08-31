package br.com.smartdispatch.dto;

import br.com.smartdispatch.enums.CategoriaChamado;
import br.com.smartdispatch.enums.PrioridadeChamado;
import br.com.smartdispatch.enums.TipoChamado;
import br.com.smartdispatch.model.Solicitante;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ChamadoRequest {

    @NotBlank
    private String numeroChamado;

    @NotBlank
    private String linkChamadoOsti;

    @NotNull
    private Long unidadeId;

    private Solicitante solicitante;
    private String numeroPatrimonio;

    @NotNull
    private TipoChamado tipo;

    @NotNull
    private CategoriaChamado categoria;

    @NotNull
    private PrioridadeChamado prioridade;

    @NotBlank
    private String descricao;

    public ChamadoRequest() {
    }

    public String getNumeroChamado() {
        return numeroChamado;
    }

    public void setNumeroChamado(String numeroChamado) {
        this.numeroChamado = numeroChamado;
    }

    public String getLinkChamadoOsti() {
        return linkChamadoOsti;
    }

    public void setLinkChamadoOsti(String linkChamadoOsti) {
        this.linkChamadoOsti = linkChamadoOsti;
    }

    public Long getUnidadeId() {
        return unidadeId;
    }

    public void setUnidadeId(Long unidadeId) {
        this.unidadeId = unidadeId;
    }

    public Solicitante getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(Solicitante solicitante) {
        this.solicitante = solicitante;
    }

    public String getNumeroPatrimonio() {
        return numeroPatrimonio;
    }

    public void setNumeroPatrimonio(String numeroPatrimonio) {
        this.numeroPatrimonio = numeroPatrimonio;
    }

    public TipoChamado getTipo() {
        return tipo;
    }

    public void setTipo(TipoChamado tipo) {
        this.tipo = tipo;
    }

    public CategoriaChamado getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaChamado categoria) {
        this.categoria = categoria;
    }

    public PrioridadeChamado getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(PrioridadeChamado prioridade) {
        this.prioridade = prioridade;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}