package br.com.smartdispatch.repository;

import br.com.smartdispatch.model.OrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrdemServicoRepository
        extends JpaRepository<OrdemServico, Long> {

    List<OrdemServico> findByChamadoId(Long chamadoId);

    Optional<OrdemServico>
    findByTecnicoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(
            Long tecnicoId
    );

    boolean existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(
            Long chamadoId
    );

    boolean existsByChamadoIdAndTecnicoIsNotNullAndDataCheckInIsNullAndDataCheckOutIsNull(
            Long chamadoId
    );

    boolean existsByChamadoIdAndTecnicoIsNullAndDataCheckInIsNullAndDataCheckOutIsNull(
            Long chamadoId
    );

    boolean existsByNumeroOrdemServico(
            String numeroOrdemServico
    );

    boolean existsByNumeroOrdemServicoAndIdNot(
            String numeroOrdemServico,
            Long ordemServicoId
    );

    Optional<OrdemServico>
    findByIdAndChamadoIdAndChamadoUnidadeContratoId(
            Long ordemServicoId,
            Long chamadoId,
            Long contratoId
    );


}