package br.com.smartdispatch.dto;

import br.com.smartdispatch.enums.NivelIndicacao;

public class SugestaoTecnicoResponse {

    private Long tecnicoId;
    private String tecnicoNome;

    private double pontuacao;
    private double distanciaKm;

    private int quantidadeOsAtivas;
    private int atribuicoesHoje;
    private int atendimentosUltimos15Dias;

    private NivelIndicacao nivelIndicacao;
    private int estrelas;

    public SugestaoTecnicoResponse() {
    }

    public SugestaoTecnicoResponse(
            Long tecnicoId,
            String tecnicoNome,
            double pontuacao,
            double distanciaKm,
            int quantidadeOsAtivas,
            int atribuicoesHoje,
            int atendimentosUltimos15Dias,
            NivelIndicacao nivelIndicacao
    ) {
        this.tecnicoId = tecnicoId;
        this.tecnicoNome = tecnicoNome;
        this.pontuacao = pontuacao;
        this.distanciaKm = distanciaKm;
        this.quantidadeOsAtivas =
                quantidadeOsAtivas;
        this.atribuicoesHoje =
                atribuicoesHoje;
        this.atendimentosUltimos15Dias =
                atendimentosUltimos15Dias;
        this.nivelIndicacao =
                nivelIndicacao;
        this.estrelas =
                nivelIndicacao.getEstrelas();
    }

    public Long getTecnicoId() {
        return tecnicoId;
    }

    public String getTecnicoNome() {
        return tecnicoNome;
    }

    public double getPontuacao() {
        return pontuacao;
    }

    public double getDistanciaKm() {
        return distanciaKm;
    }

    public int getQuantidadeOsAtivas() {
        return quantidadeOsAtivas;
    }

    public int getAtribuicoesHoje() {
        return atribuicoesHoje;
    }

    public int getAtendimentosUltimos15Dias() {
        return atendimentosUltimos15Dias;
    }

    public NivelIndicacao getNivelIndicacao() {
        return nivelIndicacao;
    }

    public int getEstrelas() {
        return estrelas;
    }
}