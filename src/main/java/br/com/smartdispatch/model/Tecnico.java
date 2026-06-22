package br.com.smartdispatch.model;

import br.com.smartdispatch.enums.PerfilUsuario;

public class Tecnico {

    private Long id;

    private Usuario usuario;
    private Contrato contrato;
    private BaseOperacional baseOperacional;
    private PerfilUsuario perfilUsuario;

    private boolean ativo;

    public Tecnico() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Contrato getContrato() {
        return contrato;
    }

    public void setContrato(Contrato contrato) {
        this.contrato = contrato;
    }

    public BaseOperacional getBaseOperacional() {
        return baseOperacional;
    }

    public void setBaseOperacional(BaseOperacional baseOperacional) {
        this.baseOperacional = baseOperacional;
    }

    public PerfilUsuario getPerfilUsuario() {
        return perfilUsuario;
    }

    public void setPerfilUsuario(PerfilUsuario perfilUsuario) {
        this.perfilUsuario = perfilUsuario;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}