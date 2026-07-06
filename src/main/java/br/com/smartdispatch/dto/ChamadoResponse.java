package br.com.smartdispatch.dto;

import br.com.smartdispatch.enums.CategoriaChamado;
import br.com.smartdispatch.enums.PrioridadeChamado;
import br.com.smartdispatch.enums.StatusChamado;
import br.com.smartdispatch.enums.TipoChamado;
import br.com.smartdispatch.model.Solicitante;

import java.time.LocalDateTime;

public class ChamadoResponse {

    private Long id;
    private String numeroChamado;
    private String linkChamadoOsti;

    private Long unidadeId;
    private String unidadeNome;

    private Long contratoId;
    private String contratoCidade;

    private Solicitante solicitante;
    private String numeroPatrimonio;

    private TipoChamado tipo;
    private CategoriaChamado categoria;
    private PrioridadeChamado prioridade;
    private StatusChamado status;

    private String descricao;

    private LocalDateTime dataAbertura;
    private LocalDateTime dataFinalizacao;

    public ChamadoResponse() {
    }

    public ChamadoResponse(
            Long id,
            String numeroChamado,
            String linkChamadoOsti,
            Long unidadeId,
            String unidadeNome,
            Long contratoId,
            String contratoCidade,
            Solicitante solicitante,
            String numeroPatrimonio,
            TipoChamado tipo,
            CategoriaChamado categoria,
            PrioridadeChamado prioridade,
            StatusChamado status,
            String descricao,
            LocalDateTime dataAbertura,
            LocalDateTime dataFinalizacao
    ) {
        this.id = id;
        this.numeroChamado = numeroChamado;
        this.linkChamadoOsti = linkChamadoOsti;
        this.unidadeId = unidadeId;
        this.unidadeNome = unidadeNome;
        this.contratoId = contratoId;
        this.contratoCidade = contratoCidade;
        this.solicitante = solicitante;
        this.numeroPatrimonio = numeroPatrimonio;
        this.tipo = tipo;
        this.categoria = categoria;
        this.prioridade = prioridade;
        this.status = status;
        this.descricao = descricao;
        this.dataAbertura = dataAbertura;
        this.dataFinalizacao = dataFinalizacao;
    }

    public Long getId() {
        return id;
    }

    public String getNumeroChamado() {
        return numeroChamado;
    }

    public String getLinkChamadoOsti() {
        return linkChamadoOsti;
    }

    public Long getUnidadeId() {
        return unidadeId;
    }

    public String getUnidadeNome() {
        return unidadeNome;
    }

    public Long getContratoId() {
        return contratoId;
    }

    public String getContratoCidade() {
        return contratoCidade;
    }

    public Solicitante getSolicitante() {
        return solicitante;
    }

    public String getNumeroPatrimonio() {
        return numeroPatrimonio;
    }

    public TipoChamado getTipo() {
        return tipo;
    }

    public CategoriaChamado getCategoria() {
        return categoria;
    }

    public PrioridadeChamado getPrioridade() {
        return prioridade;
    }

    public StatusChamado getStatus() {
        return status;
    }

    public String getDescricao() {
        return descricao;
    }

    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }

    public LocalDateTime getDataFinalizacao() {
        return dataFinalizacao;
    }
}