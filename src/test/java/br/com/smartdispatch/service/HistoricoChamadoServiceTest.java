package br.com.smartdispatch.service;

import br.com.smartdispatch.dto.HistoricoChamadoResponse;
import br.com.smartdispatch.enums.TipoEventoChamado;
import br.com.smartdispatch.model.Chamado;
import br.com.smartdispatch.model.HistoricoChamado;
import br.com.smartdispatch.model.OrdemServico;
import br.com.smartdispatch.repository.ChamadoRepository;
import br.com.smartdispatch.repository.HistoricoChamadoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricoChamadoServiceTest {

    @Mock
    private HistoricoChamadoRepository historicoRepository;

    @Mock
    private ChamadoRepository chamadoRepository;

    @InjectMocks
    private HistoricoChamadoService historicoChamadoService;

    @Test
    void deveExigirChamadoAoRegistrarHistorico() {

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> historicoChamadoService.registrar(
                        null, null, TipoEventoChamado.STATUS_ALTERADO, "Descrição válida"
                )
        );

        // Assert
        assertEquals("O chamado deve ser informado no histórico", exception.getMessage());

        verifyNoInteractions(historicoRepository);
    }

    @Test
    void deveExigirTipoEventoAoRegistrarHistorico() {

        // Arrange
        Chamado chamado = criarChamado(1L);

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> historicoChamadoService.registrar(chamado, null, null, "Descrição válida")
        );

        // Assert
        assertEquals("O tipo do evento deve ser informado", exception.getMessage());

        verifyNoInteractions(historicoRepository);
    }

    @Test
    void deveExigirDescricaoAoRegistrarHistorico() {

        // Arrange
        Chamado chamado = criarChamado(1L);

        // Act
        IllegalArgumentException exceptionNula = assertThrows(
                IllegalArgumentException.class,
                () -> historicoChamadoService.registrar(
                        chamado, null, TipoEventoChamado.STATUS_ALTERADO, null
                )
        );

        IllegalArgumentException exceptionEmBranco = assertThrows(
                IllegalArgumentException.class,
                () -> historicoChamadoService.registrar(
                        chamado, null, TipoEventoChamado.STATUS_ALTERADO, "   "
                )
        );

        // Assert
        assertEquals("A descrição do evento deve ser informada", exceptionNula.getMessage());
        assertEquals("A descrição do evento deve ser informada", exceptionEmBranco.getMessage());

        verifyNoInteractions(historicoRepository);
    }

    @Test
    void deveRegistrarHistoricoSemOrdemServico() {

        // Arrange
        Chamado chamado = criarChamado(1L);

        // Act
        historicoChamadoService.registrar(
                chamado, null, TipoEventoChamado.STATUS_ALTERADO, "  Status alterado  "
        );

        // Assert
        ArgumentCaptor<HistoricoChamado> captor = ArgumentCaptor.forClass(HistoricoChamado.class);
        verify(historicoRepository).save(captor.capture());

        HistoricoChamado salvo = captor.getValue();
        assertSame(chamado, salvo.getChamado());
        assertNull(salvo.getOrdemServico());
        assertEquals(TipoEventoChamado.STATUS_ALTERADO, salvo.getTipoEvento());
        assertEquals("Status alterado", salvo.getDescricao());
    }

    @Test
    void deveRegistrarHistoricoVinculadoAOrdemServico() {

        // Arrange
        Chamado chamado = criarChamado(1L);
        OrdemServico ordemServico = criarOrdemServico(2L, "OS-100");

        // Act
        historicoChamadoService.registrar(
                chamado, ordemServico, TipoEventoChamado.ORDEM_SERVICO_CRIADA, "OS criada"
        );

        // Assert
        ArgumentCaptor<HistoricoChamado> captor = ArgumentCaptor.forClass(HistoricoChamado.class);
        verify(historicoRepository).save(captor.capture());

        HistoricoChamado salvo = captor.getValue();
        assertSame(chamado, salvo.getChamado());
        assertSame(ordemServico, salvo.getOrdemServico());
        assertEquals(TipoEventoChamado.ORDEM_SERVICO_CRIADA, salvo.getTipoEvento());
        assertEquals("OS criada", salvo.getDescricao());
    }

    @Test
    void deveLancarNotFoundAoListarHistoricoDeChamadoForaDoContrato() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;

        when(chamadoRepository.findByIdAndUnidadeContratoId(chamadoId, contratoId))
                .thenReturn(Optional.empty());

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> historicoChamadoService.listarPorChamado(contratoId, chamadoId)
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Chamado não encontrado neste contrato", exception.getReason());

        verifyNoInteractions(historicoRepository);
    }

    @Test
    void deveListarHistoricoDoChamadoComMapeamentoCompleto() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;

        Chamado chamado = criarChamado(chamadoId);
        OrdemServico ordemServico = criarOrdemServico(20L, "OS-300");

        HistoricoChamado historico1 = new HistoricoChamado();
        historico1.setId(1L);
        historico1.setChamado(chamado);
        historico1.setOrdemServico(null);
        historico1.setTipoEvento(TipoEventoChamado.CHAMADO_CRIADO);
        historico1.setDescricao("Chamado criado");
        historico1.setDataEvento(LocalDateTime.of(2026, 1, 1, 8, 0));

        HistoricoChamado historico2 = new HistoricoChamado();
        historico2.setId(2L);
        historico2.setChamado(chamado);
        historico2.setOrdemServico(ordemServico);
        historico2.setTipoEvento(TipoEventoChamado.ORDEM_SERVICO_CRIADA);
        historico2.setDescricao("OS criada");
        historico2.setDataEvento(LocalDateTime.of(2026, 1, 1, 9, 0));

        when(chamadoRepository.findByIdAndUnidadeContratoId(chamadoId, contratoId))
                .thenReturn(Optional.of(chamado));

        when(
                historicoRepository.findByChamadoIdAndChamadoUnidadeContratoIdOrderByDataEventoAsc(
                        chamadoId, contratoId
                )
        ).thenReturn(List.of(historico1, historico2));

        // Act
        List<HistoricoChamadoResponse> resultado =
                historicoChamadoService.listarPorChamado(contratoId, chamadoId);

        // Assert
        assertEquals(2, resultado.size());

        HistoricoChamadoResponse response1 = resultado.get(0);
        assertEquals(1L, response1.getId());
        assertEquals(chamadoId, response1.getChamadoId());
        assertNull(response1.getOrdemServicoId());
        assertNull(response1.getNumeroOrdemServico());
        assertEquals(TipoEventoChamado.CHAMADO_CRIADO, response1.getTipoEvento());
        assertEquals("Chamado criado", response1.getDescricao());
        assertEquals(historico1.getDataEvento(), response1.getDataEvento());

        HistoricoChamadoResponse response2 = resultado.get(1);
        assertEquals(2L, response2.getId());
        assertEquals(20L, response2.getOrdemServicoId());
        assertEquals("OS-300", response2.getNumeroOrdemServico());
        assertEquals(TipoEventoChamado.ORDEM_SERVICO_CRIADA, response2.getTipoEvento());
        assertEquals("OS criada", response2.getDescricao());
        assertEquals(historico2.getDataEvento(), response2.getDataEvento());

        verify(historicoRepository)
                .findByChamadoIdAndChamadoUnidadeContratoIdOrderByDataEventoAsc(chamadoId, contratoId);
    }

    // ---------------------------------------------------------------
    // Fixtures auxiliares
    // ---------------------------------------------------------------

    private Chamado criarChamado(Long id) {
        Chamado chamado = new Chamado();
        chamado.setId(id);
        return chamado;
    }

    private OrdemServico criarOrdemServico(Long id, String numeroOrdemServico) {
        OrdemServico ordemServico = new OrdemServico();
        ordemServico.setId(id);
        ordemServico.setNumeroOrdemServico(numeroOrdemServico);
        return ordemServico;
    }
}
