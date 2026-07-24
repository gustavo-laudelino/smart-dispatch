package br.com.smartdispatch.model;

import br.com.smartdispatch.enums.TipoEventoChamado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "historicos_chamado",
        indexes = {
                @Index(
                        name = "idx_historico_chamado_data",
                        columnList = "chamado_id, data_evento"
                )
        }
)
public class HistoricoChamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(
            name = "chamado_id",
            nullable = false
    )
    private Chamado chamado;

    @ManyToOne
    @JoinColumn(name = "ordem_servico_id")
    private OrdemServico ordemServico;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "tipo_evento",
            nullable = false,
            length = 60
    )
    private TipoEventoChamado tipoEvento;

    @Column(
            nullable = false,
            length = 1000
    )
    private String descricao;

    @Column(
            name = "data_evento",
            nullable = false
    )
    private LocalDateTime dataEvento;

    public HistoricoChamado() {
    }

    @PrePersist
    public void definirDataEvento() {
        if (dataEvento == null) {
            dataEvento = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Chamado getChamado() {
        return chamado;
    }

    public void setChamado(
            Chamado chamado
    ) {
        this.chamado = chamado;
    }

    public OrdemServico getOrdemServico() {
        return ordemServico;
    }

    public void setOrdemServico(
            OrdemServico ordemServico
    ) {
        this.ordemServico = ordemServico;
    }

    public TipoEventoChamado getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(
            TipoEventoChamado tipoEvento
    ) {
        this.tipoEvento = tipoEvento;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(
            String descricao
    ) {
        this.descricao = descricao;
    }

    public LocalDateTime getDataEvento() {
        return dataEvento;
    }

    public void setDataEvento(
            LocalDateTime dataEvento
    ) {
        this.dataEvento = dataEvento;
    }
}