package br.com.smartdispatch.repository;

import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.model.Unidade;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class UnidadeRepositoryTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private UnidadeRepository unidadeRepository;

    @Test
    void deveListarSomenteUnidadesDoContratoInformado() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();

        Unidade unidadeA1 = persistirUnidade(contratoA, "Unidade A1");
        Unidade unidadeA2 = persistirUnidade(contratoA, "Unidade A2");
        persistirUnidade(contratoB, "Unidade B1");

        entityManager.flush();

        Long contratoAId = contratoA.getId();
        Long unidadeIdA1 = unidadeA1.getId();
        Long unidadeIdA2 = unidadeA2.getId();

        entityManager.clear();

        List<Unidade> resultado =
                unidadeRepository.findByContratoId(contratoAId);

        assertThat(resultado).hasSize(2);
        assertThat(resultado)
                .extracting(Unidade::getId)
                .containsExactlyInAnyOrder(unidadeIdA1, unidadeIdA2);
    }

    @Test
    void deveRetornarListaVaziaQuandoContratoNaoPossuiUnidades() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();

        persistirUnidade(contratoA, "Unidade A");

        entityManager.flush();

        Long contratoBId = contratoB.getId();

        entityManager.clear();

        List<Unidade> resultado =
                unidadeRepository.findByContratoId(contratoBId);

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveBuscarUnidadeQuandoIdEContratoCorrespondem() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA, "Unidade A");

        entityManager.flush();

        Long unidadeAId = unidadeA.getId();
        Long contratoAId = contratoA.getId();

        entityManager.clear();

        Optional<Unidade> resultado = unidadeRepository
                .findByIdAndContratoId(unidadeAId, contratoAId);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(unidadeAId);
        assertThat(resultado.get().getContrato().getId())
                .isEqualTo(contratoAId);
    }

    @Test
    void deveRetornarVazioQuandoUnidadePertenceAOutroContrato() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA, "Unidade A");

        entityManager.flush();

        Long unidadeAId = unidadeA.getId();
        Long contratoBId = contratoB.getId();

        entityManager.clear();

        Optional<Unidade> resultado = unidadeRepository
                .findByIdAndContratoId(unidadeAId, contratoBId);

        assertThat(resultado).isEmpty();
    }

    private Contrato persistirContrato() {
        Contrato contrato = new Contrato();
        entityManager.persist(contrato);
        return contrato;
    }

    private Unidade persistirUnidade(Contrato contrato, String nome) {
        Unidade unidade = new Unidade();
        unidade.setContrato(contrato);
        unidade.setNome(nome);
        entityManager.persist(unidade);
        return unidade;
    }
}
