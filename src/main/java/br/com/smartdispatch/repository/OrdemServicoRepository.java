package br.com.smartdispatch.repository;

import br.com.smartdispatch.model.OrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import br.com.smartdispatch.enums.StatusChamado;

public interface OrdemServicoRepository
        extends JpaRepository<OrdemServico, Long> {

    List<OrdemServico> findByChamadoId(
            Long chamadoId
    );

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

    boolean existsByIdAndTecnicoUsuarioIdAndChamadoIdAndChamadoUnidadeContratoIdAndChamadoStatusNot(
            Long ordemServicoId,
            Long usuarioId,
            Long chamadoId,
            Long contratoId,
            StatusChamado status
    );

    Optional<OrdemServico>
    findByIdAndChamadoIdAndChamadoUnidadeContratoId(
            Long ordemServicoId,
            Long chamadoId,
            Long contratoId
    );

    List<OrdemServico>
    findByTecnicoIdAndChamadoUnidadeContratoIdAndDataCheckOutIsNull(
            Long tecnicoId,
            Long contratoId
    );

    long countByTecnicoIdAndChamadoUnidadeContratoIdAndDataCheckOutGreaterThanEqual(
            Long tecnicoId,
            Long contratoId,
            LocalDateTime dataInicial
    );

    long countByTecnicoIdAndChamadoUnidadeContratoIdAndIdNotAndDataAtribuicaoTecnicoGreaterThanEqualAndDataAtribuicaoTecnicoLessThan(
            Long tecnicoId,
            Long contratoId,
            Long ordemServicoIdIgnorada,
            LocalDateTime inicio,
            LocalDateTime fim
    );
}