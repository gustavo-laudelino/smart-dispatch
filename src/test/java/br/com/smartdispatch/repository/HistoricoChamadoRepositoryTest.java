package br.com.smartdispatch.repository;

import br.com.smartdispatch.enums.CategoriaChamado;
import br.com.smartdispatch.enums.PrioridadeChamado;
import br.com.smartdispatch.enums.StatusChamado;
import br.com.smartdispatch.enums.TipoChamado;
import br.com.smartdispatch.enums.TipoEventoChamado;
import br.com.smartdispatch.model.Chamado;
import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.model.HistoricoChamado;
import br.com.smartdispatch.model.Unidade;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class HistoricoChamadoRepositoryTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private HistoricoChamadoRepository historicoChamadoRepository;

    @Test
    void deveListarHistoricoDoChamadoEmOrdemCronologica() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado("CH-INT-001", unidadeA);

        HistoricoChamado historico1 = persistirHistorico(
                chamadoA,
                TipoEventoChamado.STATUS_ALTERADO,
                "Status alterado",
                LocalDateTime.of(2026, 8, 23, 11, 0)
        );
        HistoricoChamado historico2 = persistirHistorico(
                chamadoA,
                TipoEventoChamado.CHAMADO_CRIADO,
                "Chamado criado",
                LocalDateTime.of(2026, 8, 23, 9, 0)
        );
        HistoricoChamado historico3 = persistirHistorico(
                chamadoA,
                TipoEventoChamado.TECNICO_ATRIBUIDO,
                "Tecnico atribuido",
                LocalDateTime.of(2026, 8, 23, 10, 0)
        );

        entityManager.flush();

        Long chamadoAId = chamadoA.getId();
        Long contratoAId = contratoA.getId();
        Long idHistorico09h = historico2.getId();
        Long idHistorico10h = historico3.getId();
        Long idHistorico11h = historico1.getId();

        entityManager.clear();

        List<HistoricoChamado> resultado = historicoChamadoRepository
                .findByChamadoIdAndChamadoUnidadeContratoIdOrderByDataEventoAsc(
                        chamadoAId,
                        contratoAId
                );

        assertThat(resultado).hasSize(3);
        assertThat(resultado)
                .extracting(HistoricoChamado::getId)
                .containsExactly(
                        idHistorico09h,
                        idHistorico10h,
                        idHistorico11h
                );
    }

    @Test
    void deveListarSomenteHistoricoDoChamadoInformado() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado("CH-INT-002-A", unidadeA);
        Chamado chamadoB = persistirChamado("CH-INT-002-B", unidadeA);

        HistoricoChamado historicoA1 = persistirHistorico(
                chamadoA,
                TipoEventoChamado.CHAMADO_CRIADO,
                "Historico A1",
                LocalDateTime.of(2026, 8, 23, 9, 0)
        );
        HistoricoChamado historicoA2 = persistirHistorico(
                chamadoA,
                TipoEventoChamado.STATUS_ALTERADO,
                "Historico A2",
                LocalDateTime.of(2026, 8, 23, 10, 0)
        );
        persistirHistorico(
                chamadoB,
                TipoEventoChamado.CHAMADO_CRIADO,
                "Historico B1",
                LocalDateTime.of(2026, 8, 23, 9, 0)
        );

        entityManager.flush();

        Long chamadoAId = chamadoA.getId();
        Long contratoAId = contratoA.getId();
        Long idHistoricoA1 = historicoA1.getId();
        Long idHistoricoA2 = historicoA2.getId();

        entityManager.clear();

        List<HistoricoChamado> resultado = historicoChamadoRepository
                .findByChamadoIdAndChamadoUnidadeContratoIdOrderByDataEventoAsc(
                        chamadoAId,
                        contratoAId
                );

        assertThat(resultado).hasSize(2);
        assertThat(resultado)
                .extracting(HistoricoChamado::getId)
                .containsExactlyInAnyOrder(idHistoricoA1, idHistoricoA2);
    }

    @Test
    void deveRetornarListaVaziaQuandoContratoNaoCorresponde() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado("CH-INT-003", unidadeA);

        persistirHistorico(
                chamadoA,
                TipoEventoChamado.CHAMADO_CRIADO,
                "Historico A1",
                LocalDateTime.of(2026, 8, 23, 9, 0)
        );

        entityManager.flush();

        Long chamadoAId = chamadoA.getId();
        Long contratoBId = contratoB.getId();

        entityManager.clear();

        List<HistoricoChamado> resultado = historicoChamadoRepository
                .findByChamadoIdAndChamadoUnidadeContratoIdOrderByDataEventoAsc(
                        chamadoAId,
                        contratoBId
                );

        assertThat(resultado).isEmpty();
    }

    private Contrato persistirContrato() {
        Contrato contrato = new Contrato();
        entityManager.persist(contrato);
        return contrato;
    }

    private Unidade persistirUnidade(Contrato contrato) {
        Unidade unidade = new Unidade();
        unidade.setContrato(contrato);
        entityManager.persist(unidade);
        return unidade;
    }

    private Chamado persistirChamado(String numeroChamado, Unidade unidade) {
        Chamado chamado = new Chamado();
        chamado.setNumeroChamado(numeroChamado);
        chamado.setLinkChamadoOsti(
                "https://teste.local/chamado/" + numeroChamado
        );
        chamado.setUnidade(unidade);
        chamado.setTipo(TipoChamado.INCIDENTE);
        chamado.setCategoria(CategoriaChamado.OUTROS);
        chamado.setPrioridade(PrioridadeChamado.MEDIA);
        chamado.setStatus(StatusChamado.ABERTO);
        chamado.setDescricao("Chamado para teste de integração");
        chamado.setDataAbertura(
                LocalDateTime.of(2026, 8, 23, 8, 0)
        );
        entityManager.persist(chamado);
        return chamado;
    }

    private HistoricoChamado persistirHistorico(
            Chamado chamado,
            TipoEventoChamado tipoEvento,
            String descricao,
            LocalDateTime dataEvento
    ) {
        HistoricoChamado historico = new HistoricoChamado();
        historico.setChamado(chamado);
        historico.setTipoEvento(tipoEvento);
        historico.setDescricao(descricao);
        historico.setDataEvento(dataEvento);
        entityManager.persist(historico);
        return historico;
    }
}
