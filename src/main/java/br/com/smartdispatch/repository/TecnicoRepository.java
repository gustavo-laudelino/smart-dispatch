package br.com.smartdispatch.repository;

import br.com.smartdispatch.model.Tecnico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TecnicoRepository extends JpaRepository<Tecnico, Long> {

    List<Tecnico> findByBaseOperacionalId(Long baseId);

    Optional<Tecnico> findByIdAndBaseOperacionalId(
            Long tecnicoId,
            Long baseId
    );
}