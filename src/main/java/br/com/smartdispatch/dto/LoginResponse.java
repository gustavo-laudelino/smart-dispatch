package br.com.smartdispatch.dto;

import br.com.smartdispatch.enums.PerfilUsuario;

public class LoginResponse {

    private String token;
    private String tipo;
    private long expiraEmSegundos;

    private Long usuarioId;
    private String nome;
    private String email;
    private PerfilUsuario perfil;

    public LoginResponse() {
    }

    public LoginResponse(
            String token,
            String tipo,
            long expiraEmSegundos,
            Long usuarioId,
            String nome,
            String email,
            PerfilUsuario perfil
    ) {
        this.token = token;
        this.tipo = tipo;
        this.expiraEmSegundos =
                expiraEmSegundos;
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.email = email;
        this.perfil = perfil;
    }

    public String getToken() {
        return token;
    }

    public void setToken(
            String token
    ) {
        this.token = token;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(
            String tipo
    ) {
        this.tipo = tipo;
    }

    public long getExpiraEmSegundos() {
        return expiraEmSegundos;
    }

    public void setExpiraEmSegundos(
            long expiraEmSegundos
    ) {
        this.expiraEmSegundos =
                expiraEmSegundos;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(
            Long usuarioId
    ) {
        this.usuarioId = usuarioId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(
            String nome
    ) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(
            String email
    ) {
        this.email = email;
    }

    public PerfilUsuario getPerfil() {
        return perfil;
    }

    public void setPerfil(
            PerfilUsuario perfil
    ) {
        this.perfil = perfil;
    }
}