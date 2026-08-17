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

import java.time.LocalDateTime;
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

    @InjectMocks
    private OrdemServicoService ordemServicoService;

    // ---------------------------------------------------------------
    // criar()
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
        request.setNumeroOrdemServico("  OS-100  ");
        request.setTecnicoId(tecnico.getId());
        request.setUnidadeAtendimentoId(unidadeAtendimento.getId());

        when(chamadoService.buscarEntidadePorId(contratoId, chamadoId))
                .thenReturn(chamado);

        when(ordemServicoRepository.existsByNumeroOrdemServico("OS-100"))
                .thenReturn(false);

        when(tecnicoService.buscarEntidadePorId(contratoId, tecnico.getId()))
                .thenReturn(tecnico);

        when(unidadeService.buscarPorId(contratoId, unidadeAtendimento.getId()))
                .thenReturn(unidadeAtendimento);

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> {
                    OrdemServico ordemServico = invocation.getArgument(0);
                    ordemServico.setId(50L);
                    return ordemServico;
                });

        when(
                ordemServicoRepository.existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(
                        chamadoId
                )
        ).thenReturn(false);

        when(
                ordemServicoRepository.existsByChamadoIdAndTecnicoIsNotNullAndDataCheckInIsNullAndDataCheckOutIsNull(
                        chamadoId
                )
        ).thenReturn(true);

        // Act
        OrdemServicoResponse response = ordemServicoService.criar(contratoId, chamadoId, request);

        // Assert
        assertEquals("OS-100", response.getNumeroOrdemServico());
        assertEquals(chamadoId, response.getChamadoId());
        assertEquals(tecnico.getId(), response.getTecnicoId());
        assertEquals(unidadeAtendimento.getId(), response.getUnidadeAtendimentoId());
        assertNotNull(response.getDataAtribuicaoTecnico());
        assertEquals(StatusChamado.ATRIBUIDO, chamado.getStatus());

        verify(tecnicoService).buscarEntidadePorId(contratoId, tecnico.getId());
        verify(unidadeService).buscarPorId(contratoId, unidadeAtendimento.getId());

        ArgumentCaptor<OrdemServico> ordemCaptor = ArgumentCaptor.forClass(OrdemServico.class);
        verify(ordemServicoRepository).saveAndFlush(ordemCaptor.capture());

        OrdemServico ordemSalva = ordemCaptor.getValue();
        assertEquals("OS-100", ordemSalva.getNumeroOrdemServico());
        assertEquals(chamado, ordemSalva.getChamado());
        assertEquals(tecnico, ordemSalva.getTecnico());
        assertEquals(unidadeAtendimento, ordemSalva.getUnidadeAtendimento());

        verify(historicoChamadoService).registrar(
                eq(chamado),
                any(OrdemServico.class),
                eq(TipoEventoChamado.ORDEM_SERVICO_CRIADA),
                anyString()
        );

        verify(historicoChamadoService).registrar(
                eq(chamado),
                any(OrdemServico.class),
                eq(TipoEventoChamado.TECNICO_ATRIBUIDO),
                anyString()
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
        request.setNumeroOrdemServico("OS-200");
        request.setTecnicoId(null);
        request.setUnidadeAtendimentoId(null);

        when(chamadoService.buscarEntidadePorId(contratoId, chamadoId))
                .thenReturn(chamado);

        when(ordemServicoRepository.existsByNumeroOrdemServico("OS-200"))
                .thenReturn(false);

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> {
                    OrdemServico ordemServico = invocation.getArgument(0);
                    ordemServico.setId(51L);
                    return ordemServico;
                });

        when(
                ordemServicoRepository.existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(
                        chamadoId
                )
        ).thenReturn(false);

        when(
                ordemServicoRepository.existsByChamadoIdAndTecnicoIsNotNullAndDataCheckInIsNullAndDataCheckOutIsNull(
                        chamadoId
                )
        ).thenReturn(false);

        when(
                ordemServicoRepository.existsByChamadoIdAndTecnicoIsNullAndDataCheckInIsNullAndDataCheckOutIsNull(
                        chamadoId
                )
        ).thenReturn(true);

        // Act
        OrdemServicoResponse response = ordemServicoService.criar(contratoId, chamadoId, request);

        // Assert
        assertEquals(unidadeChamado.getId(), response.getUnidadeAtendimentoId());
        assertNull(response.getTecnicoId());
        assertNull(response.getDataAtribuicaoTecnico());
        assertEquals(StatusChamado.ABERTO, chamado.getStatus());

        verifyNoInteractions(tecnicoService);
        verifyNoInteractions(unidadeService);

        verify(historicoChamadoService).registrar(
                eq(chamado),
                any(OrdemServico.class),
                eq(TipoEventoChamado.ORDEM_SERVICO_CRIADA),
                anyString()
        );

        verify(historicoChamadoService, never()).registrar(
                any(), any(), eq(TipoEventoChamado.TECNICO_ATRIBUIDO), any()
        );
    }

    @Test
    void deveLancarBadRequestQuandoRequestOuNumeroForInvalido() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;

        OrdemServicoRequest requestComNumeroNulo = new OrdemServicoRequest();
        requestComNumeroNulo.setNumeroOrdemServico(null);

        OrdemServicoRequest requestComNumeroEmBranco = new OrdemServicoRequest();
        requestComNumeroEmBranco.setNumeroOrdemServico("   ");

        // Act
        ResponseStatusException excecaoRequestNulo = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.criar(contratoId, chamadoId, null)
        );

        ResponseStatusException excecaoNumeroNulo = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.criar(contratoId, chamadoId, requestComNumeroNulo)
        );

        ResponseStatusException excecaoNumeroEmBranco = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.criar(contratoId, chamadoId, requestComNumeroEmBranco)
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, excecaoRequestNulo.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, excecaoNumeroNulo.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, excecaoNumeroEmBranco.getStatusCode());

        verifyNoInteractions(chamadoService);
        verify(ordemServicoRepository, never()).saveAndFlush(any(OrdemServico.class));
    }

    @ParameterizedTest
    @EnumSource(value = StatusChamado.class, names = {"FINALIZADO", "CANCELADO"})
    void deveBloquearCriacaoQuandoChamadoFinalizadoOuCancelado(StatusChamado status) {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade do Chamado");
        Chamado chamado = criarChamado(chamadoId, unidade, status);

        OrdemServicoRequest request = new OrdemServicoRequest();
        request.setNumeroOrdemServico("OS-300");

        when(chamadoService.buscarEntidadePorId(contratoId, chamadoId))
                .thenReturn(chamado);

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.criar(contratoId, chamadoId, request)
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(ordemServicoRepository, never()).saveAndFlush(any(OrdemServico.class));
        verifyNoInteractions(tecnicoService);
        verifyNoInteractions(unidadeService);
    }

    @Test
    void deveLancarConflitoQuandoNumeroOrdemServicoJaExistir() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade do Chamado");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.ABERTO);

        OrdemServicoRequest request = new OrdemServicoRequest();
        request.setNumeroOrdemServico("OS-400");
        request.setTecnicoId(30L);
        request.setUnidadeAtendimentoId(7L);

        when(chamadoService.buscarEntidadePorId(contratoId, chamadoId))
                .thenReturn(chamado);

        when(ordemServicoRepository.existsByNumeroOrdemServico("OS-400"))
                .thenReturn(true);

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.criar(contratoId, chamadoId, request)
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Já existe uma ordem de serviço com este número",
                exception.getReason()
        );

        verify(ordemServicoRepository, never()).saveAndFlush(any(OrdemServico.class));
        verifyNoInteractions(tecnicoService);
        verifyNoInteractions(unidadeService);
    }

    // ---------------------------------------------------------------
    // atualizar()
    // ---------------------------------------------------------------

    @Test
    void deveAtualizarNumeroTecnicoEUnidadeAntesDoCheckIn() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;

        Unidade unidadeChamado = criarUnidade(5L, contratoId, "Unidade do Chamado");
        Chamado chamado = criarChamado(chamadoId, unidadeChamado, StatusChamado.ATRIBUIDO);

        Usuario usuarioAnterior = criarUsuario(20L, "Técnico Anterior");
        Tecnico tecnicoAnterior = criarTecnico(30L, usuarioAnterior, true);
        Unidade unidadeAnterior = criarUnidade(7L, contratoId, "Unidade Anterior");

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, chamado, unidadeAnterior, tecnicoAnterior);
        ordemServico.setDataAtribuicaoTecnico(LocalDateTime.of(2026, 1, 1, 8, 0));

        Usuario usuarioNovo = criarUsuario(21L, "Técnico Novo");
        Tecnico tecnicoNovo = criarTecnico(31L, usuarioNovo, true);
        Unidade unidadeNova = criarUnidade(8L, contratoId, "Unidade Nova");

        OrdemServicoRequest request = new OrdemServicoRequest();
        request.setNumeroOrdemServico("OS-500");
        request.setTecnicoId(tecnicoNovo.getId());
        request.setUnidadeAtendimentoId(unidadeNova.getId());

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.of(ordemServico));

        when(
                ordemServicoRepository.existsByNumeroOrdemServicoAndIdNot("OS-500", ordemServicoId)
        ).thenReturn(false);

        when(tecnicoService.buscarEntidadePorId(contratoId, tecnicoNovo.getId()))
                .thenReturn(tecnicoNovo);

        when(unidadeService.buscarPorId(contratoId, unidadeNova.getId()))
                .thenReturn(unidadeNova);

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(
                ordemServicoRepository.existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(
                        chamadoId
                )
        ).thenReturn(false);

        when(
                ordemServicoRepository.existsByChamadoIdAndTecnicoIsNotNullAndDataCheckInIsNullAndDataCheckOutIsNull(
                        chamadoId
                )
        ).thenReturn(true);

        // Act
        OrdemServicoResponse response = ordemServicoService.atualizar(
                contratoId, chamadoId, ordemServicoId, request
        );

        // Assert
        assertEquals("OS-500", response.getNumeroOrdemServico());
        assertEquals(tecnicoNovo.getId(), response.getTecnicoId());
        assertEquals(unidadeNova.getId(), response.getUnidadeAtendimentoId());
        assertNotNull(response.getDataAtribuicaoTecnico());
        assertNotEquals(
                LocalDateTime.of(2026, 1, 1, 8, 0),
                response.getDataAtribuicaoTecnico()
        );
        assertEquals(StatusChamado.ATRIBUIDO, chamado.getStatus());

        verify(tecnicoService).buscarEntidadePorId(contratoId, tecnicoNovo.getId());
        verify(unidadeService).buscarPorId(contratoId, unidadeNova.getId());
        verify(ordemServicoRepository).saveAndFlush(any(OrdemServico.class));

        verify(historicoChamadoService).registrar(
                eq(chamado), eq(ordemServico), eq(TipoEventoChamado.ORDEM_SERVICO_ALTERADA), anyString()
        );

        verify(historicoChamadoService).registrar(
                eq(chamado), eq(ordemServico), eq(TipoEventoChamado.TECNICO_ALTERADO), anyString()
        );

        verify(historicoChamadoService).registrar(
                eq(chamado), eq(ordemServico), eq(TipoEventoChamado.UNIDADE_ORDEM_ALTERADA), anyString()
        );
    }

    @Test
    void deveRemoverTecnicoAntesDoCheckIn() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;

        Unidade unidadeChamado = criarUnidade(5L, contratoId, "Unidade do Chamado");
        Chamado chamado = criarChamado(chamadoId, unidadeChamado, StatusChamado.ATRIBUIDO);

        Usuario usuarioAnterior = criarUsuario(20L, "Técnico Anterior");
        Tecnico tecnicoAnterior = criarTecnico(30L, usuarioAnterior, true);
        Unidade unidadeAtendimento = criarUnidade(7L, contratoId, "Unidade de Atendimento");

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, chamado, unidadeAtendimento, tecnicoAnterior);
        ordemServico.setDataAtribuicaoTecnico(LocalDateTime.of(2026, 1, 1, 8, 0));

        OrdemServicoRequest request = new OrdemServicoRequest();
        request.setNumeroOrdemServico(ordemServico.getNumeroOrdemServico());
        request.setTecnicoId(null);
        request.setUnidadeAtendimentoId(null);

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.of(ordemServico));

        when(
                ordemServicoRepository.existsByNumeroOrdemServicoAndIdNot(
                        ordemServico.getNumeroOrdemServico(), ordemServicoId
                )
        ).thenReturn(false);

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(
                ordemServicoRepository.existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(
                        chamadoId
                )
        ).thenReturn(false);

        when(
                ordemServicoRepository.existsByChamadoIdAndTecnicoIsNotNullAndDataCheckInIsNullAndDataCheckOutIsNull(
                        chamadoId
                )
        ).thenReturn(false);

        when(
                ordemServicoRepository.existsByChamadoIdAndTecnicoIsNullAndDataCheckInIsNullAndDataCheckOutIsNull(
                        chamadoId
                )
        ).thenReturn(true);

        // Act
        OrdemServicoResponse response = ordemServicoService.atualizar(
                contratoId, chamadoId, ordemServicoId, request
        );

        // Assert
        assertNull(response.getTecnicoId());
        assertNull(response.getDataAtribuicaoTecnico());
        assertEquals(unidadeAtendimento.getId(), response.getUnidadeAtendimentoId());
        assertEquals(StatusChamado.ABERTO, chamado.getStatus());

        verifyNoInteractions(tecnicoService);
        verifyNoInteractions(unidadeService);

        verify(historicoChamadoService).registrar(
                eq(chamado), eq(ordemServico), eq(TipoEventoChamado.TECNICO_REMOVIDO), anyString()
        );
    }

    @Test
    void deveBloquearAlteracaoDeTecnicoAposCheckIn() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade do Chamado");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.EM_ATENDIMENTO);

        Usuario usuarioAtual = criarUsuario(20L, "Técnico Atual");
        Tecnico tecnicoAtual = criarTecnico(30L, usuarioAtual, true);

        OrdemServico ordemServico = criarOrdemServico(ordemServicoId, chamado, unidade, tecnicoAtual);
        ordemServico.setDataCheckIn(LocalDateTime.of(2026, 1, 1, 9, 0));

        OrdemServicoRequest request = new OrdemServicoRequest();
        request.setNumeroOrdemServico(ordemServico.getNumeroOrdemServico());
        request.setTecnicoId(99L);
        request.setUnidadeAtendimentoId(unidade.getId());

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.of(ordemServico));

        when(
                ordemServicoRepository.existsByNumeroOrdemServicoAndIdNot(
                        ordemServico.getNumeroOrdemServico(), ordemServicoId
                )
        ).thenReturn(false);

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.atualizar(contratoId, chamadoId, ordemServicoId, request)
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Não é possível alterar o técnico após o check-in",
                exception.getReason()
        );

        verify(ordemServicoRepository, never()).saveAndFlush(any(OrdemServico.class));
    }

    @Test
    void deveBloquearAlteracaoDeUnidadeAposCheckIn() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade do Chamado");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.EM_ATENDIMENTO);

        Usuario usuarioAtual = criarUsuario(20L, "Técnico Atual");
        Tecnico tecnicoAtual = criarTecnico(30L, usuarioAtual, true);

        OrdemServico ordemServico = criarOrdemServico(ordemServicoId, chamado, unidade, tecnicoAtual);
        ordemServico.setDataCheckIn(LocalDateTime.of(2026, 1, 1, 9, 0));

        OrdemServicoRequest request = new OrdemServicoRequest();
        request.setNumeroOrdemServico(ordemServico.getNumeroOrdemServico());
        request.setTecnicoId(tecnicoAtual.getId());
        request.setUnidadeAtendimentoId(999L);

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.of(ordemServico));

        when(
                ordemServicoRepository.existsByNumeroOrdemServicoAndIdNot(
                        ordemServico.getNumeroOrdemServico(), ordemServicoId
                )
        ).thenReturn(false);

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.atualizar(contratoId, chamadoId, ordemServicoId, request)
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Não é possível alterar a unidade após o check-in",
                exception.getReason()
        );

        verify(ordemServicoRepository, never()).saveAndFlush(any(OrdemServico.class));
    }

    @Test
    void deveLancarConflitoQuandoNumeroPertencerAOutraOrdem() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade do Chamado");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.ABERTO);

        OrdemServico ordemServico = criarOrdemServico(ordemServicoId, chamado, unidade, null);

        OrdemServicoRequest request = new OrdemServicoRequest();
        request.setNumeroOrdemServico("OS-999");

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.of(ordemServico));

        when(ordemServicoRepository.existsByNumeroOrdemServicoAndIdNot("OS-999", ordemServicoId))
                .thenReturn(true);

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.atualizar(contratoId, chamadoId, ordemServicoId, request)
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Já existe outra ordem de serviço com este número",
                exception.getReason()
        );

        verify(ordemServicoRepository, never()).saveAndFlush(any(OrdemServico.class));
    }

    @Test
    void deveManterChamadoEmAtendimentoQuandoExisteAtendimentoAtivoAoAtualizarOrdem() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade do Chamado");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.EM_ATENDIMENTO);

        OrdemServico ordemServico = criarOrdemServico(ordemServicoId, chamado, unidade, null);

        OrdemServicoRequest request = new OrdemServicoRequest();
        request.setNumeroOrdemServico(ordemServico.getNumeroOrdemServico());
        request.setTecnicoId(null);
        request.setUnidadeAtendimentoId(null);

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.of(ordemServico));

        when(
                ordemServicoRepository.existsByNumeroOrdemServicoAndIdNot(
                        ordemServico.getNumeroOrdemServico(), ordemServicoId
                )
        ).thenReturn(false);

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(
                ordemServicoRepository.existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(
                        chamadoId
                )
        ).thenReturn(true);

        // Act
        OrdemServicoResponse response = ordemServicoService.atualizar(
                contratoId, chamadoId, ordemServicoId, request
        );

        // Assert
        assertNotNull(response);
        assertEquals(StatusChamado.EM_ATENDIMENTO, chamado.getStatus());

        verify(ordemServicoRepository).saveAndFlush(any(OrdemServico.class));

        verify(ordemServicoRepository, never())
                .existsByChamadoIdAndTecnicoIsNotNullAndDataCheckInIsNullAndDataCheckOutIsNull(any());
        verify(ordemServicoRepository, never())
                .existsByChamadoIdAndTecnicoIsNullAndDataCheckInIsNullAndDataCheckOutIsNull(any());
    }

    // ---------------------------------------------------------------
    // realizarCheckIn()
    // ---------------------------------------------------------------

    @Test
    void deveRealizarCheckInComSucesso() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade do Chamado");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.ATRIBUIDO);

        Usuario usuario = criarUsuario(20L, "Técnico Teste");
        Tecnico tecnico = criarTecnico(30L, usuario, true);

        OrdemServico ordemServico = criarOrdemServico(ordemServicoId, chamado, unidade, tecnico);

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.of(ordemServico));

        when(
                ordemServicoRepository.findByTecnicoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(
                        tecnico.getId()
                )
        ).thenReturn(Optional.empty());

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrdemServicoResponse response = ordemServicoService.realizarCheckIn(
                contratoId, chamadoId, ordemServicoId, new CheckInRequest()
        );

        // Assert
        assertNotNull(response.getDataCheckIn());
        assertEquals(StatusChamado.EM_ATENDIMENTO, chamado.getStatus());

        verify(ordemServicoRepository).saveAndFlush(any(OrdemServico.class));

        verify(historicoChamadoService).registrar(
                eq(chamado), eq(ordemServico), eq(TipoEventoChamado.ATENDIMENTO_INICIADO), anyString()
        );
    }

    @Test
    void deveBloquearCheckInQuandoOrdemJaFoiEncerrada() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade do Chamado");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.AGUARDANDO_ANALISE);

        OrdemServico ordemServico = criarOrdemServico(ordemServicoId, chamado, unidade, null);
        ordemServico.setDataCheckIn(LocalDateTime.of(2026, 1, 1, 9, 0));
        ordemServico.setDataCheckOut(LocalDateTime.of(2026, 1, 1, 12, 0));

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.of(ordemServico));

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.realizarCheckIn(
                        contratoId, chamadoId, ordemServicoId, new CheckInRequest()
                )
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Esta ordem de serviço já foi encerrada",
                exception.getReason()
        );

        verify(ordemServicoRepository, never()).saveAndFlush(any(OrdemServico.class));
    }

    @Test
    void deveBloquearSegundoCheckInNaMesmaOrdem() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade do Chamado");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.EM_ATENDIMENTO);

        OrdemServico ordemServico = criarOrdemServico(ordemServicoId, chamado, unidade, null);
        ordemServico.setDataCheckIn(LocalDateTime.of(2026, 1, 1, 9, 0));

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.of(ordemServico));

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.realizarCheckIn(
                        contratoId, chamadoId, ordemServicoId, new CheckInRequest()
                )
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Esta ordem de serviço já possui um check-in ativo",
                exception.getReason()
        );

        verify(ordemServicoRepository, never()).saveAndFlush(any(OrdemServico.class));
    }

    @Test
    void deveBloquearCheckInSemTecnicoAtribuido() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade do Chamado");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.ABERTO);

        OrdemServico ordemServico = criarOrdemServico(ordemServicoId, chamado, unidade, null);

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.of(ordemServico));

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.realizarCheckIn(
                        contratoId, chamadoId, ordemServicoId, new CheckInRequest()
                )
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Não é possível efetuar check-in sem um técnico atribuído",
                exception.getReason()
        );

        verify(ordemServicoRepository, never())
                .findByTecnicoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(any());
        verify(ordemServicoRepository, never()).saveAndFlush(any(OrdemServico.class));
    }

    @Test
    void deveBloquearCheckInParaTecnicoInativo() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade do Chamado");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.ATRIBUIDO);

        Usuario usuario = criarUsuario(20L, "Técnico Inativo");
        Tecnico tecnicoInativo = criarTecnico(30L, usuario, false);

        OrdemServico ordemServico =
                criarOrdemServico(ordemServicoId, chamado, unidade, tecnicoInativo);

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.of(ordemServico));

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.realizarCheckIn(
                        contratoId, chamadoId, ordemServicoId, new CheckInRequest()
                )
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Não é possível efetuar check-in para um técnico inativo",
                exception.getReason()
        );

        verify(ordemServicoRepository, never())
                .findByTecnicoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(any());
        verify(ordemServicoRepository, never()).saveAndFlush(any(OrdemServico.class));
    }

    @Test
    void deveBloquearCheckInQuandoTecnicoPossuiOutroAtendimentoAtivoSemConfirmacao() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade do Chamado");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.ATRIBUIDO);

        Usuario usuario = criarUsuario(20L, "Técnico Teste");
        Tecnico tecnico = criarTecnico(30L, usuario, true);

        OrdemServico ordemServico = criarOrdemServico(ordemServicoId, chamado, unidade, tecnico);

        Chamado chamadoAnterior = criarChamado(11L, unidade, StatusChamado.EM_ATENDIMENTO);
        OrdemServico ordemAtivaAnterior =
                criarOrdemServico(41L, chamadoAnterior, unidade, tecnico);
        ordemAtivaAnterior.setDataCheckIn(LocalDateTime.of(2026, 1, 1, 8, 0));

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.of(ordemServico));

        when(
                ordemServicoRepository.findByTecnicoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(
                        tecnico.getId()
                )
        ).thenReturn(Optional.of(ordemAtivaAnterior));

        // Act - encerrarCheckInAnterior ausente (não confirmado)
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.realizarCheckIn(
                        contratoId, chamadoId, ordemServicoId, new CheckInRequest()
                )
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertNull(ordemAtivaAnterior.getDataCheckOut());
        assertNull(ordemServico.getDataCheckIn());

        verify(ordemServicoRepository, never()).saveAndFlush(any(OrdemServico.class));
    }

    @Test
    void deveEncerrarAtendimentoAnteriorEIniciarNovoQuandoConfirmado() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoAtualId = 10L;
        Long chamadoAnteriorId = 11L;
        Long ordemServicoId = 40L;
        Long ordemAnteriorId = 41L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade Central");

        Chamado chamadoAtual = criarChamado(chamadoAtualId, unidade, StatusChamado.ATRIBUIDO);
        Chamado chamadoAnterior = criarChamado(chamadoAnteriorId, unidade, StatusChamado.EM_ATENDIMENTO);

        Usuario usuario = criarUsuario(20L, "Técnico Teste");
        Tecnico tecnico = criarTecnico(30L, usuario, true);

        OrdemServico ordemAtual = criarOrdemServico(ordemServicoId, chamadoAtual, unidade, tecnico);

        OrdemServico ordemAnterior =
                criarOrdemServico(ordemAnteriorId, chamadoAnterior, unidade, tecnico);
        ordemAnterior.setDataCheckIn(LocalDateTime.of(2026, 1, 1, 8, 0));

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoAtualId, contratoId
                )
        ).thenReturn(Optional.of(ordemAtual));

        when(
                ordemServicoRepository.findByTecnicoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(
                        tecnico.getId()
                )
        ).thenReturn(Optional.of(ordemAnterior));

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(
                ordemServicoRepository.existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(
                        chamadoAnteriorId
                )
        ).thenReturn(false);

        when(
                ordemServicoRepository.existsByChamadoIdAndTecnicoIsNotNullAndDataCheckInIsNullAndDataCheckOutIsNull(
                        chamadoAnteriorId
                )
        ).thenReturn(false);

        when(
                ordemServicoRepository.existsByChamadoIdAndTecnicoIsNullAndDataCheckInIsNullAndDataCheckOutIsNull(
                        chamadoAnteriorId
                )
        ).thenReturn(false);

        CheckInRequest request = new CheckInRequest();
        request.setEncerrarCheckInAnterior(true);

        // Act
        OrdemServicoResponse response = ordemServicoService.realizarCheckIn(
                contratoId, chamadoAtualId, ordemServicoId, request
        );

        // Assert
        assertNotNull(ordemAnterior.getDataCheckOut());
        assertEquals(StatusChamado.AGUARDANDO_ANALISE, chamadoAnterior.getStatus());

        assertNotNull(response.getDataCheckIn());
        assertEquals(StatusChamado.EM_ATENDIMENTO, chamadoAtual.getStatus());

        verify(ordemServicoRepository, times(2)).saveAndFlush(any(OrdemServico.class));

        verify(historicoChamadoService).registrar(
                eq(chamadoAnterior),
                eq(ordemAnterior),
                eq(TipoEventoChamado.ATENDIMENTO_FINALIZADO_AUTOMATICAMENTE),
                anyString()
        );

        verify(historicoChamadoService).registrar(
                eq(chamadoAtual),
                eq(ordemAtual),
                eq(TipoEventoChamado.ATENDIMENTO_INICIADO),
                anyString()
        );
    }

    // ---------------------------------------------------------------
    // realizarCheckOut()
    // ---------------------------------------------------------------

    @Test
    void deveBloquearCheckOutSemCheckIn() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade do Chamado");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.ATRIBUIDO);

        OrdemServico ordemServico = criarOrdemServico(ordemServicoId, chamado, unidade, null);

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.of(ordemServico));

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.realizarCheckOut(contratoId, chamadoId, ordemServicoId)
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Não é possível realizar check-out sem um check-in",
                exception.getReason()
        );

        verify(ordemServicoRepository, never()).saveAndFlush(any(OrdemServico.class));
    }

    @Test
    void deveBloquearCheckOutDuplicado() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade do Chamado");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.EM_ATENDIMENTO);

        OrdemServico ordemServico = criarOrdemServico(ordemServicoId, chamado, unidade, null);
        ordemServico.setDataCheckIn(LocalDateTime.of(2026, 1, 1, 9, 0));
        ordemServico.setDataCheckOut(LocalDateTime.of(2026, 1, 1, 12, 0));

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.of(ordemServico));

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> ordemServicoService.realizarCheckOut(contratoId, chamadoId, ordemServicoId)
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Esta ordem de serviço já possui check-out",
                exception.getReason()
        );

        verify(ordemServicoRepository, never()).saveAndFlush(any(OrdemServico.class));
    }

    @Test
    void deveRealizarCheckOutComSucesso() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 40L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade do Chamado");
        Chamado chamado = criarChamado(chamadoId, unidade, StatusChamado.EM_ATENDIMENTO);

        Usuario usuario = criarUsuario(20L, "Técnico Teste");
        Tecnico tecnico = criarTecnico(30L, usuario, true);

        OrdemServico ordemServico = criarOrdemServico(ordemServicoId, chamado, unidade, tecnico);
        ordemServico.setDataCheckIn(LocalDateTime.of(2026, 1, 1, 9, 0));

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.of(ordemServico));

        when(ordemServicoRepository.saveAndFlush(any(OrdemServico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(
                ordemServicoRepository.existsByChamadoIdAndDataCheckInIsNotNullAndDataCheckOutIsNull(
                        chamadoId
                )
        ).thenReturn(false);

        when(
                ordemServicoRepository.existsByChamadoIdAndTecnicoIsNotNullAndDataCheckInIsNullAndDataCheckOutIsNull(
                        chamadoId
                )
        ).thenReturn(false);

        when(
                ordemServicoRepository.existsByChamadoIdAndTecnicoIsNullAndDataCheckInIsNullAndDataCheckOutIsNull(
                        chamadoId
                )
        ).thenReturn(false);

        // Act
        OrdemServicoResponse response = ordemServicoService.realizarCheckOut(
                contratoId, chamadoId, ordemServicoId
        );

        // Assert
        assertNotNull(response.getDataCheckOut());
        assertEquals(StatusChamado.AGUARDANDO_ANALISE, chamado.getStatus());

        verify(ordemServicoRepository).saveAndFlush(any(OrdemServico.class));

        verify(historicoChamadoService).registrar(
                eq(chamado), eq(ordemServico), eq(TipoEventoChamado.ATENDIMENTO_FINALIZADO), anyString()
        );
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
        ordemServico.setNumeroOrdemServico("OS-" + id);
        ordemServico.setChamado(chamado);
        ordemServico.setUnidadeAtendimento(unidadeAtendimento);
        ordemServico.setTecnico(tecnico);
        return ordemServico;
    }
}
