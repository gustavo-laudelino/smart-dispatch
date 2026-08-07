package br.com.smartdispatch.dto;

import br.com.smartdispatch.enums.PerfilUsuario;

public class UsuarioResponse {

    private Long id;
    private String nome;
    private String email;
    private String telefone;
    private PerfilUsuario perfil;
    private boolean ativo;
    private Long contratoId;
    private Long baseOperacionalId;

    public UsuarioResponse(
            Long id,
            String nome,
            String email,
            String telefone,
            PerfilUsuario perfil,
            boolean ativo,
            Long contratoId,
            Long baseOperacionalId
    ) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.perfil = perfil;
        this.ativo = ativo;
        this.contratoId = contratoId;
        this.baseOperacionalId = baseOperacionalId;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public PerfilUsuario getPerfil() {
        return perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Long getContratoId() {
        return contratoId;
    }

    public Long getBaseOperacionalId() {
        return baseOperacionalId;
    }

}