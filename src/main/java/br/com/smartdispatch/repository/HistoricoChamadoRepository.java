package br.com.smartdispatch.repository;

import br.com.smartdispatch.model.HistoricoChamado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoChamadoRepository
        extends JpaRepository<HistoricoChamado, Long> {

    List<HistoricoChamado>
    findByChamadoIdAndChamadoUnidadeContratoIdOrderByDataEventoAsc(
            Long chamadoId,
            Long contratoId
    );
}