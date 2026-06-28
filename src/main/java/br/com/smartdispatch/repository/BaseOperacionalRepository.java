package br.com.smartdispatch.repository;

import br.com.smartdispatch.model.BaseOperacional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BaseOperacionalRepository
        extends JpaRepository<BaseOperacional, Long> {

    List<BaseOperacional> findByContratoId(Long contratoId);

    Optional<BaseOperacional> findByIdAndContratoId(
            Long baseId,
            Long contratoId
    );
}