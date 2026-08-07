package br.com.smartdispatch.dto;

import br.com.smartdispatch.enums.PerfilUsuario;

public class CriarUsuarioRequest {

    private String nome;
    private String email;
    private String telefone;
    private PerfilUsuario perfil;

    private Long contratoId;
    private Long baseOperacionalId;

    public CriarUsuarioRequest() {
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public PerfilUsuario getPerfil() {
        return perfil;
    }

    public void setPerfil(PerfilUsuario perfil) {
        this.perfil = perfil;
    }

    public Long getContratoId() {
        return contratoId;
    }

    public void setContratoId(Long contratoId) {
        this.contratoId = contratoId;
    }

    public Long getBaseOperacionalId() {
        return baseOperacionalId;
    }

    public void setBaseOperacionalId(Long baseOperacionalId) {
        this.baseOperacionalId = baseOperacionalId;
    }
}