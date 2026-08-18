package br.com.smartdispatch.service;

import br.com.smartdispatch.dto.SugestaoTecnicoResponse;
import br.com.smartdispatch.enums.NivelIndicacao;
import br.com.smartdispatch.model.BaseOperacional;
import br.com.smartdispatch.model.OrdemServico;
import br.com.smartdispatch.model.Tecnico;
import br.com.smartdispatch.model.Unidade;
import br.com.smartdispatch.model.Usuario;
import br.com.smartdispatch.repository.OrdemServicoRepository;
import br.com.smartdispatch.repository.TecnicoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SugestaoTecnicoServiceTest {

    @Mock
    private TecnicoRepository tecnicoRepository;

    @Mock
    private OrdemServicoRepository ordemServicoRepository;

    @Mock
    private DistanciaService distanciaService;

    @InjectMocks
    private SugestaoTecnicoService sugestaoTecnicoService;

    @Test
    void deveLancarNotFoundQuandoOrdemServicoAlvoNaoExistir() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 100L;

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.empty());

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> sugestaoTecnicoService.listarSugestoes(contratoId, chamadoId, ordemServicoId)
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Ordem de serviço não encontrada.", exception.getReason());

        verifyNoInteractions(tecnicoRepository);
        verifyNoInteractions(distanciaService);
    }

    @Test
    void deveLancarBadRequestQuandoUnidadeDestinoNaoPossuirCoordenadas() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 100L;

        Unidade unidadeSemLatitude = criarUnidade(5L, null, -47.0);
        OrdemServico ordemServicoAlvoA = criarOrdemServico(ordemServicoId, unidadeSemLatitude);

        Unidade unidadeSemLongitude = criarUnidade(6L, -22.0, null);
        OrdemServico ordemServicoAlvoB = criarOrdemServico(ordemServicoId, unidadeSemLongitude);

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.of(ordemServicoAlvoA), Optional.of(ordemServicoAlvoB));

        // Act
        ResponseStatusException excecaoSemLatitude = assertThrows(
                ResponseStatusException.class,
                () -> sugestaoTecnicoService.listarSugestoes(contratoId, chamadoId, ordemServicoId)
        );

        ResponseStatusException excecaoSemLongitude = assertThrows(
                ResponseStatusException.class,
                () -> sugestaoTecnicoService.listarSugestoes(contratoId, chamadoId, ordemServicoId)
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, excecaoSemLatitude.getStatusCode());
        assertEquals(
                "A unidade de atendimento não possui coordenadas.",
                excecaoSemLatitude.getReason()
        );

        assertEquals(HttpStatus.BAD_REQUEST, excecaoSemLongitude.getStatusCode());
        assertEquals(
                "A unidade de atendimento não possui coordenadas.",
                excecaoSemLongitude.getReason()
        );

        verifyNoInteractions(tecnicoRepository);
        verifyNoInteractions(distanciaService);
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHouverTecnicosAtivosNoContrato() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 100L;

        Unidade unidadeDestino = criarUnidade(5L, -22.0, -47.0);
        OrdemServico ordemServicoAlvo = criarOrdemServico(ordemServicoId, unidadeDestino);

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.of(ordemServicoAlvo));

        when(tecnicoRepository.findByBaseOperacionalContratoIdAndAtivoTrue(contratoId))
                .thenReturn(List.of());

        // Act
        List<SugestaoTecnicoResponse> resultado =
                sugestaoTecnicoService.listarSugestoes(contratoId, chamadoId, ordemServicoId);

        // Assert
        assertTrue(resultado.isEmpty());

        verify(tecnicoRepository).findByBaseOperacionalContratoIdAndAtivoTrue(contratoId);
        verifyNoInteractions(distanciaService);
    }

    @Test
    void deveCalcularCandidatoSemOsAtivaUsandoBaseEJanelasCorretas() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 100L;
        Long tecnicoId = 20L;

        Unidade unidadeDestino = criarUnidade(5L, -22.0, -47.0);
        OrdemServico ordemServicoAlvo = criarOrdemServico(ordemServicoId, unidadeDestino);

        BaseOperacional base = criarBase(6L, -23.0, -46.0);
        Usuario usuario = criarUsuario(30L, "Técnico Base");
        Tecnico tecnico = criarTecnico(tecnicoId, usuario, base);

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.of(ordemServicoAlvo));

        when(tecnicoRepository.findByBaseOperacionalContratoIdAndAtivoTrue(contratoId))
                .thenReturn(List.of(tecnico));

        when(
                ordemServicoRepository.findByTecnicoIdAndChamadoUnidadeContratoIdAndDataCheckOutIsNull(
                        tecnicoId, contratoId
                )
        ).thenReturn(List.of());

        when(distanciaService.calcularEmKm(-23.0, -46.0, -22.0, -47.0))
                .thenReturn(10.0);

        when(
                ordemServicoRepository
                        .countByTecnicoIdAndChamadoUnidadeContratoIdAndIdNotAndDataAtribuicaoTecnicoGreaterThanEqualAndDataAtribuicaoTecnicoLessThan(
                                eq(tecnicoId), eq(contratoId), eq(ordemServicoId),
                                any(LocalDateTime.class), any(LocalDateTime.class)
                        )
        ).thenReturn(2L);

        when(
                ordemServicoRepository
                        .countByTecnicoIdAndChamadoUnidadeContratoIdAndDataCheckOutGreaterThanEqual(
                                eq(tecnicoId), eq(contratoId), any(LocalDateTime.class)
                        )
        ).thenReturn(3L);

        LocalDateTime antes = LocalDateTime.now();
        LocalDate dataAntes = LocalDate.now();

        // Act
        List<SugestaoTecnicoResponse> resultado =
                sugestaoTecnicoService.listarSugestoes(contratoId, chamadoId, ordemServicoId);

        LocalDateTime depois = LocalDateTime.now();
        LocalDate dataDepois = LocalDate.now();

        // Assert
        assertEquals(1, resultado.size());

        SugestaoTecnicoResponse response = resultado.get(0);
        assertEquals(tecnicoId, response.getTecnicoId());
        assertEquals("Técnico Base", response.getTecnicoNome());
        assertEquals(10.0, response.getDistanciaKm(), 0.001);
        assertEquals(0, response.getQuantidadeOsAtivas());
        assertEquals(2, response.getAtribuicoesHoje());
        assertEquals(3, response.getAtendimentosUltimos15Dias());
        assertEquals(46.0, response.getPontuacao(), 0.001);
        assertEquals(NivelIndicacao.ALTA, response.getNivelIndicacao());
        assertEquals(3, response.getEstrelas());

        // Janela de "hoje"
        ArgumentCaptor<LocalDateTime> inicioHojeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> inicioAmanhaCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(ordemServicoRepository)
                .countByTecnicoIdAndChamadoUnidadeContratoIdAndIdNotAndDataAtribuicaoTecnicoGreaterThanEqualAndDataAtribuicaoTecnicoLessThan(
                        eq(tecnicoId), eq(contratoId), eq(ordemServicoId),
                        inicioHojeCaptor.capture(), inicioAmanhaCaptor.capture()
                );

        LocalDateTime inicioHoje = inicioHojeCaptor.getValue();
        LocalDateTime inicioAmanha = inicioAmanhaCaptor.getValue();

        assertEquals(LocalTime.MIDNIGHT, inicioHoje.toLocalTime());
        assertEquals(inicioHoje.plusDays(1), inicioAmanha);
        assertFalse(inicioHoje.toLocalDate().isBefore(dataAntes));
        assertFalse(inicioHoje.toLocalDate().isAfter(dataDepois));

        // Janela dos "últimos 15 dias"
        ArgumentCaptor<LocalDateTime> inicioPeriodoCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(ordemServicoRepository)
                .countByTecnicoIdAndChamadoUnidadeContratoIdAndDataCheckOutGreaterThanEqual(
                        eq(tecnicoId), eq(contratoId), inicioPeriodoCaptor.capture()
                );

        LocalDateTime inicioPeriodo = inicioPeriodoCaptor.getValue();

        assertFalse(inicioPeriodo.isBefore(antes.minusDays(15)));
        assertFalse(inicioPeriodo.isAfter(depois.minusDays(15)));
    }

    @Test
    void deveUsarOsAtivaMaisProximaComoAncoraEIgnorarOrdemAlvoNaCarga() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 100L;
        Long tecnicoId = 20L;

        Unidade unidadeDestino = criarUnidade(5L, 0.0, 0.0);
        OrdemServico ordemServicoAlvo = criarOrdemServico(ordemServicoId, unidadeDestino);

        BaseOperacional base = criarBase(6L, -50.0, -50.0);
        Tecnico tecnico = criarTecnico(tecnicoId, criarUsuario(30L, "Técnico Ativo"), base);

        Unidade unidadeA = criarUnidade(7L, 1.0, 1.0);
        Unidade unidadeB = criarUnidade(8L, 2.0, 2.0);

        OrdemServico osA = criarOrdemServico(101L, unidadeA);
        OrdemServico osB = criarOrdemServico(102L, unidadeB);

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.of(ordemServicoAlvo));

        when(tecnicoRepository.findByBaseOperacionalContratoIdAndAtivoTrue(contratoId))
                .thenReturn(List.of(tecnico));

        when(
                ordemServicoRepository.findByTecnicoIdAndChamadoUnidadeContratoIdAndDataCheckOutIsNull(
                        tecnicoId, contratoId
                )
        ).thenReturn(List.of(ordemServicoAlvo, osA, osB));

        when(distanciaService.calcularEmKm(1.0, 1.0, 0.0, 0.0))
                .thenReturn(5.678);

        when(distanciaService.calcularEmKm(2.0, 2.0, 0.0, 0.0))
                .thenReturn(2.345);

        when(
                ordemServicoRepository
                        .countByTecnicoIdAndChamadoUnidadeContratoIdAndIdNotAndDataAtribuicaoTecnicoGreaterThanEqualAndDataAtribuicaoTecnicoLessThan(
                                eq(tecnicoId), eq(contratoId), eq(ordemServicoId),
                                any(LocalDateTime.class), any(LocalDateTime.class)
                        )
        ).thenReturn(2L);

        when(
                ordemServicoRepository
                        .countByTecnicoIdAndChamadoUnidadeContratoIdAndDataCheckOutGreaterThanEqual(
                                eq(tecnicoId), eq(contratoId), any(LocalDateTime.class)
                        )
        ).thenReturn(3L);

        // Act
        List<SugestaoTecnicoResponse> resultado =
                sugestaoTecnicoService.listarSugestoes(contratoId, chamadoId, ordemServicoId);

        // Assert
        assertEquals(1, resultado.size());

        SugestaoTecnicoResponse response = resultado.get(0);
        assertEquals(2, response.getQuantidadeOsAtivas());
        assertEquals(2, response.getAtribuicoesHoje());
        assertEquals(3, response.getAtendimentosUltimos15Dias());
        assertEquals(19.38, response.getPontuacao(), 0.001);
        assertEquals(2.35, response.getDistanciaKm(), 0.001);
        assertEquals(NivelIndicacao.ALTA, response.getNivelIndicacao());
        assertEquals(3, response.getEstrelas());

        verify(distanciaService).calcularEmKm(1.0, 1.0, 0.0, 0.0);
        verify(distanciaService).calcularEmKm(2.0, 2.0, 0.0, 0.0);
        verify(distanciaService, never()).calcularEmKm(-50.0, -50.0, 0.0, 0.0);
    }

    @Test
    void deveOrdenarPorMenorPontuacaoEDefinirNivelPelaDiferencaDoMelhor() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 100L;

        Unidade unidadeDestino = criarUnidade(5L, 0.0, 0.0);
        OrdemServico ordemServicoAlvo = criarOrdemServico(ordemServicoId, unidadeDestino);

        BaseOperacional baseA = criarBase(11L, 1.0, 1.0);
        BaseOperacional baseB = criarBase(12L, 2.0, 2.0);
        BaseOperacional baseC = criarBase(13L, 3.0, 3.0);
        BaseOperacional baseD = criarBase(14L, 4.0, 4.0);

        Tecnico tecnicoA = criarTecnico(20L, criarUsuario(30L, "Técnico A"), baseA);
        Tecnico tecnicoB = criarTecnico(21L, criarUsuario(31L, "Técnico B"), baseB);
        Tecnico tecnicoC = criarTecnico(22L, criarUsuario(32L, "Técnico C"), baseC);
        Tecnico tecnicoD = criarTecnico(23L, criarUsuario(33L, "Técnico D"), baseD);

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.of(ordemServicoAlvo));

        when(tecnicoRepository.findByBaseOperacionalContratoIdAndAtivoTrue(contratoId))
                .thenReturn(List.of(tecnicoA, tecnicoB, tecnicoC, tecnicoD));

        when(
                ordemServicoRepository.findByTecnicoIdAndChamadoUnidadeContratoIdAndDataCheckOutIsNull(
                        any(Long.class), eq(contratoId)
                )
        ).thenReturn(List.of());

        when(distanciaService.calcularEmKm(1.0, 1.0, 0.0, 0.0)).thenReturn(5.00);
        when(distanciaService.calcularEmKm(2.0, 2.0, 0.0, 0.0)).thenReturn(6.25);
        when(distanciaService.calcularEmKm(3.0, 3.0, 0.0, 0.0)).thenReturn(8.75);
        when(distanciaService.calcularEmKm(4.0, 4.0, 0.0, 0.0)).thenReturn(9.00);

        when(
                ordemServicoRepository
                        .countByTecnicoIdAndChamadoUnidadeContratoIdAndIdNotAndDataAtribuicaoTecnicoGreaterThanEqualAndDataAtribuicaoTecnicoLessThan(
                                any(Long.class), eq(contratoId), eq(ordemServicoId),
                                any(LocalDateTime.class), any(LocalDateTime.class)
                        )
        ).thenReturn(0L);

        when(
                ordemServicoRepository
                        .countByTecnicoIdAndChamadoUnidadeContratoIdAndDataCheckOutGreaterThanEqual(
                                any(Long.class), eq(contratoId), any(LocalDateTime.class)
                        )
        ).thenReturn(0L);

        // Act
        List<SugestaoTecnicoResponse> resultado =
                sugestaoTecnicoService.listarSugestoes(contratoId, chamadoId, ordemServicoId);

        // Assert
        assertEquals(4, resultado.size());

        assertEquals(
                List.of(20L, 21L, 22L, 23L),
                resultado.stream().map(SugestaoTecnicoResponse::getTecnicoId).toList()
        );

        assertEquals(20.0, resultado.get(0).getPontuacao(), 0.001);
        assertEquals(25.0, resultado.get(1).getPontuacao(), 0.001);
        assertEquals(35.0, resultado.get(2).getPontuacao(), 0.001);
        assertEquals(36.0, resultado.get(3).getPontuacao(), 0.001);

        assertEquals(NivelIndicacao.ALTA, resultado.get(0).getNivelIndicacao());
        assertEquals(NivelIndicacao.ALTA, resultado.get(1).getNivelIndicacao());
        assertEquals(NivelIndicacao.MODERADA, resultado.get(2).getNivelIndicacao());
        assertEquals(NivelIndicacao.LEVE, resultado.get(3).getNivelIndicacao());

        assertEquals(3, resultado.get(0).getEstrelas());
        assertEquals(3, resultado.get(1).getEstrelas());
        assertEquals(2, resultado.get(2).getEstrelas());
        assertEquals(1, resultado.get(3).getEstrelas());
    }

    @Test
    void deveArredondarPontuacaoEDistanciaParaDuasCasas() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long ordemServicoId = 100L;
        Long tecnicoId = 20L;

        Unidade unidadeDestino = criarUnidade(5L, 0.0, 0.0);
        OrdemServico ordemServicoAlvo = criarOrdemServico(ordemServicoId, unidadeDestino);

        BaseOperacional base = criarBase(6L, 1.0, 1.0);
        Tecnico tecnico = criarTecnico(tecnicoId, criarUsuario(30L, "Técnico Único"), base);

        when(
                ordemServicoRepository.findByIdAndChamadoIdAndChamadoUnidadeContratoId(
                        ordemServicoId, chamadoId, contratoId
                )
        ).thenReturn(Optional.of(ordemServicoAlvo));

        when(tecnicoRepository.findByBaseOperacionalContratoIdAndAtivoTrue(contratoId))
                .thenReturn(List.of(tecnico));

        when(
                ordemServicoRepository.findByTecnicoIdAndChamadoUnidadeContratoIdAndDataCheckOutIsNull(
                        tecnicoId, contratoId
                )
        ).thenReturn(List.of());

        when(distanciaService.calcularEmKm(1.0, 1.0, 0.0, 0.0))
                .thenReturn(1.236);

        when(
                ordemServicoRepository
                        .countByTecnicoIdAndChamadoUnidadeContratoIdAndIdNotAndDataAtribuicaoTecnicoGreaterThanEqualAndDataAtribuicaoTecnicoLessThan(
                                eq(tecnicoId), eq(contratoId), eq(ordemServicoId),
                                any(LocalDateTime.class), any(LocalDateTime.class)
                        )
        ).thenReturn(0L);

        when(
                ordemServicoRepository
                        .countByTecnicoIdAndChamadoUnidadeContratoIdAndDataCheckOutGreaterThanEqual(
                                eq(tecnicoId), eq(contratoId), any(LocalDateTime.class)
                        )
        ).thenReturn(0L);

        // Act
        List<SugestaoTecnicoResponse> resultado =
                sugestaoTecnicoService.listarSugestoes(contratoId, chamadoId, ordemServicoId);

        // Assert
        assertEquals(1, resultado.size());

        SugestaoTecnicoResponse response = resultado.get(0);
        assertEquals(1.24, response.getDistanciaKm(), 0.001);
        assertEquals(4.94, response.getPontuacao(), 0.001);
        assertEquals(NivelIndicacao.ALTA, response.getNivelIndicacao());
    }

    // ---------------------------------------------------------------
    // Fixtures auxiliares
    // ---------------------------------------------------------------

    private Unidade criarUnidade(Long id, Double latitude, Double longitude) {
        Unidade unidade = new Unidade();
        unidade.setId(id);
        unidade.setLatitude(latitude);
        unidade.setLongitude(longitude);
        return unidade;
    }

    private BaseOperacional criarBase(Long id, Double latitude, Double longitude) {
        BaseOperacional base = new BaseOperacional();
        base.setId(id);
        base.setLatitude(latitude);
        base.setLongitude(longitude);
        return base;
    }

    private Usuario criarUsuario(Long id, String nome) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        return usuario;
    }

    private Tecnico criarTecnico(Long id, Usuario usuario, BaseOperacional base) {
        Tecnico tecnico = new Tecnico();
        tecnico.setId(id);
        tecnico.setUsuario(usuario);
        tecnico.setBaseOperacional(base);
        tecnico.setAtivo(true);
        return tecnico;
    }

    private OrdemServico criarOrdemServico(Long id, Unidade unidadeAtendimento) {
        OrdemServico ordemServico = new OrdemServico();
        ordemServico.setId(id);
        ordemServico.setUnidadeAtendimento(unidadeAtendimento);
        return ordemServico;
    }
}
