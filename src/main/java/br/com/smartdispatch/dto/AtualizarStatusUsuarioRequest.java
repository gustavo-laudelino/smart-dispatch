package br.com.smartdispatch.dto;
import br.com.smartdispatch.enums.PerfilUsuario;


public class AtualizarStatusUsuarioRequest {

    private Long contratoId;
    private Long baseOperacionalId;
    private Boolean ativo;
    private PerfilUsuario perfil;

    public AtualizarStatusUsuarioRequest() {
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
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

    public PerfilUsuario getPerfil() {
        return perfil;
    }

    public void setPerfil(PerfilUsuario perfil) {
        this.perfil = perfil;
    }
}