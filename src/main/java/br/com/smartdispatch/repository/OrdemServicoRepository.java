package br.com.smartdispatch.repository;

import br.com.smartdispatch.model.OrdemServico;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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

    // --- Rotas antigas, aninhadas em Chamado — preservadas para a transição ---

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

    // --- Rota canônica, por contrato — OS com ou sem Chamado ---

    @EntityGraph(attributePaths = {
            "chamado", "tecnico", "tecnico.usuario",
            "unidadeAtendimento", "unidadeAtendimento.contrato"
    })
    Optional<OrdemServico>
    findByIdAndUnidadeAtendimentoContratoId(
            Long ordemServicoId,
            Long contratoId
    );

    boolean existsByIdAndTecnicoUsuarioIdAndUnidadeAtendimentoContratoId(
            Long ordemServicoId,
            Long usuarioId,
            Long contratoId
    );

    @EntityGraph(attributePaths = {
            "chamado", "tecnico", "tecnico.usuario",
            "unidadeAtendimento", "unidadeAtendimento.contrato"
    })
    @Query("""
            SELECT os FROM OrdemServico os
            WHERE os.unidadeAtendimento.contrato.id = :contratoId
            AND (:chamadoId IS NULL OR os.chamado.id = :chamadoId)
            AND (:tecnicoId IS NULL OR os.tecnico.id = :tecnicoId)
            AND os.data = COALESCE(:data, os.data)
            AND os.data >= COALESCE(:dataInicio, os.data)
            AND os.data <= COALESCE(:dataFim, os.data)
            ORDER BY os.data ASC, os.hora ASC NULLS LAST, os.id ASC
            """)
    List<OrdemServico> buscarPorContratoComFiltros(
            @Param("contratoId") Long contratoId,
            @Param("chamadoId") Long chamadoId,
            @Param("tecnicoId") Long tecnicoId,
            @Param("data") LocalDate data,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim
    );

    // --- Usadas por SugestaoTecnicoService — escopadas pelo contrato da
    // própria OS (via unidadeAtendimento, sempre presente), não pelo
    // contrato do chamado. Funciona igual para OS com ou sem chamado, e é
    // equivalente ao comportamento anterior: unidadeAtendimento sempre foi
    // validada contra o mesmo contratoId do chamado na criação. ---

    List<OrdemServico>
    findByTecnicoIdAndUnidadeAtendimentoContratoIdAndDataCheckOutIsNull(
            Long tecnicoId,
            Long contratoId
    );

    long countByTecnicoIdAndUnidadeAtendimentoContratoIdAndDataCheckOutGreaterThanEqual(
            Long tecnicoId,
            Long contratoId,
            LocalDateTime dataInicial
    );

    long countByTecnicoIdAndUnidadeAtendimentoContratoIdAndIdNotAndDataAtribuicaoTecnicoGreaterThanEqualAndDataAtribuicaoTecnicoLessThan(
            Long tecnicoId,
            Long contratoId,
            Long ordemServicoIdIgnorada,
            LocalDateTime inicio,
            LocalDateTime fim
    );
}
