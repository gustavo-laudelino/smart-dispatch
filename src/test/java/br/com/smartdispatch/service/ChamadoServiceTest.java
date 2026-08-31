package br.com.smartdispatch.service;

import br.com.smartdispatch.dto.ChamadoRequest;
import br.com.smartdispatch.dto.ChamadoResponse;
import br.com.smartdispatch.dto.StatusChamadoRequest;
import br.com.smartdispatch.enums.CategoriaChamado;
import br.com.smartdispatch.enums.PrioridadeChamado;
import br.com.smartdispatch.enums.StatusChamado;
import br.com.smartdispatch.enums.TipoChamado;
import br.com.smartdispatch.enums.TipoEventoChamado;
import br.com.smartdispatch.model.Chamado;
import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.model.Solicitante;
import br.com.smartdispatch.model.Unidade;
import br.com.smartdispatch.repository.ChamadoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChamadoServiceTest {

    @Mock
    private ChamadoRepository chamadoRepository;

    @Mock
    private UnidadeService unidadeService;

    @Mock
    private ContratoService contratoService;

    @Mock
    private HistoricoChamadoService historicoChamadoService;

    @InjectMocks
    private ChamadoService chamadoService;

    @Test
    void deveBloquearEdicaoDeChamadoFinalizadoParaTecnico() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 14L;

        Chamado chamado = new Chamado();
        chamado.setId(chamadoId);
        chamado.setStatus(StatusChamado.FINALIZADO);

        when(
                chamadoRepository.findByIdAndUnidadeContratoId(
                        chamadoId,
                        contratoId
                )
        ).thenReturn(Optional.of(chamado));

        Authentication authentication =
                new TestingAuthenticationToken(
                        "tecnico",
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_TECNICO"
                                )
                        )
                );

        ChamadoRequest request =
                new ChamadoRequest();

        // Act
        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> chamadoService.atualizar(
                                contratoId,
                                chamadoId,
                                request,
                                authentication
                        )
                );

        // Assert
        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );

        assertEquals(
                "Chamado finalizado não permite edição",
                exception.getReason()
        );

        verify(
                chamadoRepository,
                never()
        ).save(any(Chamado.class));
    }

    // ---------------------------------------------------------------
    // criar()
    // ---------------------------------------------------------------

    @Test
    void deveCriarChamadoComSucessoQuandoDadosValidos() {

        // Arrange
        Long contratoId = 1L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade Central");
        ChamadoRequest request = criarRequestValido(unidade.getId());

        when(unidadeService.buscarPorId(contratoId, unidade.getId()))
                .thenReturn(unidade);

        when(
                chamadoRepository.existsByNumeroChamadoAndUnidadeContratoId(
                        request.getNumeroChamado(),
                        contratoId
                )
        ).thenReturn(false);

        when(chamadoRepository.save(any(Chamado.class)))
                .thenAnswer(invocation -> {
                    Chamado chamado = invocation.getArgument(0);
                    chamado.setId(99L);
                    return chamado;
                });

        // Act
        ChamadoResponse response = chamadoService.criar(contratoId, request);

        // Assert
        assertEquals(99L, response.getId());
        assertEquals(request.getNumeroChamado(), response.getNumeroChamado());
        assertEquals(request.getLinkChamadoOsti(), response.getLinkChamadoOsti());
        assertEquals(StatusChamado.ABERTO, response.getStatus());
        assertEquals(unidade.getId(), response.getUnidadeId());
        assertEquals(unidade.getNome(), response.getUnidadeNome());
        assertEquals(contratoId, response.getContratoId());
        assertEquals(request.getNumeroPatrimonio(), response.getNumeroPatrimonio());
        assertEquals(request.getTipo(), response.getTipo());
        assertEquals(request.getCategoria(), response.getCategoria());
        assertEquals(request.getPrioridade(), response.getPrioridade());
        assertEquals(request.getDescricao(), response.getDescricao());
        assertEquals(
                request.getSolicitante().getNome(),
                response.getSolicitante().getNome()
        );
        assertEquals(
                request.getSolicitante().getEmail(),
                response.getSolicitante().getEmail()
        );
        assertNotNull(response.getDataAbertura());

        verify(chamadoRepository).save(any(Chamado.class));

        verify(historicoChamadoService).registrar(
                any(Chamado.class),
                isNull(),
                eq(TipoEventoChamado.CHAMADO_CRIADO),
                anyString()
        );
    }

    @Test
    void deveLancarConflitoQuandoNumeroChamadoJaCadastradoNoContrato() {

        // Arrange
        Long contratoId = 1L;

        Unidade unidade = criarUnidade(5L, contratoId, "Unidade Central");
        ChamadoRequest request = criarRequestValido(unidade.getId());

        when(unidadeService.buscarPorId(contratoId, unidade.getId()))
                .thenReturn(unidade);

        when(
                chamadoRepository.existsByNumeroChamadoAndUnidadeContratoId(
                        request.getNumeroChamado(),
                        contratoId
                )
        ).thenReturn(true);

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> chamadoService.criar(contratoId, request)
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Já existe um chamado com este número neste contrato",
                exception.getReason()
        );

        verify(chamadoRepository, never()).save(any(Chamado.class));
        verifyNoInteractions(historicoChamadoService);
    }

    // ---------------------------------------------------------------
    // listarPorContrato()
    // ---------------------------------------------------------------

    @Test
    void deveListarChamadosOrdenadosPorDataAberturaDecrescente() {

        // Arrange
        Long contratoId = 1L;
        Unidade unidade = criarUnidade(5L, contratoId, "Unidade Central");

        Chamado chamadoAntigo = criarChamadoValido(1L, unidade, StatusChamado.ABERTO);
        chamadoAntigo.setDataAbertura(LocalDateTime.of(2026, 1, 1, 10, 0));

        Chamado chamadoRecente = criarChamadoValido(2L, unidade, StatusChamado.ABERTO);
        chamadoRecente.setDataAbertura(LocalDateTime.of(2026, 6, 1, 10, 0));

        when(contratoService.buscarPorId(contratoId))
                .thenReturn(unidade.getContrato());

        when(chamadoRepository.findByUnidadeContratoId(contratoId))
                .thenReturn(List.of(chamadoAntigo, chamadoRecente));

        // Act
        List<ChamadoResponse> resultado = chamadoService.listarPorContrato(contratoId);

        // Assert
        assertEquals(2, resultado.size());
        assertEquals(chamadoRecente.getId(), resultado.get(0).getId());
        assertEquals(chamadoAntigo.getId(), resultado.get(1).getId());
    }

    // ---------------------------------------------------------------
    // listarFeed()
    // ---------------------------------------------------------------

    @Test
    void deveBuscarFeedPorContratoQuandoContratoIdInformado() {

        // Arrange
        Long contratoId = 1L;
        Unidade unidade = criarUnidade(5L, contratoId, "Unidade Central");
        Chamado chamado = criarChamadoValido(1L, unidade, StatusChamado.ABERTO);

        Page<Chamado> pagina = new PageImpl<>(List.of(chamado));

        when(
                chamadoRepository.findByUnidadeContratoId(
                        eq(contratoId),
                        any(Pageable.class)
                )
        ).thenReturn(pagina);

        // Act
        Page<ChamadoResponse> resultado =
                chamadoService.listarFeed(contratoId, 0, 10, "desc");

        // Assert
        assertEquals(1, resultado.getTotalElements());
        assertEquals(chamado.getId(), resultado.getContent().get(0).getId());

        verify(chamadoRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void deveBuscarFeedGlobalQuandoContratoIdNulo() {

        // Arrange
        Unidade unidade = criarUnidade(5L, 1L, "Unidade Central");
        Chamado chamado = criarChamadoValido(1L, unidade, StatusChamado.ABERTO);

        Page<Chamado> pagina = new PageImpl<>(List.of(chamado));

        when(chamadoRepository.findAll(any(Pageable.class)))
                .thenReturn(pagina);

        // Act
        Page<ChamadoResponse> resultado =
                chamadoService.listarFeed(null, 0, 10, "desc");

        // Assert
        assertEquals(1, resultado.getTotalElements());

        verify(chamadoRepository, never())
                .findByUnidadeContratoId(any(Long.class), any(Pageable.class));
    }

    @Test
    void deveDefinirDirecaoDeOrdenacaoCorretamente() {

        // Arrange
        Long contratoId = 1L;
        Page<Chamado> paginaVazia = new PageImpl<>(List.of());

        when(
                chamadoRepository.findByUnidadeContratoId(
                        eq(contratoId),
                        any(Pageable.class)
                )
        ).thenReturn(paginaVazia);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        // Act
        chamadoService.listarFeed(contratoId, 0, 10, "asc");
        chamadoService.listarFeed(contratoId, 0, 10, "qualquer-outro-valor");

        // Assert
        verify(chamadoRepository, times(2))
                .findByUnidadeContratoId(eq(contratoId), captor.capture());

        List<Pageable> paginacoes = captor.getAllValues();

        assertEquals(
                Sort.Direction.ASC,
                paginacoes.get(0).getSort().getOrderFor("dataAbertura").getDirection()
        );

        assertEquals(
                Sort.Direction.DESC,
                paginacoes.get(1).getSort().getOrderFor("dataAbertura").getDirection()
        );
    }

    // ---------------------------------------------------------------
    // buscarPorId()
    // ---------------------------------------------------------------

    @Test
    void deveLancarNotFoundQuandoChamadoNaoPertenceAoContrato() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 14L;

        when(chamadoRepository.findByIdAndUnidadeContratoId(chamadoId, contratoId))
                .thenReturn(Optional.empty());

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> chamadoService.buscarPorId(contratoId, chamadoId)
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals(
                "Chamado não encontrado neste contrato",
                exception.getReason()
        );
    }

    // ---------------------------------------------------------------
    // atualizar()
    // ---------------------------------------------------------------

    @Test
    void devePermitirEdicaoDeChamadoFinalizadoParaGestor() {

        // Arrange
        Long contratoId = 1L;
        Unidade unidade = criarUnidade(5L, contratoId, "Unidade Central");
        Chamado chamado = criarChamadoValido(14L, unidade, StatusChamado.FINALIZADO);

        ChamadoRequest request = criarRequestParaChamado(chamado);
        request.setDescricao("Descrição atualizada pelo gestor");

        when(chamadoRepository.findByIdAndUnidadeContratoId(chamado.getId(), contratoId))
                .thenReturn(Optional.of(chamado));

        when(unidadeService.buscarPorId(contratoId, unidade.getId()))
                .thenReturn(unidade);

        when(
                chamadoRepository.existsByNumeroChamadoAndUnidadeContratoIdAndIdNot(
                        request.getNumeroChamado(),
                        contratoId,
                        chamado.getId()
                )
        ).thenReturn(false);

        when(chamadoRepository.save(any(Chamado.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Authentication authentication = authenticationComRole("ROLE_ADMIN");

        // Act
        ChamadoResponse response = chamadoService.atualizar(
                contratoId,
                chamado.getId(),
                request,
                authentication
        );

        // Assert
        assertEquals("Descrição atualizada pelo gestor", response.getDescricao());

        verify(chamadoRepository).save(any(Chamado.class));

        verify(historicoChamadoService).registrar(
                any(Chamado.class),
                isNull(),
                eq(TipoEventoChamado.DADOS_CHAMADO_ALTERADOS),
                anyString()
        );
    }

    @Test
    void deveLancarConflitoQuandoNumeroPertenceAOutroChamadoNoContrato() {

        // Arrange
        Long contratoId = 1L;
        Unidade unidade = criarUnidade(5L, contratoId, "Unidade Central");
        Chamado chamado = criarChamadoValido(14L, unidade, StatusChamado.ABERTO);

        ChamadoRequest request = criarRequestParaChamado(chamado);

        when(chamadoRepository.findByIdAndUnidadeContratoId(chamado.getId(), contratoId))
                .thenReturn(Optional.of(chamado));

        when(unidadeService.buscarPorId(contratoId, unidade.getId()))
                .thenReturn(unidade);

        when(
                chamadoRepository.existsByNumeroChamadoAndUnidadeContratoIdAndIdNot(
                        request.getNumeroChamado(),
                        contratoId,
                        chamado.getId()
                )
        ).thenReturn(true);

        Authentication authentication = authenticationComRole("ROLE_ADMIN");

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> chamadoService.atualizar(
                        contratoId,
                        chamado.getId(),
                        request,
                        authentication
                )
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Já existe outro chamado com este número neste contrato",
                exception.getReason()
        );

        verify(chamadoRepository, never()).save(any(Chamado.class));
    }

    @Test
    void naoDeveRegistrarHistoricoQuandoRequestNaoAlteraNenhumDado() {

        // Arrange
        Long contratoId = 1L;
        Unidade unidade = criarUnidade(5L, contratoId, "Unidade Central");
        Chamado chamado = criarChamadoValido(14L, unidade, StatusChamado.ABERTO);

        ChamadoRequest request = criarRequestParaChamado(chamado);

        when(chamadoRepository.findByIdAndUnidadeContratoId(chamado.getId(), contratoId))
                .thenReturn(Optional.of(chamado));

        when(unidadeService.buscarPorId(contratoId, unidade.getId()))
                .thenReturn(unidade);

        when(
                chamadoRepository.existsByNumeroChamadoAndUnidadeContratoIdAndIdNot(
                        request.getNumeroChamado(),
                        contratoId,
                        chamado.getId()
                )
        ).thenReturn(false);

        when(chamadoRepository.save(any(Chamado.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Authentication authentication = authenticationComRole("ROLE_ADMIN");

        // Act
        chamadoService.atualizar(contratoId, chamado.getId(), request, authentication);

        // Assert
        verify(chamadoRepository).save(any(Chamado.class));
        verifyNoInteractions(historicoChamadoService);
    }

    @Test
    void devePermitirCtoEditarChamadoFinalizado() {

        // Arrange
        Long contratoId = 1L;
        Unidade unidade = criarUnidade(5L, contratoId, "Unidade Central");
        Chamado chamado = criarChamadoValido(14L, unidade, StatusChamado.FINALIZADO);

        ChamadoRequest request = criarRequestParaChamado(chamado);
        request.setDescricao("Descrição atualizada pelo CTO");

        when(chamadoRepository.findByIdAndUnidadeContratoId(chamado.getId(), contratoId))
                .thenReturn(Optional.of(chamado));

        when(unidadeService.buscarPorId(contratoId, unidade.getId()))
                .thenReturn(unidade);

        when(
                chamadoRepository.existsByNumeroChamadoAndUnidadeContratoIdAndIdNot(
                        request.getNumeroChamado(),
                        contratoId,
                        chamado.getId()
                )
        ).thenReturn(false);

        when(chamadoRepository.save(any(Chamado.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Authentication authentication = authenticationComRole("ROLE_CTO");

        // Act
        ChamadoResponse response = chamadoService.atualizar(
                contratoId,
                chamado.getId(),
                request,
                authentication
        );

        // Assert
        assertEquals("Descrição atualizada pelo CTO", response.getDescricao());

        verify(chamadoRepository).save(any(Chamado.class));

        verify(historicoChamadoService).registrar(
                any(Chamado.class),
                isNull(),
                eq(TipoEventoChamado.DADOS_CHAMADO_ALTERADOS),
                anyString()
        );
    }

    @Test
    void deveRegistrarHistoricoAoAlterarNumeroChamadoEUnidade() {

        // Arrange
        Long contratoId = 1L;
        Unidade unidadeOriginal = criarUnidade(5L, contratoId, "Unidade Central");
        Unidade novaUnidade = criarUnidade(6L, contratoId, "Unidade Norte");

        Chamado chamado = criarChamadoValido(14L, unidadeOriginal, StatusChamado.ABERTO);

        ChamadoRequest request = criarRequestParaChamado(chamado);
        request.setNumeroChamado("CH-NOVO-14");
        request.setUnidadeId(novaUnidade.getId());

        when(chamadoRepository.findByIdAndUnidadeContratoId(chamado.getId(), contratoId))
                .thenReturn(Optional.of(chamado));

        when(unidadeService.buscarPorId(contratoId, novaUnidade.getId()))
                .thenReturn(novaUnidade);

        when(
                chamadoRepository.existsByNumeroChamadoAndUnidadeContratoIdAndIdNot(
                        "CH-NOVO-14",
                        contratoId,
                        chamado.getId()
                )
        ).thenReturn(false);

        when(chamadoRepository.save(any(Chamado.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Authentication authentication = authenticationComRole("ROLE_ADMIN");

        // Act
        ChamadoResponse response = chamadoService.atualizar(
                contratoId, chamado.getId(), request, authentication
        );

        // Assert
        assertEquals("CH-NOVO-14", response.getNumeroChamado());
        assertEquals(novaUnidade.getId(), response.getUnidadeId());

        verify(chamadoRepository).save(any(Chamado.class));

        ArgumentCaptor<String> descricaoCaptor = ArgumentCaptor.forClass(String.class);

        verify(historicoChamadoService).registrar(
                any(Chamado.class),
                isNull(),
                eq(TipoEventoChamado.DADOS_CHAMADO_ALTERADOS),
                descricaoCaptor.capture()
        );

        String descricao = descricaoCaptor.getValue();
        assertTrue(descricao.contains("número do chamado de CH-14 para CH-NOVO-14"));
        assertTrue(descricao.contains("unidade de Unidade Central para Unidade Norte"));
    }

    @Test
    void deveRegistrarHistoricoAoPreencherPatrimonioEAlterarTipoECategoria() {

        // Arrange
        Long contratoId = 1L;
        Unidade unidade = criarUnidade(5L, contratoId, "Unidade Central");
        Chamado chamado = criarChamadoValido(14L, unidade, StatusChamado.ABERTO);
        chamado.setNumeroPatrimonio(null);

        ChamadoRequest request = criarRequestParaChamado(chamado);
        request.setNumeroPatrimonio("PAT-NOVO-14");
        request.setTipo(TipoChamado.REQUISICAO);
        request.setCategoria(CategoriaChamado.INSTALACAO_DE_PROGRAMAS);

        when(chamadoRepository.findByIdAndUnidadeContratoId(chamado.getId(), contratoId))
                .thenReturn(Optional.of(chamado));

        when(unidadeService.buscarPorId(contratoId, unidade.getId()))
                .thenReturn(unidade);

        when(
                chamadoRepository.existsByNumeroChamadoAndUnidadeContratoIdAndIdNot(
                        request.getNumeroChamado(),
                        contratoId,
                        chamado.getId()
                )
        ).thenReturn(false);

        when(chamadoRepository.save(any(Chamado.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Authentication authentication = authenticationComRole("ROLE_ADMIN");

        // Act
        ChamadoResponse response = chamadoService.atualizar(
                contratoId, chamado.getId(), request, authentication
        );

        // Assert
        assertEquals("PAT-NOVO-14", response.getNumeroPatrimonio());
        assertEquals(TipoChamado.REQUISICAO, response.getTipo());
        assertEquals(CategoriaChamado.INSTALACAO_DE_PROGRAMAS, response.getCategoria());

        ArgumentCaptor<String> descricaoCaptor = ArgumentCaptor.forClass(String.class);

        verify(historicoChamadoService).registrar(
                any(Chamado.class),
                isNull(),
                eq(TipoEventoChamado.DADOS_CHAMADO_ALTERADOS),
                descricaoCaptor.capture()
        );

        String descricao = descricaoCaptor.getValue();
        assertTrue(descricao.contains("patrimônio de não informado para PAT-NOVO-14"));
        assertTrue(descricao.contains("tipo de Incidente para Requisição"));
        assertTrue(descricao.contains("categoria de Outros para Instalação de programas"));
    }

    @Test
    void deveRegistrarHistoricoAoPreencherSolicitanteAnteriormenteAusente() {

        // Arrange
        Long contratoId = 1L;
        Unidade unidade = criarUnidade(5L, contratoId, "Unidade Central");
        Chamado chamado = criarChamadoValido(14L, unidade, StatusChamado.ABERTO);

        ChamadoRequest request = criarRequestParaChamado(chamado);

        chamado.setSolicitante(null);

        Solicitante novoSolicitante = new Solicitante();
        novoSolicitante.setNome("Maria Solicitante");
        novoSolicitante.setEmail("maria@exemplo.com");
        novoSolicitante.setTelefone("11988887777");
        novoSolicitante.setIdentificacao("98765432100");

        request.setSolicitante(novoSolicitante);

        when(chamadoRepository.findByIdAndUnidadeContratoId(chamado.getId(), contratoId))
                .thenReturn(Optional.of(chamado));

        when(unidadeService.buscarPorId(contratoId, unidade.getId()))
                .thenReturn(unidade);

        when(
                chamadoRepository.existsByNumeroChamadoAndUnidadeContratoIdAndIdNot(
                        request.getNumeroChamado(),
                        contratoId,
                        chamado.getId()
                )
        ).thenReturn(false);

        when(chamadoRepository.save(any(Chamado.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Authentication authentication = authenticationComRole("ROLE_ADMIN");

        // Act
        ChamadoResponse response = chamadoService.atualizar(
                contratoId, chamado.getId(), request, authentication
        );

        // Assert
        assertEquals("Maria Solicitante", response.getSolicitante().getNome());

        ArgumentCaptor<String> descricaoCaptor = ArgumentCaptor.forClass(String.class);

        verify(historicoChamadoService).registrar(
                any(Chamado.class),
                isNull(),
                eq(TipoEventoChamado.DADOS_CHAMADO_ALTERADOS),
                descricaoCaptor.capture()
        );

        String descricao = descricaoCaptor.getValue();
        assertTrue(descricao.contains("solicitante de não informado para Maria Solicitante"));
        assertTrue(descricao.contains("e-mail do solicitante atualizado"));
        assertTrue(descricao.contains("telefone do solicitante atualizado"));
        assertTrue(descricao.contains("identificação do solicitante atualizada"));
    }

    // ---------------------------------------------------------------
    // atualizarStatus()
    // ---------------------------------------------------------------

    @Test
    void deveLancarBadRequestQuandoRequestOuStatusNulo() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 14L;
        Authentication authentication = authenticationComRole("ROLE_ADMIN");

        StatusChamadoRequest requestComStatusNulo = new StatusChamadoRequest();

        // Act - request nulo
        ResponseStatusException excecaoRequestNulo = assertThrows(
                ResponseStatusException.class,
                () -> chamadoService.atualizarStatus(
                        contratoId, chamadoId, null, authentication
                )
        );

        // Act - status nulo dentro do request
        ResponseStatusException excecaoStatusNulo = assertThrows(
                ResponseStatusException.class,
                () -> chamadoService.atualizarStatus(
                        contratoId, chamadoId, requestComStatusNulo, authentication
                )
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, excecaoRequestNulo.getStatusCode());
        assertEquals(
                "O status do chamado deve ser informado",
                excecaoRequestNulo.getReason()
        );

        assertEquals(HttpStatus.BAD_REQUEST, excecaoStatusNulo.getStatusCode());
        assertEquals(
                "O status do chamado deve ser informado",
                excecaoStatusNulo.getReason()
        );

        verify(chamadoRepository, never())
                .findByIdAndUnidadeContratoId(any(Long.class), any(Long.class));
    }

    @Test
    void deveLancarBadRequestQuandoStatusEhControladoAutomaticamente() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 14L;
        Authentication authentication = authenticationComRole("ROLE_ADMIN");

        List<StatusChamado> statusAutomaticos = List.of(
                StatusChamado.ABERTO,
                StatusChamado.ATRIBUIDO,
                StatusChamado.EM_ATENDIMENTO
        );

        for (StatusChamado status : statusAutomaticos) {

            StatusChamadoRequest request = new StatusChamadoRequest();
            request.setStatus(status);

            // Act
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> chamadoService.atualizarStatus(
                            contratoId, chamadoId, request, authentication
                    )
            );

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals(
                    "Este status é controlado automaticamente pelo sistema",
                    exception.getReason()
            );
        }

        verify(chamadoRepository, never())
                .findByIdAndUnidadeContratoId(any(Long.class), any(Long.class));
    }

    @Test
    void deveBloquearAlteracaoDeStatusDeChamadoFinalizadoParaTecnico() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 14L;

        Chamado chamado = new Chamado();
        chamado.setId(chamadoId);
        chamado.setStatus(StatusChamado.FINALIZADO);

        when(chamadoRepository.findByIdAndUnidadeContratoId(chamadoId, contratoId))
                .thenReturn(Optional.of(chamado));

        Authentication authentication = authenticationComRole("ROLE_TECNICO");

        StatusChamadoRequest request = new StatusChamadoRequest();
        request.setStatus(StatusChamado.PENDENTE);

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> chamadoService.atualizarStatus(
                        contratoId, chamadoId, request, authentication
                )
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals(
                "Chamado finalizado não permite alteração de status",
                exception.getReason()
        );

        verify(chamadoRepository, never()).save(any(Chamado.class));
    }

    @Test
    void devePermitirGestorReabrirChamadoFinalizado() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 14L;
        Unidade unidade = criarUnidade(5L, contratoId, "Unidade Central");

        Chamado chamado = new Chamado();
        chamado.setId(chamadoId);
        chamado.setUnidade(unidade);
        chamado.setStatus(StatusChamado.FINALIZADO);
        chamado.setDataFinalizacao(LocalDateTime.of(2026, 1, 1, 12, 0));

        when(chamadoRepository.findByIdAndUnidadeContratoId(chamadoId, contratoId))
                .thenReturn(Optional.of(chamado));

        when(chamadoRepository.save(any(Chamado.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Authentication authentication = authenticationComRole("ROLE_ADMIN");

        StatusChamadoRequest request = new StatusChamadoRequest();
        request.setStatus(StatusChamado.PENDENTE);

        // Act
        ChamadoResponse response = chamadoService.atualizarStatus(
                contratoId, chamadoId, request, authentication
        );

        // Assert
        assertEquals(StatusChamado.PENDENTE, response.getStatus());
        assertNull(response.getDataFinalizacao());

        verify(chamadoRepository).save(any(Chamado.class));

        verify(historicoChamadoService).registrar(
                eq(chamado),
                isNull(),
                eq(TipoEventoChamado.STATUS_ALTERADO),
                anyString()
        );
    }

    @Test
    void deveDefinirDataFinalizacaoQuandoNovoStatusForFinalizado() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 14L;
        Unidade unidade = criarUnidade(5L, contratoId, "Unidade Central");

        Chamado chamado = new Chamado();
        chamado.setId(chamadoId);
        chamado.setUnidade(unidade);
        chamado.setStatus(StatusChamado.AGUARDANDO_ANALISE);

        when(chamadoRepository.findByIdAndUnidadeContratoId(chamadoId, contratoId))
                .thenReturn(Optional.of(chamado));

        when(chamadoRepository.save(any(Chamado.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Authentication authentication = authenticationComRole("ROLE_ADMIN");

        StatusChamadoRequest request = new StatusChamadoRequest();
        request.setStatus(StatusChamado.FINALIZADO);

        // Act
        ChamadoResponse response = chamadoService.atualizarStatus(
                contratoId, chamadoId, request, authentication
        );

        // Assert
        assertEquals(StatusChamado.FINALIZADO, response.getStatus());
        assertNotNull(response.getDataFinalizacao());

        verify(chamadoRepository).save(any(Chamado.class));

        verify(historicoChamadoService).registrar(
                eq(chamado),
                isNull(),
                eq(TipoEventoChamado.STATUS_ALTERADO),
                anyString()
        );
    }

    @Test
    void naoDeveAlterarNadaQuandoNovoStatusIgualAoStatusAtual() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 14L;
        Unidade unidade = criarUnidade(5L, contratoId, "Unidade Central");

        Chamado chamado = new Chamado();
        chamado.setId(chamadoId);
        chamado.setUnidade(unidade);
        chamado.setStatus(StatusChamado.PENDENTE);

        when(chamadoRepository.findByIdAndUnidadeContratoId(chamadoId, contratoId))
                .thenReturn(Optional.of(chamado));

        Authentication authentication = authenticationComRole("ROLE_ADMIN");

        StatusChamadoRequest request = new StatusChamadoRequest();
        request.setStatus(StatusChamado.PENDENTE);

        // Act
        ChamadoResponse response = chamadoService.atualizarStatus(
                contratoId, chamadoId, request, authentication
        );

        // Assert
        assertEquals(StatusChamado.PENDENTE, response.getStatus());

        verify(chamadoRepository, never()).save(any(Chamado.class));
        verifyNoInteractions(historicoChamadoService);
    }

    @Test
    void devePermitirCtoAlterarStatusDeChamadoFinalizado() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 14L;
        Unidade unidade = criarUnidade(5L, contratoId, "Unidade Central");

        Chamado chamado = new Chamado();
        chamado.setId(chamadoId);
        chamado.setUnidade(unidade);
        chamado.setStatus(StatusChamado.FINALIZADO);
        chamado.setDataFinalizacao(LocalDateTime.of(2026, 1, 1, 12, 0));

        when(chamadoRepository.findByIdAndUnidadeContratoId(chamadoId, contratoId))
                .thenReturn(Optional.of(chamado));

        when(chamadoRepository.save(any(Chamado.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Authentication authentication = authenticationComRole("ROLE_CTO");

        StatusChamadoRequest request = new StatusChamadoRequest();
        request.setStatus(StatusChamado.AGUARDANDO_ANALISE);

        // Act
        ChamadoResponse response = chamadoService.atualizarStatus(
                contratoId, chamadoId, request, authentication
        );

        // Assert
        assertEquals(StatusChamado.AGUARDANDO_ANALISE, response.getStatus());
        assertNull(response.getDataFinalizacao());

        verify(chamadoRepository).save(any(Chamado.class));

        verify(historicoChamadoService).registrar(
                eq(chamado),
                isNull(),
                eq(TipoEventoChamado.STATUS_ALTERADO),
                anyString()
        );
    }

    @Test
    void devePermitirGestorCancelarChamadoEmAtendimento() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 14L;
        Unidade unidade = criarUnidade(5L, contratoId, "Unidade Central");

        Chamado chamado = new Chamado();
        chamado.setId(chamadoId);
        chamado.setUnidade(unidade);
        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);

        when(chamadoRepository.findByIdAndUnidadeContratoId(chamadoId, contratoId))
                .thenReturn(Optional.of(chamado));

        when(chamadoRepository.save(any(Chamado.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Authentication authentication = authenticationComRole("ROLE_ADMIN");

        StatusChamadoRequest request = new StatusChamadoRequest();
        request.setStatus(StatusChamado.CANCELADO);

        // Act
        ChamadoResponse response = chamadoService.atualizarStatus(
                contratoId, chamadoId, request, authentication
        );

        // Assert
        assertEquals(StatusChamado.CANCELADO, response.getStatus());

        verify(chamadoRepository).save(any(Chamado.class));

        ArgumentCaptor<String> descricaoCaptor = ArgumentCaptor.forClass(String.class);

        verify(historicoChamadoService).registrar(
                eq(chamado),
                isNull(),
                eq(TipoEventoChamado.STATUS_ALTERADO),
                descricaoCaptor.capture()
        );

        assertEquals(
                "Status alterado de Em atendimento para Cancelado.",
                descricaoCaptor.getValue()
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

    private Solicitante criarSolicitante() {
        Solicitante solicitante = new Solicitante();
        solicitante.setNome("Solicitante Padrão");
        solicitante.setEmail("solicitante@exemplo.com");
        solicitante.setTelefone("11999999999");
        solicitante.setIdentificacao("12345678900");
        return solicitante;
    }

    private Solicitante criarSolicitanteCopiaDe(Solicitante original) {
        Solicitante copia = new Solicitante();
        copia.setNome(original.getNome());
        copia.setEmail(original.getEmail());
        copia.setTelefone(original.getTelefone());
        copia.setIdentificacao(original.getIdentificacao());
        return copia;
    }

    private Chamado criarChamadoValido(Long id, Unidade unidade, StatusChamado status) {
        Chamado chamado = new Chamado();
        chamado.setId(id);
        chamado.setNumeroChamado("CH-" + id);
        chamado.setLinkChamadoOsti("http://osti.exemplo.com/" + id);
        chamado.setUnidade(unidade);
        chamado.setSolicitante(criarSolicitante());
        chamado.setNumeroPatrimonio("PAT-" + id);
        chamado.setTipo(TipoChamado.INCIDENTE);
        chamado.setCategoria(CategoriaChamado.OUTROS);
        chamado.setPrioridade(PrioridadeChamado.MEDIA);
        chamado.setStatus(status);
        chamado.setDescricao("Descrição do chamado " + id);
        return chamado;
    }

    private ChamadoRequest criarRequestValido(Long unidadeId) {
        ChamadoRequest request = new ChamadoRequest();
        request.setNumeroChamado("CH-100");
        request.setLinkChamadoOsti("http://osti.exemplo.com/100");
        request.setUnidadeId(unidadeId);
        request.setSolicitante(criarSolicitante());
        request.setNumeroPatrimonio("PAT-100");
        request.setTipo(TipoChamado.INCIDENTE);
        request.setCategoria(CategoriaChamado.OUTROS);
        request.setPrioridade(PrioridadeChamado.MEDIA);
        request.setDescricao("Descrição do chamado de teste");
        return request;
    }

    private ChamadoRequest criarRequestParaChamado(Chamado chamado) {
        ChamadoRequest request = new ChamadoRequest();
        request.setNumeroChamado(chamado.getNumeroChamado());
        request.setLinkChamadoOsti(chamado.getLinkChamadoOsti());
        request.setUnidadeId(chamado.getUnidade().getId());
        request.setSolicitante(criarSolicitanteCopiaDe(chamado.getSolicitante()));
        request.setNumeroPatrimonio(chamado.getNumeroPatrimonio());
        request.setTipo(chamado.getTipo());
        request.setCategoria(chamado.getCategoria());
        request.setPrioridade(chamado.getPrioridade());
        request.setDescricao(chamado.getDescricao());
        return request;
    }

    private Authentication authenticationComRole(String role) {
        return new TestingAuthenticationToken(
                "usuario-teste",
                null,
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}
