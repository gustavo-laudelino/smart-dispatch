package br.com.smartdispatch.service;

import br.com.smartdispatch.dto.CheckInRequest;
import br.com.smartdispatch.dto.OrdemServicoRequest;
import br.com.smartdispatch.dto.OrdemServicoResponse;
import br.com.smartdispatch.enums.StatusChamado;
import br.com.smartdispatch.enums.TipoEventoChamado;
import br.com.smartdispatch.model.Chamado;
import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.model.OrdemServico;
import br.com.smartdispatch.model.Tecnico;
import br.com.smartdispatch.model.Unidade;
import br.com.smartdispatch.model.Usuario;
import br.com.smartdispatch.repository.OrdemServicoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdemServicoServiceTest {

    @Mock
    private OrdemServicoRepository ordemServicoRepository;

    @Mock
    private ChamadoService chamadoService;

    @Mock
    private TecnicoService tecnicoService;

    @Mock
    private UnidadeService unidadeService;

    @Mock
    private HistoricoChamadoService historicoChamadoService;

    @Mock
    private NumeracaoService numeracaoService;

    @InjectMocks
    private OrdemServicoService ordemServicoService;

    // ---------------------------------------------------------------
    // criar() — vinculada a Chamado (rota antiga aninhada)
    // ---------------------------------------------------------------

    @Test
    void deveCriarOrdemComTecnicoEUnidadeInformados() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;

        Unidade unidadeChamado = criarUnidade(5L, contratoId, "Unidade do Chamado");
        Chamado chamado = criarChamado(chamadoId, unidadeChamado, StatusChamado.ABERTO);

        Unidade unidadeAtendimento = criarUnidade(7L, contratoId, "Unidade de Atendimento");
        Usuario usuario = criarUsuario(20L, "Técnico Teste");
        Tecnico tecnico = criarTecnico(30L, usuario, true);

        OrdemServicoRequest request = new OrdemServicoRequest();
        request.setTecnicoId(tecnico.getId());
        request.setUnidadeAtendimentoId(unidadeAtendimento.getId());

        when(chamadoService.buscarEntidadePorId(contratoId, chamadoId))
                .thenReturn(chamado);

        when(numeracaoService.proximoNumeroOrdemServico())
                .thenReturn(100L);

        when(tecnicoService.buscarEntidadePorId(contratoId, tecnico.getId()))
                .thenReturn(tecnico);

        when(unidadeService.buscarPorId(contratoId, unidadeAtendimento.getId()))
                .thenReturn(unidadeAtendimento);

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> {
                    OrdemServico salva = invocation.getArgument(0);
                    salva.setId(50L);
                    return salva;
                });

        // Act
        OrdemServicoResponse response =
                ordemServicoService.criar(contratoId, chamadoId, request);

        // Assert
        assertEquals(100L, response.getNumeroOrdemServico());
        assertEquals(tecnico.getId(), response.getTecnicoId());
        assertEquals(unidadeAtendimento.getId(), response.getUnidadeAtendimentoId());
        assertEquals(chamadoId, response.getChamadoId());
        assertEquals(contratoId, response.getContratoId());

        verify(historicoChamadoService).registrar(
                eq(chamado), any(OrdemServico.class),
                eq(TipoEventoChamado.ORDEM_SERVICO_CRIADA), anyString()
        );

        verify(historicoChamadoService).registrar(
                eq(chamado), any(OrdemServico.class),
                eq(TipoEventoChamado.TECNICO_ATRIBUIDO), anyString()
        );
    }

    @Test
    void deveCriarOrdemSemTecnicoUsandoUnidadeDoChamado() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;

        Unidade unidadeChamado = criarUnidade(5L, contratoId, "Unidade do Chamado");
        Chamado chamado = criarChamado(chamadoId, unidadeChamado, StatusChamado.ABERTO);

        OrdemServicoRequest request = new OrdemServicoRequest();

        when(chamadoService.buscarEntidadePorId(contratoId, chamadoId))
                .thenReturn(chamado);

        when(numeracaoService.proximoNumeroOrdemServico())
                .thenReturn(101L);

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> {
                    OrdemServico salva = invocation.getArgument(0);
                    salva.setId(51L);
                    return salva;
                });

        // Act
        OrdemServicoResponse response =
                ordemServicoService.criar(contratoId, chamadoId, request);

        // Assert
        assertEquals(unidadeChamado.getId(), response.getUnidadeAtendimentoId());
        assertNull(response.getTecnicoId());

        verifyNoInteractions(tecnicoService);

        verify(historicoChamadoService, never()).registrar(
                any(), any(), eq(TipoEventoChamado.TECNICO_ATRIBUIDO), anyString()
        );
    }

    @ParameterizedTest
    @EnumSource(value = StatusChamado.class, names = {"FINALIZADO", "CANCELADO"})
    void deveBloquearCriacaoQuandoChamadoFinalizadoOuCancelado(StatusChamado status) {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");
        Chamado chamado = criarChamado(chamadoId, unidade, status);

        OrdemServicoRequest request = new OrdemServicoRequest();

        when(chamadoService.buscarEntidadePorId(contratoId, chamadoId))
                .thenReturn(chamado);

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.criar(contratoId, chamadoId, request)
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verifyNoInteractions(numeracaoService);
        verify(ordemServicoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveLancarBadRequestQuandoRequestForNulo() {

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.criar(1L, 10L, null)
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(chamadoService, numeracaoService, ordemServicoRepository);
    }

    @Test
    void devePreencherDescricaoEPatrimonioDoChamadoQuandoNaoInformados() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.ABERTO);
        chamado.setDescricao("Descrição original do chamado");
        chamado.setNumeroPatrimonio("PAT-999");

        OrdemServicoRequest request = new OrdemServicoRequest();

        when(chamadoService.buscarEntidadePorId(contratoId, chamadoId))
                .thenReturn(chamado);

        when(numeracaoService.proximoNumeroOrdemServico()).thenReturn(102L);

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrdemServicoResponse response =
                ordemServicoService.criar(contratoId, chamadoId, request);

        // Assert
        assertEquals("Descrição original do chamado", response.getDescricao());
        assertEquals("PAT-999", response.getNumeroPatrimonio());
    }

    @Test
    void deveManterDescricaoEPatrimonioInformadosMesmoComChamado() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.ABERTO);
        chamado.setDescricao("Descrição do chamado");
        chamado.setNumeroPatrimonio("PAT-DO-CHAMADO");

        OrdemServicoRequest request = new OrdemServicoRequest();
        request.setDescricao("Descrição específica da OS");
        request.setNumeroPatrimonio("PAT-DA-OS");

        when(chamadoService.buscarEntidadePorId(contratoId, chamadoId))
                .thenReturn(chamado);

        when(numeracaoService.proximoNumeroOrdemServico()).thenReturn(103L);

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrdemServicoResponse response =
                ordemServicoService.criar(contratoId, chamadoId, request);

        // Assert
        assertEquals("Descrição específica da OS", response.getDescricao());
        assertEquals("PAT-DA-OS", response.getNumeroPatrimonio());
    }

    @Test
    void deveUsarDataAtualComoDefaultQuandoNaoInformada() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.ABERTO);

        OrdemServicoRequest request = new OrdemServicoRequest();

        when(chamadoService.buscarEntidadePorId(contratoId, chamadoId))
                .thenReturn(chamado);

        when(numeracaoService.proximoNumeroOrdemServico()).thenReturn(104L);

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrdemServicoResponse response =
                ordemServicoService.criar(contratoId, chamadoId, request);

        // Assert
        assertEquals(LocalDate.now(), response.getData());
        assertNull(response.getHora());
    }

    // ---------------------------------------------------------------
    // criar() — avulsa, sem Chamado (rota canônica)
    // ---------------------------------------------------------------

    @Test
    void deveCriarOrdemAvulsaSemChamado() {

        // Arrange
        Long contratoId = 1L;

        Unidade unidadeAtendimento = criarUnidade(7L, contratoId, "Unidade de Atendimento");

        OrdemServicoRequest request = new OrdemServicoRequest();
        request.setUnidadeAtendimentoId(unidadeAtendimento.getId());
        request.setDescricao("Manutenção preventiva avulsa");
        request.setData(LocalDate.of(2026, 9, 20));

        when(numeracaoService.proximoNumeroOrdemServico()).thenReturn(200L);

        when(unidadeService.buscarPorId(contratoId, unidadeAtendimento.getId()))
                .thenReturn(unidadeAtendimento);

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> {
                    OrdemServico salva = invocation.getArgument(0);
                    salva.setId(60L);
                    return salva;
                });

        // Act
        OrdemServicoResponse response = ordemServicoService.criar(contratoId, request);

        // Assert
        assertNull(response.getChamadoId());
        assertNull(response.getNumeroChamado());
        assertEquals(200L, response.getNumeroOrdemServico());
        assertEquals("Manutenção preventiva avulsa", response.getDescricao());
        assertEquals(LocalDate.of(2026, 9, 20), response.getData());
        assertEquals(contratoId, response.getContratoId());

        verifyNoInteractions(chamadoService);
    }

    @Test
    void deveExigirUnidadeAtendimentoQuandoNaoHaChamado() {

        // Arrange
        Long contratoId = 1L;
        OrdemServicoRequest request = new OrdemServicoRequest();

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.criar(contratoId, request)
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(numeracaoService, ordemServicoRepository);
    }

    @Test
    void deveNaoRegistrarHistoricoNemRecalcularStatusParaOrdemAvulsa() {

        // Arrange
        Long contratoId = 1L;

        Unidade unidadeAtendimento = criarUnidade(7L, contratoId, "Unidade de Atendimento");

        OrdemServicoRequest request = new OrdemServicoRequest();
        request.setUnidadeAtendimentoId(unidadeAtendimento.getId());

        when(numeracaoService.proximoNumeroOrdemServico()).thenReturn(201L);

        when(unidadeService.buscarPorId(contratoId, unidadeAtendimento.getId()))
                .thenReturn(unidadeAtendimento);

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ordemServicoService.criar(contratoId, request);

        // Assert
        verifyNoInteractions(historicoChamadoService);
        verify(ordemServicoRepository, never())
                .existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(any());
    }

    // ---------------------------------------------------------------
    // atualizar() — rota canônica
    // ---------------------------------------------------------------

    @Test
    void deveAtualizarTecnicoEUnidadeAntesDoCheckIn() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;

        Unidade unidadeAntiga = criarUnidade(5L, contratoId, "Unidade Antiga");
        Chamado chamado = criarChamado(chamadoId, unidadeAntiga, StatusChamado.ATRIBUIDO);

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, chamado, unidadeAntiga, null);

        Unidade unidadeNova = criarUnidade(8L, contratoId, "Unidade Nova");
        Usuario usuario = criarUsuario(20L, "Técnico Novo");
        Tecnico tecnicoNovo = criarTecnico(30L, usuario, true);

        OrdemServicoRequest request = new OrdemServicoRequest();
        request.setChamadoId(chamadoId);
        request.setTecnicoId(tecnicoNovo.getId());
        request.setUnidadeAtendimentoId(unidadeNova.getId());

        when(ordemServicoRepository.findByIdAndUnidadeAtendimentoContratoId(
                ordemServicoId, contratoId
        )).thenReturn(Optional.of(ordemServico));

        when(chamadoService.buscarEntidadePorId(contratoId, chamadoId))
                .thenReturn(chamado);

        when(tecnicoService.buscarEntidadePorId(contratoId, tecnicoNovo.getId()))
                .thenReturn(tecnicoNovo);

        when(unidadeService.buscarPorId(contratoId, unidadeNova.getId()))
                .thenReturn(unidadeNova);

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(ordemServicoRepository.existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(chamadoId))
                .thenReturn(false);
        when(ordemServicoRepository.existsByChamadoIdAndTecnicoIsNotNullAndDataCheckInIsNullAndDataCheckOutIsNull(chamadoId))
                .thenReturn(true);

        // Act
        OrdemServicoResponse response =
                ordemServicoService.atualizar(contratoId, ordemServicoId, request);

        // Assert
        assertEquals(tecnicoNovo.getId(), response.getTecnicoId());
        assertEquals(unidadeNova.getId(), response.getUnidadeAtendimentoId());
        assertEquals(StatusChamado.ATRIBUIDO, chamado.getStatus());

        verify(historicoChamadoService).registrar(
                eq(chamado), eq(ordemServico),
                eq(TipoEventoChamado.TECNICO_ATRIBUIDO), anyString()
        );
    }

    @Test
    void deveRemoverTecnicoAntesDoCheckIn() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.ATRIBUIDO);

        Usuario usuario = criarUsuario(20L, "Técnico Atual");
        Tecnico tecnicoAtual = criarTecnico(30L, usuario, true);

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, chamado, unidade, tecnicoAtual);

        OrdemServicoRequest request = new OrdemServicoRequest();
        request.setChamadoId(chamadoId);
        request.setUnidadeAtendimentoId(unidade.getId());

        when(ordemServicoRepository.findByIdAndUnidadeAtendimentoContratoId(
                ordemServicoId, contratoId
        )).thenReturn(Optional.of(ordemServico));

        when(chamadoService.buscarEntidadePorId(contratoId, chamadoId))
                .thenReturn(chamado);

        when(unidadeService.buscarPorId(contratoId, unidade.getId()))
                .thenReturn(unidade);

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(ordemServicoRepository.existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(chamadoId))
                .thenReturn(false);
        when(ordemServicoRepository.existsByChamadoIdAndTecnicoIsNotNullAndDataCheckInIsNullAndDataCheckOutIsNull(chamadoId))
                .thenReturn(false);
        when(ordemServicoRepository.existsByChamadoIdAndTecnicoIsNullAndDataCheckInIsNullAndDataCheckOutIsNull(chamadoId))
                .thenReturn(true);

        // Act
        OrdemServicoResponse response =
                ordemServicoService.atualizar(contratoId, ordemServicoId, request);

        // Assert
        assertNull(response.getTecnicoId());
        assertNull(ordemServico.getDataAtribuicaoTecnico());

        verify(historicoChamadoService).registrar(
                eq(chamado), eq(ordemServico),
                eq(TipoEventoChamado.TECNICO_REMOVIDO), anyString()
        );
    }

    @Test
    void deveBloquearAlteracaoDeTecnicoAposCheckIn() {

        // Arrange
        Long contratoId = 1L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");
        Chamado chamado = criarChamado(10L, unidade, StatusChamado.EM_ATENDIMENTO);

        Usuario usuario = criarUsuario(20L, "Técnico Atual");
        Tecnico tecnicoAtual = criarTecnico(30L, usuario, true);

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, chamado, unidade, tecnicoAtual);
        ordemServico.setDataCheckIn(LocalDateTime.now());

        Usuario outroUsuario = criarUsuario(21L, "Outro Técnico");
        Tecnico outroTecnico = criarTecnico(31L, outroUsuario, true);

        OrdemServicoRequest request = new OrdemServicoRequest();
        request.setTecnicoId(outroTecnico.getId());

        when(ordemServicoRepository.findByIdAndUnidadeAtendimentoContratoId(
                ordemServicoId, contratoId
        )).thenReturn(Optional.of(ordemServico));

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.atualizar(contratoId, ordemServicoId, request)
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(ordemServicoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveBloquearAlteracaoDeUnidadeAposCheckIn() {

        // Arrange
        Long contratoId = 1L;
        Long ordemServicoId = 40L;

        Unidade unidadeAtual = criarUnidade(5L, contratoId, "Unidade Atual");
        Chamado chamado = criarChamado(10L, unidadeAtual, StatusChamado.EM_ATENDIMENTO);

        Usuario usuario = criarUsuario(20L, "Técnico");
        Tecnico tecnico = criarTecnico(30L, usuario, true);

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, chamado, unidadeAtual, tecnico);
        ordemServico.setDataCheckIn(LocalDateTime.now());

        Unidade unidadeOutra = criarUnidade(9L, contratoId, "Outra Unidade");

        OrdemServicoRequest request = new OrdemServicoRequest();
        request.setTecnicoId(tecnico.getId());
        request.setUnidadeAtendimentoId(unidadeOutra.getId());

        when(ordemServicoRepository.findByIdAndUnidadeAtendimentoContratoId(
                ordemServicoId, contratoId
        )).thenReturn(Optional.of(ordemServico));

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.atualizar(contratoId, ordemServicoId, request)
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(ordemServicoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveBloquearAlteracaoDeChamadoAposCheckIn() {

        // Arrange
        Long contratoId = 1L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");
        Chamado chamado = criarChamado(10L, unidade, StatusChamado.EM_ATENDIMENTO);

        Usuario usuario = criarUsuario(20L, "Técnico");
        Tecnico tecnico = criarTecnico(30L, usuario, true);

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, chamado, unidade, tecnico);
        ordemServico.setDataCheckIn(LocalDateTime.now());

        OrdemServicoRequest request = new OrdemServicoRequest();
        request.setTecnicoId(tecnico.getId());
        request.setChamadoId(999L);

        when(ordemServicoRepository.findByIdAndUnidadeAtendimentoContratoId(
                ordemServicoId, contratoId
        )).thenReturn(Optional.of(ordemServico));

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.atualizar(contratoId, ordemServicoId, request)
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verifyNoInteractions(chamadoService);
    }

    @Test
    void deveManterChamadoEmAtendimentoQuandoExisteAtendimentoAtivoAoAtualizarOrdem() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.EM_ATENDIMENTO);

        Usuario usuario = criarUsuario(20L, "Técnico");
        Tecnico tecnico = criarTecnico(30L, usuario, true);

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, chamado, unidade, tecnico);

        OrdemServicoRequest request = new OrdemServicoRequest();
        request.setTecnicoId(tecnico.getId());
        request.setUnidadeAtendimentoId(unidade.getId());

        when(ordemServicoRepository.findByIdAndUnidadeAtendimentoContratoId(
                ordemServicoId, contratoId
        )).thenReturn(Optional.of(ordemServico));

        when(tecnicoService.buscarEntidadePorId(contratoId, tecnico.getId()))
                .thenReturn(tecnico);
        when(unidadeService.buscarPorId(contratoId, unidade.getId()))
                .thenReturn(unidade);

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(ordemServicoRepository.existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(chamadoId))
                .thenReturn(true);

        // Act
        ordemServicoService.atualizar(contratoId, ordemServicoId, request);

        // Assert
        assertEquals(StatusChamado.EM_ATENDIMENTO, chamado.getStatus());
    }

    @Test
    void deveBloquearRemocaoDeTecnicoAposCheckIn() {

        // Arrange
        Long contratoId = 1L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");
        Chamado chamado = criarChamado(10L, unidade, StatusChamado.EM_ATENDIMENTO);

        Usuario usuario = criarUsuario(20L, "Técnico");
        Tecnico tecnico = criarTecnico(30L, usuario, true);

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, chamado, unidade, tecnico);
        ordemServico.setDataCheckIn(LocalDateTime.now());

        OrdemServicoRequest request = new OrdemServicoRequest();

        when(ordemServicoRepository.findByIdAndUnidadeAtendimentoContratoId(
                ordemServicoId, contratoId
        )).thenReturn(Optional.of(ordemServico));

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.atualizar(contratoId, ordemServicoId, request)
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void deveAtribuirTecnicoPelaPrimeiraVezViaAtualizar() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.ABERTO);

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, chamado, unidade, null);

        Usuario usuario = criarUsuario(20L, "Técnico Novo");
        Tecnico tecnico = criarTecnico(30L, usuario, true);

        OrdemServicoRequest request = new OrdemServicoRequest();
        request.setTecnicoId(tecnico.getId());
        request.setUnidadeAtendimentoId(unidade.getId());

        when(ordemServicoRepository.findByIdAndUnidadeAtendimentoContratoId(
                ordemServicoId, contratoId
        )).thenReturn(Optional.of(ordemServico));

        when(tecnicoService.buscarEntidadePorId(contratoId, tecnico.getId()))
                .thenReturn(tecnico);
        when(unidadeService.buscarPorId(contratoId, unidade.getId()))
                .thenReturn(unidade);

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(ordemServicoRepository.existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(chamadoId))
                .thenReturn(false);
        when(ordemServicoRepository.existsByChamadoIdAndTecnicoIsNotNullAndDataCheckInIsNullAndDataCheckOutIsNull(chamadoId))
                .thenReturn(true);

        // Act
        OrdemServicoResponse response =
                ordemServicoService.atualizar(contratoId, ordemServicoId, request);

        // Assert
        assertEquals(tecnico.getId(), response.getTecnicoId());
        assertNotNull(ordemServico.getDataAtribuicaoTecnico());
    }

    // ---------------------------------------------------------------
    // rota antiga (aninhada em Chamado) — convergência
    // ---------------------------------------------------------------

    @Test
    void atualizarComRotaAntigaDeveConvergirParaMesmaRegra() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.ABERTO);

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, chamado, unidade, null);

        OrdemServicoRequest request = new OrdemServicoRequest();
        request.setUnidadeAtendimentoId(unidade.getId());

        when(ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                ordemServicoId, chamadoId, contratoId
        )).thenReturn(Optional.of(ordemServico));

        when(ordemServicoRepository.findByIdAndUnidadeAtendimentoContratoId(
                ordemServicoId, contratoId
        )).thenReturn(Optional.of(ordemServico));

        when(unidadeService.buscarPorId(contratoId, unidade.getId()))
                .thenReturn(unidade);

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(ordemServicoRepository.existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(chamadoId))
                .thenReturn(false);
        when(ordemServicoRepository.existsByChamadoIdAndTecnicoIsNotNullAndDataCheckInIsNullAndDataCheckOutIsNull(chamadoId))
                .thenReturn(false);
        when(ordemServicoRepository.existsByChamadoIdAndTecnicoIsNullAndDataCheckInIsNullAndDataCheckOutIsNull(chamadoId))
                .thenReturn(true);

        // Act
        OrdemServicoResponse response = ordemServicoService.atualizar(
                contratoId, chamadoId, ordemServicoId, request
        );

        // Assert
        assertEquals(unidade.getId(), response.getUnidadeAtendimentoId());
    }

    // ---------------------------------------------------------------
    // check-in
    // ---------------------------------------------------------------

    @Test
    void deveRealizarCheckInComSucesso() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;
        Long tecnicoId = 30L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.ATRIBUIDO);

        Usuario usuario = criarUsuario(20L, "Técnico");
        Tecnico tecnico = criarTecnico(tecnicoId, usuario, true);

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, chamado, unidade, tecnico);

        when(ordemServicoRepository.findByIdAndUnidadeAtendimentoContratoId(
                ordemServicoId, contratoId
        )).thenReturn(Optional.of(ordemServico));

        when(ordemServicoRepository.findByTecnicoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(tecnicoId))
                .thenReturn(Optional.empty());

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrdemServicoResponse response = ordemServicoService.realizarCheckIn(
                contratoId, ordemServicoId, new CheckInRequest()
        );

        // Assert
        assertNotNull(response.getDataCheckIn());
        assertEquals(StatusChamado.EM_ATENDIMENTO, chamado.getStatus());

        verify(historicoChamadoService).registrar(
                eq(chamado), eq(ordemServico),
                eq(TipoEventoChamado.ATENDIMENTO_INICIADO), anyString()
        );
    }

    @Test
    void deveBloquearCheckInQuandoOrdemJaFoiEncerrada() {

        // Arrange
        Long contratoId = 1L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");
        Chamado chamado = criarChamado(10L, unidade, StatusChamado.AGUARDANDO_ANALISE);

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, chamado, unidade, null);
        ordemServico.setDataCheckIn(LocalDateTime.now().minusHours(2));
        ordemServico.setDataCheckOut(LocalDateTime.now().minusHours(1));

        when(ordemServicoRepository.findByIdAndUnidadeAtendimentoContratoId(
                ordemServicoId, contratoId
        )).thenReturn(Optional.of(ordemServico));

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.realizarCheckIn(
                        contratoId, ordemServicoId, new CheckInRequest()
                )
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void deveBloquearSegundoCheckInNaMesmaOrdem() {

        // Arrange
        Long contratoId = 1L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");
        Chamado chamado = criarChamado(10L, unidade, StatusChamado.EM_ATENDIMENTO);

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, chamado, unidade, null);
        ordemServico.setDataCheckIn(LocalDateTime.now());

        when(ordemServicoRepository.findByIdAndUnidadeAtendimentoContratoId(
                ordemServicoId, contratoId
        )).thenReturn(Optional.of(ordemServico));

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.realizarCheckIn(
                        contratoId, ordemServicoId, new CheckInRequest()
                )
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void deveBloquearCheckInSemTecnicoAtribuido() {

        // Arrange
        Long contratoId = 1L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");
        Chamado chamado = criarChamado(10L, unidade, StatusChamado.ABERTO);

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, chamado, unidade, null);

        when(ordemServicoRepository.findByIdAndUnidadeAtendimentoContratoId(
                ordemServicoId, contratoId
        )).thenReturn(Optional.of(ordemServico));

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.realizarCheckIn(
                        contratoId, ordemServicoId, new CheckInRequest()
                )
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void deveBloquearCheckInParaTecnicoInativo() {

        // Arrange
        Long contratoId = 1L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");
        Chamado chamado = criarChamado(10L, unidade, StatusChamado.ATRIBUIDO);

        Usuario usuario = criarUsuario(20L, "Técnico Inativo");
        Tecnico tecnicoInativo = criarTecnico(30L, usuario, false);

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, chamado, unidade, tecnicoInativo);

        when(ordemServicoRepository.findByIdAndUnidadeAtendimentoContratoId(
                ordemServicoId, contratoId
        )).thenReturn(Optional.of(ordemServico));

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.realizarCheckIn(
                        contratoId, ordemServicoId, new CheckInRequest()
                )
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void deveBloquearCheckInQuandoTecnicoPossuiOutroAtendimentoAtivoSemConfirmacao() {

        // Arrange
        Long contratoId = 1L;
        Long ordemServicoId = 40L;
        Long tecnicoId = 30L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");
        Chamado chamado = criarChamado(10L, unidade, StatusChamado.ATRIBUIDO);

        Usuario usuario = criarUsuario(20L, "Técnico");
        Tecnico tecnico = criarTecnico(tecnicoId, usuario, true);

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, chamado, unidade, tecnico);

        OrdemServico ordemAtiva =
                criarOrdemServico(41L, chamado, unidade, tecnico);
        ordemAtiva.setDataCheckIn(LocalDateTime.now().minusHours(1));

        when(ordemServicoRepository.findByIdAndUnidadeAtendimentoContratoId(
                ordemServicoId, contratoId
        )).thenReturn(Optional.of(ordemServico));

        when(ordemServicoRepository.findByTecnicoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(tecnicoId))
                .thenReturn(Optional.of(ordemAtiva));

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.realizarCheckIn(
                        contratoId, ordemServicoId, new CheckInRequest()
                )
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(ordemServicoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveEncerrarAtendimentoAnteriorEIniciarNovoQuandoConfirmado() {

        // Arrange
        Long contratoId = 1L;
        Long ordemServicoId = 40L;
        Long tecnicoId = 30L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");
        Chamado chamado = criarChamado(10L, unidade, StatusChamado.ATRIBUIDO);
        Chamado chamadoAnterior = criarChamado(11L, unidade, StatusChamado.EM_ATENDIMENTO);

        Usuario usuario = criarUsuario(20L, "Técnico");
        Tecnico tecnico = criarTecnico(tecnicoId, usuario, true);

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, chamado, unidade, tecnico);

        OrdemServico ordemAtiva =
                criarOrdemServico(41L, chamadoAnterior, unidade, tecnico);
        ordemAtiva.setDataCheckIn(LocalDateTime.now().minusHours(1));

        CheckInRequest request = new CheckInRequest();
        request.setEncerrarCheckInAnterior(true);

        when(ordemServicoRepository.findByIdAndUnidadeAtendimentoContratoId(
                ordemServicoId, contratoId
        )).thenReturn(Optional.of(ordemServico));

        when(ordemServicoRepository.findByTecnicoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(tecnicoId))
                .thenReturn(Optional.of(ordemAtiva));

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(ordemServicoRepository.existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(11L))
                .thenReturn(false);
        when(ordemServicoRepository.existsByChamadoIdAndTecnicoIsNotNullAndDataCheckInIsNullAndDataCheckOutIsNull(11L))
                .thenReturn(false);
        when(ordemServicoRepository.existsByChamadoIdAndTecnicoIsNullAndDataCheckInIsNullAndDataCheckOutIsNull(11L))
                .thenReturn(false);

        // Act
        OrdemServicoResponse response =
                ordemServicoService.realizarCheckIn(contratoId, ordemServicoId, request);

        // Assert
        assertNotNull(response.getDataCheckIn());
        assertNotNull(ordemAtiva.getDataCheckOut());
        assertEquals(StatusChamado.AGUARDANDO_ANALISE, chamadoAnterior.getStatus());

        verify(historicoChamadoService).registrar(
                eq(chamadoAnterior), eq(ordemAtiva),
                eq(TipoEventoChamado.ATENDIMENTO_FINALIZADO_AUTOMATICAMENTE), anyString()
        );
    }

    // ---------------------------------------------------------------
    // check-out
    // ---------------------------------------------------------------

    @Test
    void deveBloquearCheckOutSemCheckIn() {

        // Arrange
        Long contratoId = 1L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");
        Chamado chamado = criarChamado(10L, unidade, StatusChamado.ATRIBUIDO);

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, chamado, unidade, null);

        when(ordemServicoRepository.findByIdAndUnidadeAtendimentoContratoId(
                ordemServicoId, contratoId
        )).thenReturn(Optional.of(ordemServico));

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.realizarCheckOut(contratoId, ordemServicoId)
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void deveBloquearCheckOutDuplicado() {

        // Arrange
        Long contratoId = 1L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");
        Chamado chamado = criarChamado(10L, unidade, StatusChamado.AGUARDANDO_ANALISE);

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, chamado, unidade, null);
        ordemServico.setDataCheckIn(LocalDateTime.now().minusHours(2));
        ordemServico.setDataCheckOut(LocalDateTime.now().minusHours(1));

        when(ordemServicoRepository.findByIdAndUnidadeAtendimentoContratoId(
                ordemServicoId, contratoId
        )).thenReturn(Optional.of(ordemServico));

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.realizarCheckOut(contratoId, ordemServicoId)
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void deveRealizarCheckOutComSucesso() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.EM_ATENDIMENTO);

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, chamado, unidade, null);
        ordemServico.setDataCheckIn(LocalDateTime.now().minusHours(1));

        when(ordemServicoRepository.findByIdAndUnidadeAtendimentoContratoId(
                ordemServicoId, contratoId
        )).thenReturn(Optional.of(ordemServico));

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(ordemServicoRepository.existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(chamadoId))
                .thenReturn(false);
        when(ordemServicoRepository.existsByChamadoIdAndTecnicoIsNotNullAndDataCheckInIsNullAndDataCheckOutIsNull(chamadoId))
                .thenReturn(false);
        when(ordemServicoRepository.existsByChamadoIdAndTecnicoIsNullAndDataCheckInIsNullAndDataCheckOutIsNull(chamadoId))
                .thenReturn(false);

        // Act
        OrdemServicoResponse response =
                ordemServicoService.realizarCheckOut(contratoId, ordemServicoId);

        // Assert
        assertNotNull(response.getDataCheckOut());
        assertEquals(StatusChamado.AGUARDANDO_ANALISE, chamado.getStatus());

        verify(historicoChamadoService).registrar(
                eq(chamado), eq(ordemServico),
                eq(TipoEventoChamado.ATENDIMENTO_FINALIZADO), anyString()
        );
    }

    @Test
    void deveRealizarCheckInEcheckOutDeOrdemAvulsaSemChamado() {

        // Arrange
        Long contratoId = 1L;
        Long ordemServicoId = 40L;
        Long tecnicoId = 30L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");

        Usuario usuario = criarUsuario(20L, "Técnico");
        Tecnico tecnico = criarTecnico(tecnicoId, usuario, true);

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, null, unidade, tecnico);

        when(ordemServicoRepository.findByIdAndUnidadeAtendimentoContratoId(
                ordemServicoId, contratoId
        )).thenReturn(Optional.of(ordemServico));

        when(ordemServicoRepository.findByTecnicoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(tecnicoId))
                .thenReturn(Optional.empty());

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrdemServicoResponse checkInResponse = ordemServicoService.realizarCheckIn(
                contratoId, ordemServicoId, new CheckInRequest()
        );

        OrdemServicoResponse checkOutResponse =
                ordemServicoService.realizarCheckOut(contratoId, ordemServicoId);

        // Assert
        assertNotNull(checkInResponse.getDataCheckIn());
        assertNotNull(checkOutResponse.getDataCheckOut());
        assertNull(checkInResponse.getChamadoId());

        verifyNoInteractions(historicoChamadoService);
        verify(ordemServicoRepository, never())
                .existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(any());
    }

    // ---------------------------------------------------------------
    // buscarEntidadePorId()
    // ---------------------------------------------------------------

    @Test
    void deveLancarNotFoundQuandoOrdemNaoPertenceAoChamadoEContrato() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.empty());

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.buscarEntidadePorId(contratoId, chamadoId, ordemServicoId)
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals(
                "Ordem de serviço não encontrada neste chamado",
                exception.getReason()
        );
    }

    @Test
    void deveLancarNotFoundQuandoOrdemNaoPertenceAoContrato() {

        // Arrange
        Long contratoId = 1L;
        Long ordemServicoId = 40L;

        when(
                ordemServicoRepository.findByIdAndUnidadeAtendimentoContratoId(
                        ordemServicoId, contratoId
                )
        ).thenReturn(Optional.empty());

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.buscarEntidadePorId(contratoId, ordemServicoId)
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals(
                "Ordem de serviço não encontrada neste contrato",
                exception.getReason()
        );
    }

    // ---------------------------------------------------------------
    // listarPorContrato()
    // ---------------------------------------------------------------

    @Test
    void deveListarPorContratoRepassandoFiltrosAoRepository() {

        // Arrange
        Long contratoId = 1L;
        Long tecnicoId = 30L;
        LocalDate data = LocalDate.of(2026, 9, 20);

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade");
        OrdemServico ordemServico =
                criarOrdemServico(40L, null, unidade, null);

        when(
                ordemServicoRepository.buscarPorContratoComFiltros(
                        contratoId, null, tecnicoId, data, null, null
                )
        ).thenReturn(List.of(ordemServico));

        // Act
        List<OrdemServicoResponse> resultado = ordemServicoService.listarPorContrato(
                contratoId, null, tecnicoId, data, null, null
        );

        // Assert
        assertEquals(1, resultado.size());
        verify(ordemServicoRepository).buscarPorContratoComFiltros(
                contratoId, null, tecnicoId, data, null, null
        );
    }

    // ---------------------------------------------------------------
    // Fixtures auxiliares
    // ---------------------------------------------------------------

    private Unidade criarUnidade(Long id, Long contratoId, String nome) {
        Contrato contrato = new Contrato();
        contrato.setId(contratoId);
        contrato.setCidade("Cidade Teste");

        Unidade unidade = new Unidade();
        unidade.setId(id);
        unidade.setNome(nome);
        unidade.setContrato(contrato);
        return unidade;
    }

    private Usuario criarUsuario(Long id, String nome) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        return usuario;
    }

    private Tecnico criarTecnico(Long id, Usuario usuario, boolean ativo) {
        Tecnico tecnico = new Tecnico();
        tecnico.setId(id);
        tecnico.setUsuario(usuario);
        tecnico.setAtivo(ativo);
        return tecnico;
    }

    private Chamado criarChamado(Long id, Unidade unidade, StatusChamado status) {
        Chamado chamado = new Chamado();
        chamado.setId(id);
        chamado.setNumeroChamado("CH-" + id);
        chamado.setUnidade(unidade);
        chamado.setStatus(status);
        return chamado;
    }

    private OrdemServico criarOrdemServico(
            Long id,
            Chamado chamado,
            Unidade unidadeAtendimento,
            Tecnico tecnico
    ) {
        OrdemServico ordemServico = new OrdemServico();
        ordemServico.setId(id);
        ordemServico.setNumeroOrdemServico(id);
        ordemServico.setChamado(chamado);
        ordemServico.setUnidadeAtendimento(unidadeAtendimento);
        ordemServico.setTecnico(tecnico);
        ordemServico.setData(LocalDate.now());
        return ordemServico;
    }
}
