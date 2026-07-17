package br.com.smartdispatch.enums;

public enum NivelIndicacao {

    LEVE(1),
    MODERADA(2),
    ALTA(3);

    private final int estrelas;

    NivelIndicacao(int estrelas) {
        this.estrelas = estrelas;
    }

    public int getEstrelas() {
        return estrelas;
    }
}