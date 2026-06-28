package br.com.smartdispatch.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tecnicos")
public class Tecnico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(
            name = "usuario_id",
            nullable = false,
            unique = true
    )
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(
            name = "base_operacional_id",
            nullable = false
    )
    private BaseOperacional baseOperacional;

    @Column(nullable = false)
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

    public BaseOperacional getBaseOperacional() {
        return baseOperacional;
    }

    public void setBaseOperacional(BaseOperacional baseOperacional) {
        this.baseOperacional = baseOperacional;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}