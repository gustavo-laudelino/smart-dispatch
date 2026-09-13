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
        chamado.setContrato(contrato);
        chamado.setNumeroChamadoInterno(1L);
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
        ordemServico.setNumeroOrdemServico(1L);
        ordemServico.setChamado(chamado);
        ordemServico.setUnidadeAtendimento(unidade);
        ordemServico.setData(java.time.LocalDate.now());
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
                .isEqualTo(1L);
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
                persistirOrdemServico(2, chamadoA, unidadeA);

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
                persistirOrdemServico(3, chamadoA, unidadeA);

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
                persistirOrdemServico(4, chamadoA, unidadeA);

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

    @Test
    void deveConfirmarTecnicoAtribuidoQuandoEscoposCorrespondem() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado("CH-INT-005", unidadeA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.005@teste.local"
        );
        BaseOperacional baseOperacionalA =
                persistirBaseOperacional(contratoA);
        Tecnico tecnicoA =
                persistirTecnico(usuarioA, baseOperacionalA);

        OrdemServico ordemServicoA = new OrdemServico();
        ordemServicoA.setNumeroOrdemServico(5L);
        ordemServicoA.setChamado(chamadoA);
        ordemServicoA.setUnidadeAtendimento(unidadeA);
        ordemServicoA.setTecnico(tecnicoA);
        ordemServicoA.setData(java.time.LocalDate.now());
        entityManager.persist(ordemServicoA);

        entityManager.flush();

        Long ordemServicoId = ordemServicoA.getId();
        Long usuarioId = usuarioA.getId();
        Long chamadoId = chamadoA.getId();
        Long contratoId = contratoA.getId();

        entityManager.clear();

        boolean resultado = ordemServicoRepository
                .existsByIdAndTecnicoUsuarioIdAndChamadoIdAndChamadoUnidadeContratoIdAndChamadoStatusNot(
                        ordemServicoId,
                        usuarioId,
                        chamadoId,
                        contratoId,
                        StatusChamado.FINALIZADO
                );

        assertThat(resultado).isTrue();
    }

    @Test
    void deveNegarTecnicoAtribuidoQuandoUsuarioNaoCorresponde() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado("CH-INT-006", unidadeA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.006@teste.local"
        );
        Usuario usuarioB = persistirUsuario(
                "Usuario Teste B",
                "usuario.b.006@teste.local"
        );
        BaseOperacional baseOperacionalA =
                persistirBaseOperacional(contratoA);
        Tecnico tecnicoA =
                persistirTecnico(usuarioA, baseOperacionalA);

        OrdemServico ordemServicoA = new OrdemServico();
        ordemServicoA.setNumeroOrdemServico(6L);
        ordemServicoA.setChamado(chamadoA);
        ordemServicoA.setUnidadeAtendimento(unidadeA);
        ordemServicoA.setTecnico(tecnicoA);
        ordemServicoA.setData(java.time.LocalDate.now());
        entityManager.persist(ordemServicoA);

        entityManager.flush();

        Long ordemServicoId = ordemServicoA.getId();
        Long usuarioIdIncorreto = usuarioB.getId();
        Long chamadoId = chamadoA.getId();
        Long contratoId = contratoA.getId();

        entityManager.clear();

        boolean resultado = ordemServicoRepository
                .existsByIdAndTecnicoUsuarioIdAndChamadoIdAndChamadoUnidadeContratoIdAndChamadoStatusNot(
                        ordemServicoId,
                        usuarioIdIncorreto,
                        chamadoId,
                        contratoId,
                        StatusChamado.FINALIZADO
                );

        assertThat(resultado).isFalse();
    }

    @Test
    void deveNegarTecnicoAtribuidoQuandoContratoNaoCorresponde() {
        Contrato contratoA = persistirContrato();
        Contrato contratoB = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado("CH-INT-007", unidadeA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.007@teste.local"
        );
        BaseOperacional baseOperacionalA =
                persistirBaseOperacional(contratoA);
        Tecnico tecnicoA =
                persistirTecnico(usuarioA, baseOperacionalA);

        OrdemServico ordemServicoA = new OrdemServico();
        ordemServicoA.setNumeroOrdemServico(7L);
        ordemServicoA.setChamado(chamadoA);
        ordemServicoA.setUnidadeAtendimento(unidadeA);
        ordemServicoA.setTecnico(tecnicoA);
        ordemServicoA.setData(java.time.LocalDate.now());
        entityManager.persist(ordemServicoA);

        entityManager.flush();

        Long ordemServicoId = ordemServicoA.getId();
        Long usuarioId = usuarioA.getId();
        Long chamadoId = chamadoA.getId();
        Long contratoIdIncorreto = contratoB.getId();

        entityManager.clear();

        boolean resultado = ordemServicoRepository
                .existsByIdAndTecnicoUsuarioIdAndChamadoIdAndChamadoUnidadeContratoIdAndChamadoStatusNot(
                        ordemServicoId,
                        usuarioId,
                        chamadoId,
                        contratoIdIncorreto,
                        StatusChamado.FINALIZADO
                );

        assertThat(resultado).isFalse();
    }

    @Test
    void deveNegarTecnicoAtribuidoQuandoChamadoEstaFinalizado() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado("CH-INT-008", unidadeA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.008@teste.local"
        );
        BaseOperacional baseOperacionalA =
                persistirBaseOperacional(contratoA);
        Tecnico tecnicoA =
                persistirTecnico(usuarioA, baseOperacionalA);

        OrdemServico ordemServicoA = new OrdemServico();
        ordemServicoA.setNumeroOrdemServico(8L);
        ordemServicoA.setChamado(chamadoA);
        ordemServicoA.setUnidadeAtendimento(unidadeA);
        ordemServicoA.setTecnico(tecnicoA);
        ordemServicoA.setData(java.time.LocalDate.now());
        entityManager.persist(ordemServicoA);

        chamadoA.setStatus(StatusChamado.FINALIZADO);

        entityManager.flush();

        Long ordemServicoId = ordemServicoA.getId();
        Long usuarioId = usuarioA.getId();
        Long chamadoId = chamadoA.getId();
        Long contratoId = contratoA.getId();

        entityManager.clear();

        boolean resultado = ordemServicoRepository
                .existsByIdAndTecnicoUsuarioIdAndChamadoIdAndChamadoUnidadeContratoIdAndChamadoStatusNot(
                        ordemServicoId,
                        usuarioId,
                        chamadoId,
                        contratoId,
                        StatusChamado.FINALIZADO
                );

        assertThat(resultado).isFalse();
    }

    @Test
    void deveBuscarOrdemComCheckInAtivoDoTecnico() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado("CH-INT-009", unidadeA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.009@teste.local"
        );
        BaseOperacional baseOperacionalA =
                persistirBaseOperacional(contratoA);
        Tecnico tecnicoA =
                persistirTecnico(usuarioA, baseOperacionalA);

        OrdemServico ordemServicoA = persistirOrdemServicoComTecnico(
                9,
                chamadoA,
                unidadeA,
                tecnicoA,
                LocalDateTime.of(2026, 8, 23, 10, 0),
                null
        );

        entityManager.flush();

        Long tecnicoId = tecnicoA.getId();
        Long ordemServicoId = ordemServicoA.getId();

        entityManager.clear();

        Optional<OrdemServico> resultado = ordemServicoRepository
                .findByTecnicoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(
                        tecnicoId
                );

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getId()).isEqualTo(ordemServicoId);
    }

    @Test
    void deveNaoBuscarOrdemDoTecnicoQuandoCheckOutJaExiste() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado("CH-INT-010", unidadeA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.010@teste.local"
        );
        BaseOperacional baseOperacionalA =
                persistirBaseOperacional(contratoA);
        Tecnico tecnicoA =
                persistirTecnico(usuarioA, baseOperacionalA);

        persistirOrdemServicoComTecnico(
                10,
                chamadoA,
                unidadeA,
                tecnicoA,
                LocalDateTime.of(2026, 8, 23, 10, 0),
                LocalDateTime.of(2026, 8, 23, 11, 0)
        );

        entityManager.flush();

        Long tecnicoId = tecnicoA.getId();

        entityManager.clear();

        Optional<OrdemServico> resultado = ordemServicoRepository
                .findByTecnicoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(
                        tecnicoId
                );

        assertThat(resultado).isEmpty();
    }

    @Test
    void deveConfirmarAtendimentoAtivoQuandoExisteCheckInSemCheckOut() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado("CH-INT-011", unidadeA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.011@teste.local"
        );
        BaseOperacional baseOperacionalA =
                persistirBaseOperacional(contratoA);
        Tecnico tecnicoA =
                persistirTecnico(usuarioA, baseOperacionalA);

        persistirOrdemServicoComTecnico(
                11,
                chamadoA,
                unidadeA,
                tecnicoA,
                LocalDateTime.of(2026, 8, 23, 10, 0),
                null
        );

        entityManager.flush();

        Long chamadoId = chamadoA.getId();

        entityManager.clear();

        boolean resultado = ordemServicoRepository
                .existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(
                        chamadoId
                );

        assertThat(resultado).isTrue();
    }

    @Test
    void deveNegarAtendimentoAtivoQuandoCheckOutJaExiste() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado("CH-INT-012", unidadeA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.012@teste.local"
        );
        BaseOperacional baseOperacionalA =
                persistirBaseOperacional(contratoA);
        Tecnico tecnicoA =
                persistirTecnico(usuarioA, baseOperacionalA);

        persistirOrdemServicoComTecnico(
                12,
                chamadoA,
                unidadeA,
                tecnicoA,
                LocalDateTime.of(2026, 8, 23, 10, 0),
                LocalDateTime.of(2026, 8, 23, 11, 0)
        );

        entityManager.flush();

        Long chamadoId = chamadoA.getId();

        entityManager.clear();

        boolean resultado = ordemServicoRepository
                .existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(
                        chamadoId
                );

        assertThat(resultado).isFalse();
    }

    @Test
    void deveConfirmarOrdemAtribuidaAindaNaoIniciada() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado("CH-INT-013", unidadeA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.013@teste.local"
        );
        BaseOperacional baseOperacionalA =
                persistirBaseOperacional(contratoA);
        Tecnico tecnicoA =
                persistirTecnico(usuarioA, baseOperacionalA);

        persistirOrdemServicoComTecnico(
                13,
                chamadoA,
                unidadeA,
                tecnicoA,
                null,
                null
        );

        entityManager.flush();

        Long chamadoId = chamadoA.getId();

        entityManager.clear();

        boolean resultado = ordemServicoRepository
                .existsByChamadoIdAndTecnicoIsNotNullAndDataCheckInIsNullAndDataCheckOutIsNull(
                        chamadoId
                );

        assertThat(resultado).isTrue();
    }

    @Test
    void deveConfirmarOrdemSemTecnicoAindaNaoIniciada() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado("CH-INT-014", unidadeA);

        persistirOrdemServicoComTecnico(
                14,
                chamadoA,
                unidadeA,
                null,
                null,
                null
        );

        entityManager.flush();

        Long chamadoId = chamadoA.getId();

        entityManager.clear();

        boolean resultado = ordemServicoRepository
                .existsByChamadoIdAndTecnicoIsNullAndDataCheckInIsNullAndDataCheckOutIsNull(
                        chamadoId
                );

        assertThat(resultado).isTrue();
    }

    @Test
    void deveNegarOrdemSemTecnicoQuandoTecnicoEstaAtribuido() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Chamado chamadoA = persistirChamado("CH-INT-015", unidadeA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.015@teste.local"
        );
        BaseOperacional baseOperacionalA =
                persistirBaseOperacional(contratoA);
        Tecnico tecnicoA =
                persistirTecnico(usuarioA, baseOperacionalA);

        persistirOrdemServicoComTecnico(
                15,
                chamadoA,
                unidadeA,
                tecnicoA,
                null,
                null
        );

        entityManager.flush();

        Long chamadoId = chamadoA.getId();

        entityManager.clear();

        boolean resultado = ordemServicoRepository
                .existsByChamadoIdAndTecnicoIsNullAndDataCheckInIsNullAndDataCheckOutIsNull(
                        chamadoId
                );

        assertThat(resultado).isFalse();
    }

    @Test
    void deveListarSomenteOrdensAbertasDoTecnico() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.020@teste.local"
        );
        BaseOperacional baseOperacionalA =
                persistirBaseOperacional(contratoA);
        Tecnico tecnicoA =
                persistirTecnico(usuarioA, baseOperacionalA);
        Usuario usuarioB = persistirUsuario(
                "Usuario Teste B",
                "usuario.b.020@teste.local"
        );
        Tecnico tecnicoB =
                persistirTecnico(usuarioB, baseOperacionalA);

        Chamado chamadoA = persistirChamado("CH-INT-020-A", unidadeA);
        Chamado chamadoB = persistirChamado("CH-INT-020-B", unidadeA);
        Chamado chamadoC = persistirChamado("CH-INT-020-C", unidadeA);

        OrdemServico ordemServicoA = persistirOrdemServicoComTecnico(
                20,
                chamadoA,
                unidadeA,
                tecnicoA,
                null,
                null
        );
        persistirOrdemServicoComTecnico(
                21,
                chamadoB,
                unidadeA,
                tecnicoA,
                LocalDateTime.of(2026, 8, 23, 9, 0),
                LocalDateTime.of(2026, 8, 23, 10, 0)
        );
        persistirOrdemServicoComTecnico(
                22,
                chamadoC,
                unidadeA,
                tecnicoB,
                null,
                null
        );

        entityManager.flush();

        Long tecnicoAId = tecnicoA.getId();
        Long contratoAId = contratoA.getId();
        Long ordemServicoIdA = ordemServicoA.getId();

        entityManager.clear();

        List<OrdemServico> resultado = ordemServicoRepository
                .findByTecnicoIdAndUnidadeAtendimentoContratoIdAndDataCheckOutIsNull(
                        tecnicoAId,
                        contratoAId
                );

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo(ordemServicoIdA);
    }

    @Test
    void deveListarMultiplasOrdensAbertasDoMesmoTecnico() {
        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.021@teste.local"
        );
        BaseOperacional baseOperacionalA =
                persistirBaseOperacional(contratoA);
        Tecnico tecnicoA =
                persistirTecnico(usuarioA, baseOperacionalA);

        Chamado chamadoA = persistirChamado("CH-INT-021-A", unidadeA);
        Chamado chamadoB = persistirChamado("CH-INT-021-B", unidadeA);

        OrdemServico ordemServicoA = persistirOrdemServicoComTecnico(
                23,
                chamadoA,
                unidadeA,
                tecnicoA,
                null,
                null
        );
        OrdemServico ordemServicoB = persistirOrdemServicoComTecnico(
                24,
                chamadoB,
                unidadeA,
                tecnicoA,
                null,
                null
        );

        entityManager.flush();

        Long tecnicoAId = tecnicoA.getId();
        Long contratoAId = contratoA.getId();
        Long ordemServicoIdA = ordemServicoA.getId();
        Long ordemServicoIdB = ordemServicoB.getId();

        entityManager.clear();

        List<OrdemServico> resultado = ordemServicoRepository
                .findByTecnicoIdAndUnidadeAtendimentoContratoIdAndDataCheckOutIsNull(
                        tecnicoAId,
                        contratoAId
                );

        assertThat(resultado).hasSize(2);
        assertThat(resultado)
                .extracting(OrdemServico::getId)
                .containsExactlyInAnyOrder(ordemServicoIdA, ordemServicoIdB);
    }

    @Test
    void deveContarCheckoutsDesdeDataInicialIncluindoLimite() {
        LocalDateTime dataInicial = LocalDateTime.of(2026, 8, 23, 10, 0);

        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.022@teste.local"
        );
        BaseOperacional baseOperacionalA =
                persistirBaseOperacional(contratoA);
        Tecnico tecnicoA =
                persistirTecnico(usuarioA, baseOperacionalA);

        Chamado chamadoA = persistirChamado("CH-INT-022-A", unidadeA);
        Chamado chamadoB = persistirChamado("CH-INT-022-B", unidadeA);
        Chamado chamadoC = persistirChamado("CH-INT-022-C", unidadeA);

        persistirOrdemServicoComTecnico(
                25,
                chamadoA,
                unidadeA,
                tecnicoA,
                LocalDateTime.of(2026, 8, 23, 9, 0),
                LocalDateTime.of(2026, 8, 23, 9, 59)
        );
        persistirOrdemServicoComTecnico(
                26,
                chamadoB,
                unidadeA,
                tecnicoA,
                LocalDateTime.of(2026, 8, 23, 9, 30),
                LocalDateTime.of(2026, 8, 23, 10, 0)
        );
        persistirOrdemServicoComTecnico(
                27,
                chamadoC,
                unidadeA,
                tecnicoA,
                LocalDateTime.of(2026, 8, 23, 10, 30),
                LocalDateTime.of(2026, 8, 23, 11, 0)
        );

        entityManager.flush();

        Long tecnicoAId = tecnicoA.getId();
        Long contratoAId = contratoA.getId();

        entityManager.clear();

        long resultado = ordemServicoRepository
                .countByTecnicoIdAndUnidadeAtendimentoContratoIdAndDataCheckOutGreaterThanEqual(
                        tecnicoAId,
                        contratoAId,
                        dataInicial
                );

        assertThat(resultado).isEqualTo(2);
    }

    @Test
    void deveRespeitarTecnicoNaContagemDeCheckouts() {
        LocalDateTime dataInicial = LocalDateTime.of(2026, 8, 23, 10, 0);

        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.023@teste.local"
        );
        BaseOperacional baseOperacionalA =
                persistirBaseOperacional(contratoA);
        Tecnico tecnicoA =
                persistirTecnico(usuarioA, baseOperacionalA);
        Usuario usuarioB = persistirUsuario(
                "Usuario Teste B",
                "usuario.b.023@teste.local"
        );
        Tecnico tecnicoB =
                persistirTecnico(usuarioB, baseOperacionalA);

        Chamado chamadoA = persistirChamado("CH-INT-023-A", unidadeA);
        Chamado chamadoB = persistirChamado("CH-INT-023-B", unidadeA);

        persistirOrdemServicoComTecnico(
                28,
                chamadoA,
                unidadeA,
                tecnicoA,
                LocalDateTime.of(2026, 8, 23, 10, 30),
                LocalDateTime.of(2026, 8, 23, 11, 0)
        );
        persistirOrdemServicoComTecnico(
                29,
                chamadoB,
                unidadeA,
                tecnicoB,
                LocalDateTime.of(2026, 8, 23, 10, 30),
                LocalDateTime.of(2026, 8, 23, 11, 0)
        );

        entityManager.flush();

        Long tecnicoAId = tecnicoA.getId();
        Long contratoAId = contratoA.getId();

        entityManager.clear();

        long resultado = ordemServicoRepository
                .countByTecnicoIdAndUnidadeAtendimentoContratoIdAndDataCheckOutGreaterThanEqual(
                        tecnicoAId,
                        contratoAId,
                        dataInicial
                );

        assertThat(resultado).isEqualTo(1);
    }

    @Test
    void deveContarAtribuicoesComInicioInclusivoEFimExclusivo() {
        LocalDateTime inicio = LocalDateTime.of(2026, 8, 23, 10, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 8, 23, 12, 0);

        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.024@teste.local"
        );
        BaseOperacional baseOperacionalA =
                persistirBaseOperacional(contratoA);
        Tecnico tecnicoA =
                persistirTecnico(usuarioA, baseOperacionalA);

        Chamado chamadoA = persistirChamado("CH-INT-024-A", unidadeA);
        Chamado chamadoB = persistirChamado("CH-INT-024-B", unidadeA);
        Chamado chamadoC = persistirChamado("CH-INT-024-C", unidadeA);
        Chamado chamadoD = persistirChamado("CH-INT-024-D", unidadeA);
        Chamado chamadoIgnorada =
                persistirChamado("CH-INT-024-IGN", unidadeA);

        persistirOrdemServicoComTecnico(
                30,
                chamadoA,
                unidadeA,
                tecnicoA,
                null,
                null,
                LocalDateTime.of(2026, 8, 23, 9, 59)
        );
        persistirOrdemServicoComTecnico(
                31,
                chamadoB,
                unidadeA,
                tecnicoA,
                null,
                null,
                LocalDateTime.of(2026, 8, 23, 10, 0)
        );
        persistirOrdemServicoComTecnico(
                32,
                chamadoC,
                unidadeA,
                tecnicoA,
                null,
                null,
                LocalDateTime.of(2026, 8, 23, 11, 0)
        );
        persistirOrdemServicoComTecnico(
                33,
                chamadoD,
                unidadeA,
                tecnicoA,
                null,
                null,
                LocalDateTime.of(2026, 8, 23, 12, 0)
        );
        OrdemServico ordemServicoIgnorada = persistirOrdemServicoComTecnico(
                34,
                chamadoIgnorada,
                unidadeA,
                tecnicoA,
                null,
                null,
                LocalDateTime.of(2026, 8, 23, 8, 0)
        );

        entityManager.flush();

        Long tecnicoAId = tecnicoA.getId();
        Long contratoAId = contratoA.getId();
        Long ordemServicoIdIgnorada = ordemServicoIgnorada.getId();

        entityManager.clear();

        long resultado = ordemServicoRepository
                .countByTecnicoIdAndUnidadeAtendimentoContratoIdAndIdNotAndDataAtribuicaoTecnicoGreaterThanEqualAndDataAtribuicaoTecnicoLessThan(
                        tecnicoAId,
                        contratoAId,
                        ordemServicoIdIgnorada,
                        inicio,
                        fim
                );

        assertThat(resultado).isEqualTo(2);
    }

    @Test
    void deveIgnorarOrdemInformadaNaContagemDeAtribuicoes() {
        LocalDateTime inicio = LocalDateTime.of(2026, 8, 23, 10, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 8, 23, 12, 0);

        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.025@teste.local"
        );
        BaseOperacional baseOperacionalA =
                persistirBaseOperacional(contratoA);
        Tecnico tecnicoA =
                persistirTecnico(usuarioA, baseOperacionalA);

        Chamado chamadoA = persistirChamado("CH-INT-025-A", unidadeA);
        Chamado chamadoB = persistirChamado("CH-INT-025-B", unidadeA);

        OrdemServico ordemServicoA = persistirOrdemServicoComTecnico(
                35,
                chamadoA,
                unidadeA,
                tecnicoA,
                null,
                null,
                LocalDateTime.of(2026, 8, 23, 10, 30)
        );
        persistirOrdemServicoComTecnico(
                36,
                chamadoB,
                unidadeA,
                tecnicoA,
                null,
                null,
                LocalDateTime.of(2026, 8, 23, 11, 0)
        );

        entityManager.flush();

        Long tecnicoAId = tecnicoA.getId();
        Long contratoAId = contratoA.getId();
        Long ordemServicoIdIgnorada = ordemServicoA.getId();

        entityManager.clear();

        long resultado = ordemServicoRepository
                .countByTecnicoIdAndUnidadeAtendimentoContratoIdAndIdNotAndDataAtribuicaoTecnicoGreaterThanEqualAndDataAtribuicaoTecnicoLessThan(
                        tecnicoAId,
                        contratoAId,
                        ordemServicoIdIgnorada,
                        inicio,
                        fim
                );

        assertThat(resultado).isEqualTo(1);
    }

    @Test
    void deveRespeitarTecnicoNaContagemDeAtribuicoes() {
        LocalDateTime inicio = LocalDateTime.of(2026, 8, 23, 10, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 8, 23, 12, 0);

        Contrato contratoA = persistirContrato();
        Unidade unidadeA = persistirUnidade(contratoA);
        Usuario usuarioA = persistirUsuario(
                "Usuario Teste A",
                "usuario.a.026@teste.local"
        );
        BaseOperacional baseOperacionalA =
                persistirBaseOperacional(contratoA);
        Tecnico tecnicoA =
                persistirTecnico(usuarioA, baseOperacionalA);
        Usuario usuarioB = persistirUsuario(
                "Usuario Teste B",
                "usuario.b.026@teste.local"
        );
        Tecnico tecnicoB =
                persistirTecnico(usuarioB, baseOperacionalA);

        Chamado chamadoA = persistirChamado("CH-INT-026-A", unidadeA);
        Chamado chamadoB = persistirChamado("CH-INT-026-B", unidadeA);
        Chamado chamadoIgnorada =
                persistirChamado("CH-INT-026-IGN", unidadeA);

        persistirOrdemServicoComTecnico(
                37,
                chamadoA,
                unidadeA,
                tecnicoA,
                null,
                null,
                LocalDateTime.of(2026, 8, 23, 10, 30)
        );
        persistirOrdemServicoComTecnico(
                38,
                chamadoB,
                unidadeA,
                tecnicoB,
                null,
                null,
                LocalDateTime.of(2026, 8, 23, 10, 30)
        );
        OrdemServico ordemServicoIgnorada = persistirOrdemServicoComTecnico(
                39,
                chamadoIgnorada,
                unidadeA,
                tecnicoA,
                null,
                null,
                LocalDateTime.of(2026, 8, 23, 8, 0)
        );

        entityManager.flush();

        Long tecnicoAId = tecnicoA.getId();
        Long contratoAId = contratoA.getId();
        Long ordemServicoIdIgnorada = ordemServicoIgnorada.getId();

        entityManager.clear();

        long resultado = ordemServicoRepository
                .countByTecnicoIdAndUnidadeAtendimentoContratoIdAndIdNotAndDataAtribuicaoTecnicoGreaterThanEqualAndDataAtribuicaoTecnicoLessThan(
                        tecnicoAId,
                        contratoAId,
                        ordemServicoIdIgnorada,
                        inicio,
                        fim
                );

        assertThat(resultado).isEqualTo(1);
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

    private long proximoNumeroChamadoInterno = 100_000;

    private Chamado persistirChamado(String numeroChamado, Unidade unidade) {
        Chamado chamado = new Chamado();
        chamado.setNumeroChamado(numeroChamado);
        chamado.setLinkChamadoOsti(
                "https://teste.local/chamado/" + numeroChamado
        );
        chamado.setUnidade(unidade);
        chamado.setContrato(unidade.getContrato());
        chamado.setNumeroChamadoInterno(proximoNumeroChamadoInterno++);
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
            long numeroOrdemServico,
            Chamado chamado,
            Unidade unidadeAtendimento
    ) {
        OrdemServico ordemServico = new OrdemServico();
        ordemServico.setNumeroOrdemServico(numeroOrdemServico);
        ordemServico.setChamado(chamado);
        ordemServico.setUnidadeAtendimento(unidadeAtendimento);
        ordemServico.setData(java.time.LocalDate.now());
        entityManager.persist(ordemServico);
        return ordemServico;
    }

    private OrdemServico persistirOrdemServicoComTecnico(
            long numeroOrdemServico,
            Chamado chamado,
            Unidade unidadeAtendimento,
            Tecnico tecnico,
            LocalDateTime dataCheckIn,
            LocalDateTime dataCheckOut
    ) {
        return persistirOrdemServicoComTecnico(
                numeroOrdemServico,
                chamado,
                unidadeAtendimento,
                tecnico,
                dataCheckIn,
                dataCheckOut,
                null
        );
    }

    private OrdemServico persistirOrdemServicoComTecnico(
            long numeroOrdemServico,
            Chamado chamado,
            Unidade unidadeAtendimento,
            Tecnico tecnico,
            LocalDateTime dataCheckIn,
            LocalDateTime dataCheckOut,
            LocalDateTime dataAtribuicaoTecnico
    ) {
        OrdemServico ordemServico = new OrdemServico();
        ordemServico.setNumeroOrdemServico(numeroOrdemServico);
        ordemServico.setChamado(chamado);
        ordemServico.setUnidadeAtendimento(unidadeAtendimento);
        ordemServico.setTecnico(tecnico);
        ordemServico.setDataCheckIn(dataCheckIn);
        ordemServico.setDataCheckOut(dataCheckOut);
        ordemServico.setDataAtribuicaoTecnico(dataAtribuicaoTecnico);
        ordemServico.setData(java.time.LocalDate.now());
        entityManager.persist(ordemServico);
        return ordemServico;
    }

    private Usuario persistirUsuario(String nome, String email) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setPerfil(PerfilUsuario.TECNICO);
        usuario.setSenha("senha-teste");
        usuario.setAtivo(true);
        entityManager.persist(usuario);
        return usuario;
    }

    private BaseOperacional persistirBaseOperacional(Contrato contrato) {
        BaseOperacional baseOperacional = new BaseOperacional();
        baseOperacional.setContrato(contrato);
        entityManager.persist(baseOperacional);
        return baseOperacional;
    }

    private Tecnico persistirTecnico(
            Usuario usuario,
            BaseOperacional baseOperacional
    ) {
        Tecnico tecnico = new Tecnico();
        tecnico.setUsuario(usuario);
        tecnico.setBaseOperacional(baseOperacional);
        tecnico.setAtivo(true);
        entityManager.persist(tecnico);
        return tecnico;
    }
}
