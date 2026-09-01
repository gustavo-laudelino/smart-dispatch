package br.com.smartdispatch.repository;

import br.com.smartdispatch.model.Chamado;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {

    @EntityGraph(attributePaths = {"unidade", "unidade.contrato"})
    List<Chamado> findAll();

    @EntityGraph(attributePaths = {"unidade", "unidade.contrato"})
    Page<Chamado> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"unidade", "unidade.contrato"})
    Page<Chamado> findByUnidadeContratoId(
            Long contratoId,
            Pageable pageable
    );

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

    @EntityGraph(attributePaths = {"unidade", "unidade.contrato"})
    @Query("""
            SELECT c FROM Chamado c
            WHERE (:contratoId IS NULL OR c.unidade.contrato.id = :contratoId)
            AND EXISTS (
                SELECT 1 FROM OrdemServico os
                WHERE os.chamado = c
                AND os.tecnico.id = :tecnicoId
            )
            """)
    Page<Chamado> findByTecnicoAtualEContratoOpcional(
            @Param("tecnicoId") Long tecnicoId,
            @Param("contratoId") Long contratoId,
            Pageable pageable
    );

}