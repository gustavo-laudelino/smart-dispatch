package br.com.smartdispatch.repository;

import br.com.smartdispatch.model.Chamado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {

    List<Chamado> findByUnidadeContratoId(Long contratoId);

    Optional<Chamado> findByIdAndUnidadeContratoId(
            Long chamadoId,
            Long contratoId
    );

    boolean existsByNumeroChamadoAndUnidadeContratoId(
            String numeroChamado,
            Long contratoId
    );

    boolean existsByNumeroChamadoAndUnidadeContratoIdAndIdNot(
            String numeroChamado,
            Long contratoId,
            Long chamadoId
    );
}