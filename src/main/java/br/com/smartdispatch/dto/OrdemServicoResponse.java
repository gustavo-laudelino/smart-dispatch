package br.com.smartdispatch.dto;

import java.time.LocalDateTime;

public class OrdemServicoResponse {

    private Long id;
    private String numeroOrdemServico;

    private Long chamadoId;
    private String numeroChamado;

    private Long tecnicoId;
    private String tecnicoNome;

    private Long unidadeAtendimentoId;
    private String unidadeAtendimentoNome;

    private LocalDateTime dataCheckIn;
    private LocalDateTime dataCheckOut;

    public OrdemServicoResponse() {
    }

    public OrdemServicoResponse(
            Long id,
            String numeroOrdemServico,
            Long chamadoId,
            String numeroChamado,
            Long tecnicoId,
            String tecnicoNome,
            Long unidadeAtendimentoId,
            String unidadeAtendimentoNome,
            LocalDateTime dataCheckIn,
            LocalDateTime dataCheckOut
    ) {
        this.id = id;
        this.numeroOrdemServico = numeroOrdemServico;
        this.chamadoId = chamadoId;
        this.numeroChamado = numeroChamado;
        this.tecnicoId = tecnicoId;
        this.tecnicoNome = tecnicoNome;
        this.unidadeAtendimentoId = unidadeAtendimentoId;
        this.unidadeAtendimentoNome = unidadeAtendimentoNome;
        this.dataCheckIn = dataCheckIn;
        this.dataCheckOut = dataCheckOut;
    }

    public Long getId() {
        return id;
    }

    public String getNumeroOrdemServico() {
        return numeroOrdemServico;
    }

    public Long getChamadoId() {
        return chamadoId;
    }

    public String getNumeroChamado() {
        return numeroChamado;
    }

    public Long getTecnicoId() {
        return tecnicoId;
    }

    public String getTecnicoNome() {
        return tecnicoNome;
    }

    public Long getUnidadeAtendimentoId() {
        return unidadeAtendimentoId;
    }

    public String getUnidadeAtendimentoNome() {
        return unidadeAtendimentoNome;
    }

    public LocalDateTime getDataCheckIn() {
        return dataCheckIn;
    }

    public LocalDateTime getDataCheckOut() {
        return dataCheckOut;
    }
}