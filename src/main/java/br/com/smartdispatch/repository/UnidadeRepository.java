package br.com.smartdispatch.repository;
import java.util.Optional;
import br.com.smartdispatch.model.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnidadeRepository extends JpaRepository<Unidade, Long> {

    List<Unidade> findByContratoId(Long contratoId);

    Optional<Unidade> findByIdAndContratoId(
            Long unidadeId,
            Long contratoId
    );
}