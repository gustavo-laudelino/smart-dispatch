package br.com.smartdispatch.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class OrdemServicoResponse {

    private Long id;
    private Long numeroOrdemServico;

    private Long contratoId;
    private String contratoCidade;

    private Long chamadoId;
    private String numeroChamado;

    private String descricao;
    private String numeroPatrimonio;

    private Long tecnicoId;
    private String tecnicoNome;

    private LocalDate data;
    private LocalTime hora;

    private LocalDateTime dataAtribuicaoTecnico;

    private Long unidadeAtendimentoId;
    private String unidadeAtendimentoNome;

    private LocalDateTime dataCheckIn;
    private LocalDateTime dataCheckOut;

    public OrdemServicoResponse() {
    }

    public OrdemServicoResponse(
            Long id,
            Long numeroOrdemServico,
            Long contratoId,
            String contratoCidade,
            Long chamadoId,
            String numeroChamado,
            String descricao,
            String numeroPatrimonio,
            Long tecnicoId,
            String tecnicoNome,
            LocalDate data,
            LocalTime hora,
            LocalDateTime dataAtribuicaoTecnico,
            Long unidadeAtendimentoId,
            String unidadeAtendimentoNome,
            LocalDateTime dataCheckIn,
            LocalDateTime dataCheckOut
    ) {
        this.id = id;
        this.numeroOrdemServico = numeroOrdemServico;
        this.contratoId = contratoId;
        this.contratoCidade = contratoCidade;
        this.chamadoId = chamadoId;
        this.numeroChamado = numeroChamado;
        this.descricao = descricao;
        this.numeroPatrimonio = numeroPatrimonio;
        this.tecnicoId = tecnicoId;
        this.tecnicoNome = tecnicoNome;
        this.data = data;
        this.hora = hora;
        this.dataAtribuicaoTecnico = dataAtribuicaoTecnico;
        this.unidadeAtendimentoId = unidadeAtendimentoId;
        this.unidadeAtendimentoNome = unidadeAtendimentoNome;
        this.dataCheckIn = dataCheckIn;
        this.dataCheckOut = dataCheckOut;
    }

    public Long getId() {
        return id;
    }

    public Long getNumeroOrdemServico() {
        return numeroOrdemServico;
    }

    public Long getContratoId() {
        return contratoId;
    }

    public String getContratoCidade() {
        return contratoCidade;
    }

    public Long getChamadoId() {
        return chamadoId;
    }

    public String getNumeroChamado() {
        return numeroChamado;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getNumeroPatrimonio() {
        return numeroPatrimonio;
    }

    public Long getTecnicoId() {
        return tecnicoId;
    }

    public String getTecnicoNome() {
        return tecnicoNome;
    }

    public LocalDate getData() {
        return data;
    }

    public LocalTime getHora() {
        return hora;
    }

    public LocalDateTime getDataAtribuicaoTecnico() {
        return dataAtribuicaoTecnico;
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
