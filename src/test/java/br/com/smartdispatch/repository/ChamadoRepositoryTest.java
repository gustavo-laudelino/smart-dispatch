package br.com.smartdispatch.repository;

import br.com.smartdispatch.enums.CategoriaChamado;
import br.com.smartdispatch.enums.PerfilUsuario;
import br.com.smartdispatch.enums.PrioridadeChamado;
import br.com.smartdispatch.enums.StatusChamado;
import br.com.smartdispatch.enums.TipoChamado;
import br.com.smartdispatch.model.BaseOperacional;
import br.com.smartdispatch.model.Chamado;
import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.model.OrdemServico;
import br.com.smartdispatch.model.Tecnico;
import br.com.smartdispatch.model.Unidade;
import br.com.smartdispatch.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
class ChamadoRepositoryTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Test
    void deveListarSomenteChamadosDoContratoInformado() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Unidade unidadeB = persistirUnidade(contratoB);

        Chamado chamadoA1 = persistirChamado("CH-INT-001-A", unidadeA);
        Chamado chamadoA2 = persistirChamado("CH-INT-001-B", unidadeA);
        persistirChamado("CH-INT-001-C", unidadeB);

        entityManager.flush();

        Long contratoAId = contratoA.getId();
        Long chamadoIdA1 = chamadoA1.getId();
        Long chamadoIdA2 = chamadoA2.getId();

        entityManager.clear();

        List<Chamado> resultado =
                chamadoRepository.findByUnidadeContratoId(contratoAId);

        assertThat(resultado).hasSize(2);
        assertThat(resultado)
                .extracting(Chamado::getId)
                .containsExactlyInAnyOrder(chamadoIdA1, chamadoIdA2);
    }

    @Test
    void deveRetornarListaVaziaQuandoContratoNaoPossuiChamados() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        persistirUnidade(contratoB);

        persistirChamado("CH-INT-002", unidadeA);

        entityManager.flush();

        Long contratoBId = contratoB.getId();

        entityManager.clear();

        List<Chamado> resultado =
                chamadoRepository.findByUnidadeContratoId(contratoBId);

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveBuscarChamadoQuandoIdEContratoCorrespondem() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado("CH-INT-003", unidadeA);

        entityManager.flush();

        Long chamadoAId = chamadoA.getId();
        Long contratoAId = contratoA.getId();

        entityManager.clear();

        Optional<Chamado> resultado = chamadoRepository
                .findByIdAndUnidadeContratoId(chamadoAId, contratoAId);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(chamadoAId);
    }

    @Test
    void deveRetornarVazioQuandoChamadoPertenceAOutroContrato() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);

        Chamado chamadoA = persistirChamado("CH-INT-004", unidadeA);

        entityManager.flush();

        Long chamadoAId = chamadoA.getId();
        Long contratoBId = contratoB.getId();

        entityManager.clear();

        Optional<Chamado> resultado = chamadoRepository
                .findByIdAndUnidadeContratoId(chamadoAId, contratoBId);

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveConfirmarNumeroExistenteNoMesmoContrato() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        persistirChamado("CH-INT-027", unidadeA);

        entityManager.flush();

        Long contratoAId = contratoA.getId();

        entityManager.clear();

        boolean resultado = chamadoRepository
                .existsByNumeroChamadoAndUnidadeContratoId(
                        "CH-INT-027",
                        contratoAId
                );

        assertThat(resultado).isTrue();
    }

    @Test
    void deveNegarNumeroQuandoExisteSomenteEmOutroContrato() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        persistirChamado("CH-INT-027", unidadeA);

        entityManager.flush();

        Long contratoBId = contratoB.getId();

        entityManager.clear();

        boolean resultado = chamadoRepository
                .existsByNumeroChamadoAndUnidadeContratoId(
                        "CH-INT-027",
                        contratoBId
                );

        assertThat(resultado).isFalse();
    }

    @Test
    void deveConfirmarNumeroPertencenteAOutroChamado() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        persistirChamado("CH-INT-029-A", unidadeA);
        Chamado chamadoB = persistirChamado("CH-INT-029-B", unidadeA);

        entityManager.flush();

        Long contratoAId = contratoA.getId();
        Long chamadoBId = chamadoB.getId();

        entityManager.clear();

        boolean resultado = chamadoRepository
                .existsByNumeroChamadoAndUnidadeContratoIdAndIdNot(
                        "CH-INT-029-A",
                        contratoAId,
                        chamadoBId
                );

        assertThat(resultado).isTrue();
    }

    @Test
    void deveNegarConflitoQuandoNumeroPertenceAoProprioChamado() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado("CH-INT-030", unidadeA);

        entityManager.flush();

        Long contratoAId = contratoA.getId();
        Long chamadoAId = chamadoA.getId();

        entityManager.clear();

        boolean resultado = chamadoRepository
                .existsByNumeroChamadoAndUnidadeContratoIdAndIdNot(
                        "CH-INT-030",
                        contratoAId,
                        chamadoAId
                );

        assertThat(resultado).isFalse();
    }

    // ---------------------------------------------------------------
    // findByTecnicoAtualEContratoOpcional()
    // ---------------------------------------------------------------

    @Test
    void deveListarSomenteChamadosComOrdemDeServicoDoTecnicoFiltrado() {
        Contrato contrato = persistirContrato();
        Unidade unidade = persistirUnidade(contrato);
        BaseOperacional base = persistirBaseOperacional(contrato);

        Tecnico tecnicoA = persistirTecnico(
                persistirUsuario("tecnico.a.repo@teste.local"), base
        );
        Tecnico tecnicoB = persistirTecnico(
                persistirUsuario("tecnico.b.repo@teste.local"), base
        );

        Chamado chamadoDoTecnicoA = persistirChamado("CH-TEC-001", unidade);
        persistirOrdemServico("OS-TEC-001", chamadoDoTecnicoA, tecnicoA);

        Chamado chamadoDoTecnicoB = persistirChamado("CH-TEC-002", unidade);
        persistirOrdemServico("OS-TEC-002", chamadoDoTecnicoB, tecnicoB);

        Chamado chamadoSemTecnico = persistirChamado("CH-TEC-003", unidade);
        persistirOrdemServico("OS-TEC-003", chamadoSemTecnico, null);

        entityManager.flush();

        Long tecnicoAId = tecnicoA.getId();
        Long chamadoDoTecnicoAId = chamadoDoTecnicoA.getId();

        entityManager.clear();

        Page<Chamado> resultado = chamadoRepository
                .findByTecnicoAtualEContratoOpcional(
                        tecnicoAId, null, PageRequest.of(0, 10)
                );

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getId())
                .isEqualTo(chamadoDoTecnicoAId);
        assertThat(resultado.getTotalElements()).isEqualTo(1);
    }

    @Test
    void deveListarChamadoUmaUnicaVezMesmoComMultiplasOrdensDoMesmoTecnico() {
        Contrato contrato = persistirContrato();
        Unidade unidade = persistirUnidade(contrato);
        BaseOperacional base = persistirBaseOperacional(contrato);

        Tecnico tecnico = persistirTecnico(
                persistirUsuario("tecnico.multiplas@teste.local"), base
        );

        Chamado chamado = persistirChamado("CH-TEC-004", unidade);
        persistirOrdemServico("OS-TEC-004-A", chamado, tecnico);
        persistirOrdemServico("OS-TEC-004-B", chamado, tecnico);
        persistirOrdemServico("OS-TEC-004-C", chamado, tecnico);

        entityManager.flush();

        Long tecnicoId = tecnico.getId();
        Long chamadoId = chamado.getId();

        entityManager.clear();

        Page<Chamado> resultado = chamadoRepository
                .findByTecnicoAtualEContratoOpcional(
                        tecnicoId, null, PageRequest.of(0, 10)
                );

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getId()).isEqualTo(chamadoId);
        assertThat(resultado.getTotalElements()).isEqualTo(1);
    }

    @Test
    void deveRespeitarTecnicoIdEContratoIdCombinados() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Unidade unidadeB = persistirUnidade(contratoB);
        BaseOperacional baseA = persistirBaseOperacional(contratoA);

        Tecnico tecnico = persistirTecnico(
                persistirUsuario("tecnico.doiscontratos@teste.local"), baseA
        );

        Chamado chamadoContratoA = persistirChamado("CH-TEC-005-A", unidadeA);
        persistirOrdemServico("OS-TEC-005-A", chamadoContratoA, tecnico);

        Chamado chamadoContratoB = persistirChamado("CH-TEC-005-B", unidadeB);
        persistirOrdemServico("OS-TEC-005-B", chamadoContratoB, tecnico);

        entityManager.flush();

        Long tecnicoId = tecnico.getId();
        Long contratoAId = contratoA.getId();
        Long chamadoContratoAId = chamadoContratoA.getId();

        entityManager.clear();

        Page<Chamado> resultado = chamadoRepository
                .findByTecnicoAtualEContratoOpcional(
                        tecnicoId, contratoAId, PageRequest.of(0, 10)
                );

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getId())
                .isEqualTo(chamadoContratoAId);
    }

    @Test
    void devePaginarChamadosFiltradosPorTecnicoComTotalCorreto() {
        Contrato contrato = persistirContrato();
        Unidade unidade = persistirUnidade(contrato);
        BaseOperacional base = persistirBaseOperacional(contrato);

        Tecnico tecnico = persistirTecnico(
                persistirUsuario("tecnico.paginacao@teste.local"), base
        );

        persistirOrdemServico(
                "OS-TEC-006-1", persistirChamado("CH-TEC-006-1", unidade), tecnico
        );
        persistirOrdemServico(
                "OS-TEC-006-2", persistirChamado("CH-TEC-006-2", unidade), tecnico
        );
        persistirOrdemServico(
                "OS-TEC-006-3", persistirChamado("CH-TEC-006-3", unidade), tecnico
        );
        persistirOrdemServico(
                "OS-TEC-006-X", persistirChamado("CH-TEC-006-X", unidade), null
        );

        entityManager.flush();

        Long tecnicoId = tecnico.getId();

        entityManager.clear();

        Page<Chamado> resultado = chamadoRepository
                .findByTecnicoAtualEContratoOpcional(
                        tecnicoId, null, PageRequest.of(0, 2)
                );

        assertThat(resultado.getContent()).hasSize(2);
        assertThat(resultado.getTotalElements()).isEqualTo(3);
        assertThat(resultado.getTotalPages()).isEqualTo(2);
    }

    @Test
    void deveListarTodosOsChamados() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Unidade unidadeB = persistirUnidade(contratoB);

        Chamado chamadoA1 = persistirChamado("CH-INT-005-A", unidadeA);
        Chamado chamadoA2 = persistirChamado("CH-INT-005-B", unidadeA);
        Chamado chamadoB1 = persistirChamado("CH-INT-005-C", unidadeB);

        entityManager.flush();

        Long chamadoIdA1 = chamadoA1.getId();
        Long chamadoIdA2 = chamadoA2.getId();
        Long chamadoIdB1 = chamadoB1.getId();

        entityManager.clear();

        List<Chamado> resultado = chamadoRepository.findAll();

        assertThat(resultado).hasSize(3);
        assertThat(resultado)
                .extracting(Chamado::getId)
                .containsExactlyInAnyOrder(
                        chamadoIdA1,
                        chamadoIdA2,
                        chamadoIdB1
                );
    }

    @Test
    void devePaginarTodosOsChamados() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);

        persistirChamado("CH-INT-006-A", unidadeA);
        persistirChamado("CH-INT-006-B", unidadeA);
        persistirChamado("CH-INT-006-C", unidadeA);
        persistirChamado("CH-INT-006-D", unidadeA);
        persistirChamado("CH-INT-006-E", unidadeA);

        entityManager.flush();
        entityManager.clear();

        Page<Chamado> resultado =
                chamadoRepository.findAll(PageRequest.of(0, 2));

        assertThat(resultado.getContent()).hasSize(2);
        assertThat(resultado.getTotalElements()).isEqualTo(5);
        assertThat(resultado.getTotalPages()).isEqualTo(3);
        assertThat(resultado.getNumber()).isEqualTo(0);
        assertThat(resultado.getSize()).isEqualTo(2);
    }

    @Test
    void devePaginarSomenteChamadosDoContratoInformado() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Unidade unidadeB = persistirUnidade(contratoB);

        persistirChamado("CH-INT-007-A", unidadeA);
        persistirChamado("CH-INT-007-B", unidadeA);
        persistirChamado("CH-INT-007-C", unidadeA);
        persistirChamado("CH-INT-007-D", unidadeB);
        persistirChamado("CH-INT-007-E", unidadeB);

        entityManager.flush();

        Long contratoAId = contratoA.getId();

        entityManager.clear();

        Page<Chamado> resultado = chamadoRepository
                .findByUnidadeContratoId(
                        contratoAId,
                        PageRequest.of(0, 2)
                );

        assertThat(resultado.getContent()).hasSize(2);
        assertThat(resultado.getTotalElements()).isEqualTo(3);
        assertThat(resultado.getTotalPages()).isEqualTo(2);
        assertThat(resultado.getContent())
                .extracting(chamado ->
                        chamado.getUnidade().getContrato().getId()
                )
                .containsOnly(contratoAId);
    }

    @Test
    void deveRetornarSegundaPaginaDoContrato() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Unidade unidadeB = persistirUnidade(contratoB);

        persistirChamado("CH-INT-008-A", unidadeA);
        persistirChamado("CH-INT-008-B", unidadeA);
        persistirChamado("CH-INT-008-C", unidadeA);
        persistirChamado("CH-INT-008-D", unidadeB);

        entityManager.flush();

        Long contratoAId = contratoA.getId();

        entityManager.clear();

        Page<Chamado> resultado = chamadoRepository
                .findByUnidadeContratoId(
                        contratoAId,
                        PageRequest.of(1, 2)
                );

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getTotalElements()).isEqualTo(3);
        assertThat(resultado.getTotalPages()).isEqualTo(2);
        assertThat(resultado.getNumber()).isEqualTo(1);
        assertThat(
                resultado.getContent().get(0)
                        .getUnidade()
                        .getContrato()
                        .getId()
        ).isEqualTo(contratoAId);
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

    private BaseOperacional persistirBaseOperacional(Contrato contrato) {
        BaseOperacional base = new BaseOperacional();
        base.setContrato(contrato);
        entityManager.persist(base);
        return base;
    }

    private Usuario persistirUsuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setNome("Técnico Teste");
        usuario.setEmail(email);
        usuario.setPerfil(PerfilUsuario.TECNICO);
        usuario.setSenha("senha-teste");
        usuario.setAtivo(true);
        entityManager.persist(usuario);
        return usuario;
    }

    private Tecnico persistirTecnico(Usuario usuario, BaseOperacional base) {
        Tecnico tecnico = new Tecnico();
        tecnico.setUsuario(usuario);
        tecnico.setBaseOperacional(base);
        tecnico.setAtivo(true);
        entityManager.persist(tecnico);
        return tecnico;
    }

    private OrdemServico persistirOrdemServico(
            String numeroOrdemServico,
            Chamado chamado,
            Tecnico tecnico
    ) {
        OrdemServico ordemServico = new OrdemServico();
        ordemServico.setNumeroOrdemServico(numeroOrdemServico);
        ordemServico.setChamado(chamado);
        ordemServico.setTecnico(tecnico);
        ordemServico.setUnidadeAtendimento(chamado.getUnidade());
        entityManager.persist(ordemServico);
        return ordemServico;
    }
}
