package br.com.smartdispatch.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contratos")
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cidade;
    private String secretarioResponsavel;
    private Integer slaHoras;
    private String linkPortalChamados;

    public Contrato() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getSecretarioResponsavel() {
        return secretarioResponsavel;
    }

    public void setSecretarioResponsavel(String secretarioResponsavel) {
        this.secretarioResponsavel = secretarioResponsavel;
    }

    public Integer getSlaHoras() {
        return slaHoras;
    }

    public void setSlaHoras(Integer slaHoras) {
        this.slaHoras = slaHoras;
    }

    public String getLinkPortalChamados() {
        return linkPortalChamados;
    }

    public void setLinkPortalChamados(String linkPortalChamados) {
        this.linkPortalChamados = linkPortalChamados;
    }
}