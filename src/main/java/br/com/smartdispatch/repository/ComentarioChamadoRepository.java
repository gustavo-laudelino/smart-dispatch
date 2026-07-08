package br.com.smartdispatch.repository;

import br.com.smartdispatch.model.ComentarioChamado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComentarioChamadoRepository
        extends JpaRepository<ComentarioChamado, Long> {

    List<ComentarioChamado>
    findByChamadoIdOrderByDataCriacaoAsc(
            Long chamadoId
    );
}