package br.com.smartdispatch.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Geração segura de números sequenciais automáticos. Nunca usa MAX+1
 * (condição de corrida sob concorrência) — numeroOrdemServico usa uma
 * SEQUENCE nativa do Postgres (global, atômica por definição); o contador
 * por contrato de numeroChamadoInterno usa SELECT ... FOR UPDATE, que
 * serializa concorrência apenas entre criações do mesmo contrato.
 */
@Service
public class NumeracaoService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public Long proximoNumeroOrdemServico() {
        Number valor = (Number) entityManager
                .createNativeQuery("SELECT nextval('seq_numero_os')")
                .getSingleResult();

        return valor.longValue();
    }

    @Transactional
    public Long proximoNumeroChamadoInterno(Long contratoId) {
        entityManager.createNativeQuery(
                        "INSERT INTO contrato_contadores "
                                + "(contrato_id, proximo_numero_chamado_interno) "
                                + "VALUES (:contratoId, 1) "
                                + "ON CONFLICT (contrato_id) DO NOTHING"
                )
                .setParameter("contratoId", contratoId)
                .executeUpdate();

        Number valorAtual = (Number) entityManager
                .createNativeQuery(
                        "SELECT proximo_numero_chamado_interno "
                                + "FROM contrato_contadores "
                                + "WHERE contrato_id = :contratoId "
                                + "FOR UPDATE"
                )
                .setParameter("contratoId", contratoId)
                .getSingleResult();

        entityManager.createNativeQuery(
                        "UPDATE contrato_contadores "
                                + "SET proximo_numero_chamado_interno = "
                                + "proximo_numero_chamado_interno + 1 "
                                + "WHERE contrato_id = :contratoId"
                )
                .setParameter("contratoId", contratoId)
                .executeUpdate();

        return valorAtual.longValue();
    }
}
