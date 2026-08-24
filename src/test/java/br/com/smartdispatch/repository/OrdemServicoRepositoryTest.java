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
}
