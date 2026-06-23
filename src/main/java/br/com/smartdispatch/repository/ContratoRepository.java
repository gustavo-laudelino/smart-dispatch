package br.com.smartdispatch.repository;

import br.com.smartdispatch.model.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContratoRepository extends JpaRepository<Contrato, Long> {
}