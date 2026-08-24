package br.com.smartdispatch.repository;

import br.com.smartdispatch.model.BaseOperacional;
import br.com.smartdispatch.model.Contrato;
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
class BaseOperacionalRepositoryTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private BaseOperacionalRepository baseOperacionalRepository;

    @Test
    void deveListarSomenteBasesDoContratoInformado() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();

        BaseOperacional baseA1 =
                persistirBaseOperacional(contratoA, "Base A1");
        BaseOperacional baseA2 =
                persistirBaseOperacional(contratoA, "Base A2");
        persistirBaseOperacional(contratoB, "Base B1");

        entityManager.flush();

        Long contratoAId = contratoA.getId();
        Long baseIdA1 = baseA1.getId();
        Long baseIdA2 = baseA2.getId();

        entityManager.clear();

        List<BaseOperacional> resultado = baseOperacionalRepository
                .findByContratoId(contratoAId);

        assertThat(resultado).hasSize(2);
        assertThat(resultado)
                .extracting(BaseOperacional::getId)
                .containsExactlyInAnyOrder(baseIdA1, baseIdA2);
    }

    @Test
    void deveRetornarListaVaziaQuandoContratoNaoPossuiBases() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();

        persistirBaseOperacional(contratoA, "Base A");

        entityManager.flush();

        Long contratoBId = contratoB.getId();

        entityManager.clear();

        List<BaseOperacional> resultado = baseOperacionalRepository
                .findByContratoId(contratoBId);

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveBuscarBaseQuandoIdEContratoCorrespondem() {
        Contrato contratoA = persistirContrato();
        BaseOperacional baseA =
                persistirBaseOperacional(contratoA, "Base A");

        entityManager.flush();

        Long baseAId = baseA.getId();
        Long contratoAId = contratoA.getId();

        entityManager.clear();

        Optional<BaseOperacional> resultado = baseOperacionalRepository
                .findByIdAndContratoId(baseAId, contratoAId);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(baseAId);
        assertThat(resultado.get().getContrato().getId())
                .isEqualTo(contratoAId);
    }

    @Test
    void deveRetornarVazioQuandoBasePertenceAOutroContrato() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();
        BaseOperacional baseA =
                persistirBaseOperacional(contratoA, "Base A");

        entityManager.flush();

        Long baseAId = baseA.getId();
        Long contratoBId = contratoB.getId();

        entityManager.clear();

        Optional<BaseOperacional> resultado = baseOperacionalRepository
                .findByIdAndContratoId(baseAId, contratoBId);

        assertThat(resultado).isEmpty();
    }

    private Contrato persistirContrato() {
        Contrato contrato = new Contrato();
        entityManager.persist(contrato);
        return contrato;
    }

    private BaseOperacional persistirBaseOperacional(
            Contrato contrato,
            String nome
    ) {
        BaseOperacional baseOperacional = new BaseOperacional();
        baseOperacional.setContrato(contrato);
        baseOperacional.setNome(nome);
        entityManager.persist(baseOperacional);
        return baseOperacional;
    }
}
