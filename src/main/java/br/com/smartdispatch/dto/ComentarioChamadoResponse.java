package br.com.smartdispatch.dto;

import java.time.LocalDateTime;

public class ComentarioChamadoResponse {

    private Long id;

    private Long chamadoId;

    private Long autorId;
    private String autorNome;

    private Long ordemServicoId;
    private String numeroOrdemServico;

    private String texto;
    private LocalDateTime dataCriacao;

    public ComentarioChamadoResponse() {
    }

    public ComentarioChamadoResponse(
            Long id,
            Long chamadoId,
            Long autorId,
            String autorNome,
            Long ordemServicoId,
            String numeroOrdemServico,
            String texto,
            LocalDateTime dataCriacao
    ) {
        this.id = id;
        this.chamadoId = chamadoId;
        this.autorId = autorId;
        this.autorNome = autorNome;
        this.ordemServicoId = ordemServicoId;
        this.numeroOrdemServico = numeroOrdemServico;
        this.texto = texto;
        this.dataCriacao = dataCriacao;
    }

    public Long getId() {
        return id;
    }

    public Long getChamadoId() {
        return chamadoId;
    }

    public Long getAutorId() {
        return autorId;
    }

    public String getAutorNome() {
        return autorNome;
    }

    public Long getOrdemServicoId() {
        return ordemServicoId;
    }

    public String getNumeroOrdemServico() {
        return numeroOrdemServico;
    }

    public String getTexto() {
        return texto;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }
}