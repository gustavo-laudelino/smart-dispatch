package br.com.smartdispatch.model;
import br.com.smartdispatch.enums.CategoriaChamado;
import br.com.smartdispatch.enums.Prioridade;
import br.com.smartdispatch.enums.StatusChamado;
import br.com.smartdispatch.enums.TipoChamado;

import java.time.LocalDateTime;

public class Chamado {

    private Long id;
    private String numeroChamado;

    private LocalDateTime dataAbertura;
    private LocalDateTime dataFinalizacao;
    private LocalDateTime dataAtribuicao;

    private Contrato contrato;
    private Unidade unidade;
    private Solicitante solicitante;
    private Patrimonio patrimonio;
    private Tecnico tecnicoResponsavel;

    private TipoChamado tipo;
    private CategoriaChamado categoria;
    private StatusChamado status;

    private Prioridade prioridade;

    private String descricao;



    public Chamado() {
        this.status = StatusChamado.ABERTO;
        this.dataAbertura = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroChamado() {
        return numeroChamado;
    }

    public void setNumeroChamado(String numeroChamado) {
        this.numeroChamado = numeroChamado;
    }

    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }

    public void setDataAbertura(LocalDateTime dataAbertura) {
        this.dataAbertura = dataAbertura;
    }

    public LocalDateTime getDataFinalizacao() { return dataFinalizacao; }

    public void setDataFinalizacao(LocalDateTime dataFinalizacao) { this.dataFinalizacao = dataFinalizacao; }

    public LocalDateTime getDataAtribuicao() { return dataAtribuicao; }

    public void setDataAtribuicao(LocalDateTime dataAtribuicao) { this.dataAtribuicao = dataAtribuicao; }


    public Contrato getContrato() {
        return contrato;
    }

    public void setContrato(Contrato contrato) {
        this.contrato = contrato;
    }

    public Unidade getUnidade() {
        return unidade;
    }

    public void setUnidade(Unidade unidade) {
        this.unidade = unidade;
    }

    public Solicitante getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(Solicitante solicitante) {
        this.solicitante = solicitante;
    }

    public Patrimonio getPatrimonio() {
        return patrimonio;
    }

    public void setPatrimonio(Patrimonio patrimonio) {
        this.patrimonio = patrimonio;
    }

    public Tecnico getTecnicoResponsavel() {
        return tecnicoResponsavel;
    }

    public void setTecnicoResponsavel(Tecnico tecnicoResponsavel) {
        this.tecnicoResponsavel = tecnicoResponsavel;
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

    public StatusChamado getStatus() {
        return status;
    }

    public void setStatus(StatusChamado status) {
        this.status = status;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(Prioridade prioridade) {
        this.prioridade = prioridade;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }


    public void atribuirTecnico(Tecnico tecnico) {
        if (tecnico == null) {
            throw new IllegalArgumentException("O técnico não pode ser nulo.");
        }

        this.dataAtribuicao = LocalDateTime.now();
        this.tecnicoResponsavel = tecnico;
        this.status = StatusChamado.ATRIBUIDO;
    }

    public void iniciarAtendimento() {
        if (this.tecnicoResponsavel == null) {
            throw new IllegalStateException(
                    "Não é possível iniciar um atendimento sem técnico responsável."
            );
        }

        this.status = StatusChamado.EM_ANDAMENTO;
    }

    public void aguardarPeca() {
        this.status = StatusChamado.AGUARDANDO_PECA;
    }

    public void aguardarCliente() {
        this.status = StatusChamado.AGUARDANDO_CLIENTE;
    }

    public void finalizar() {
        // Um chamado só pode ser finalizado se tiver sido atribuído
        // a um técnico. Chamados abertos por engano devem ser cancelados.
        if (this.tecnicoResponsavel == null) {
            throw new IllegalStateException(
                    "Não é possível finalizar um chamado sem um técnico responsável."
            );
        }

        this.status = StatusChamado.FINALIZADO;
        this.dataFinalizacao = LocalDateTime.now();
    }

    public void cancelar() {
        this.status = StatusChamado.CANCELADO;
    }

}
