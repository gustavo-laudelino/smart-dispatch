package br.com.smartdispatch.repository;

import br.com.smartdispatch.enums.CategoriaChamado;
import br.com.smartdispatch.enums.PrioridadeChamado;
import br.com.smartdispatch.enums.StatusChamado;
import br.com.smartdispatch.enums.TipoChamado;
import br.com.smartdispatch.model.Chamado;
import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.model.OrdemServico;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class OrdemServicoRepositoryTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private OrdemServicoRepository ordemServicoRepository;

    @Test
    void deveBuscarOrdensDeServicoPorChamadoId() {
        Contrato contrato = new Contrato();
        entityManager.persist(contrato);

        Unidade unidade = new Unidade();
        unidade.setContrato(contrato);
        entityManager.persist(unidade);

        Chamado chamado = new Chamado();
        chamado.setNumeroChamado("CH-INT-001");
        chamado.setLinkChamadoOsti(
                "https://teste.local/chamado/CH-INT-001"
        );
        chamado.setUnidade(unidade);
        chamado.setTipo(TipoChamado.INCIDENTE);
        chamado.setCategoria(CategoriaChamado.OUTROS);
        chamado.setPrioridade(PrioridadeChamado.MEDIA);
        chamado.setStatus(StatusChamado.ABERTO);
        chamado.setDescricao("Chamado para teste de integração");
        chamado.setDataAbertura(
                LocalDateTime.of(2026, 8, 23, 10, 0)
        );
        entityManager.persist(chamado);

        OrdemServico ordemServico = new OrdemServico();
        ordemServico.setNumeroOrdemServico("OS-INT-001");
        ordemServico.setChamado(chamado);
        ordemServico.setUnidadeAtendimento(unidade);
        entityManager.persist(ordemServico);

        entityManager.flush();

        Long chamadoId = chamado.getId();
        Long ordemServicoId = ordemServico.getId();

        entityManager.clear();

        List<OrdemServico> resultado =
                ordemServicoRepository.findByChamadoId(chamadoId);

        assertThat(resultado).hasSize(1);

        OrdemServico ordemServicoEncontrada = resultado.get(0);

        assertThat(ordemServicoEncontrada.getId())
                .isEqualTo(ordemServicoId);
        assertThat(ordemServicoEncontrada.getNumeroOrdemServico())
                .isEqualTo("OS-INT-001");
        assertThat(ordemServicoEncontrada.getChamado()).isNotNull();
        assertThat(ordemServicoEncontrada.getChamado().getId())
                .isEqualTo(chamadoId);
    }

    @Test
    void deveBuscarOrdemQuandoIdChamadoEContratoCorrespondem() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado("CH-INT-002", unidadeA);
        OrdemServico ordemServicoA =
                persistirOrdemServico("OS-INT-002", chamadoA, unidadeA);

        entityManager.flush();

        Long ordemServicoId = ordemServicoA.getId();
        Long chamadoId = chamadoA.getId();
        Long contratoId = contratoA.getId();

        entityManager.clear();

        Optional<OrdemServico> resultado = ordemServicoRepository
                .findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId,
                        chamadoId,
                        contratoId
                );

        assertThat(resultado).isPresent();

        OrdemServico ordemServicoEncontrada = resultado.get();

        assertThat(ordemServicoEncontrada.getId())
                .isEqualTo(ordemServicoId);
        assertThat(ordemServicoEncontrada.getChamado().getId())
                .isEqualTo(chamadoId);
        assertThat(
                ordemServicoEncontrada.getChamado()
                        .getUnidade()
                        .getContrato()
                        .getId()
        ).isEqualTo(contratoId);
    }

    @Test
    void deveRetornarVazioQuandoContratoNaoCorresponde() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado("CH-INT-003", unidadeA);
        OrdemServico ordemServicoA =
                persistirOrdemServico("OS-INT-003", chamadoA, unidadeA);

        entityManager.flush();

        Long ordemServicoId = ordemServicoA.getId();
        Long chamadoId = chamadoA.getId();
        Long contratoIdIncorreto = contratoB.getId();

        entityManager.clear();

        Optional<OrdemServico> resultado = ordemServicoRepository
                .findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId,
                        chamadoId,
                        contratoIdIncorreto
                );

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveRetornarVazioQuandoChamadoNaoCorresponde() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado("CH-INT-004-A", unidadeA);
        Chamado chamadoB = persistirChamado("CH-INT-004-B", unidadeA);
        OrdemServico ordemServicoA =
                persistirOrdemServico("OS-INT-004", chamadoA, unidadeA);

        entityManager.flush();

        Long ordemServicoId = ordemServicoA.getId();
        Long chamadoIdIncorreto = chamadoB.getId();
        Long contratoId = contratoA.getId();

        entityManager.clear();

        Optional<OrdemServico> resultado = ordemServicoRepository
                .findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId,
                        chamadoIdIncorreto,
                        contratoId
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
                LocalDateTime.of(2026, 8, 23, 10, 0)
        );
        entityManager.persist(chamado);
        return chamado;
    }

    private OrdemServico persistirOrdemServico(
            String numeroOrdemServico,
            Chamado chamado,
            Unidade unidadeAtendimento
    ) {
        OrdemServico ordemServico = new OrdemServico();
        ordemServico.setNumeroOrdemServico(numeroOrdemServico);
        ordemServico.setChamado(chamado);
        ordemServico.setUnidadeAtendimento(unidadeAtendimento);
        entityManager.persist(ordemServico);
        return ordemServico;
    }
}
