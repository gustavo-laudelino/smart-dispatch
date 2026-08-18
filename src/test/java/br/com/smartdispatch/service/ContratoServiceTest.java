package br.com.smartdispatch.service;

import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.repository.ContratoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContratoServiceTest {

    @Mock
    private ContratoRepository contratoRepository;

    @InjectMocks
    private ContratoService contratoService;

    @Test
    void deveCriarContrato() {

        // Arrange
        Contrato contrato = criarContrato(
                null, "Cidade Teste", "Secretário Teste", 24, "http://portal.exemplo.com"
        );

        Contrato contratoSalvo = criarContrato(
                1L, "Cidade Teste", "Secretário Teste", 24, "http://portal.exemplo.com"
        );

        when(contratoRepository.save(contrato))
                .thenReturn(contratoSalvo);

        // Act
        Contrato resultado = contratoService.criar(contrato);

        // Assert
        assertSame(contratoSalvo, resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Cidade Teste", resultado.getCidade());
        assertEquals("Secretário Teste", resultado.getSecretarioResponsavel());
        assertEquals(24, resultado.getSlaHoras());
        assertEquals("http://portal.exemplo.com", resultado.getLinkPortalChamados());

        verify(contratoRepository).save(contrato);
    }

    @Test
    void deveListarContratos() {

        // Arrange
        Contrato contrato1 = criarContrato(1L, "Cidade A", "Secretário A", 24, "http://a.exemplo.com");
        Contrato contrato2 = criarContrato(2L, "Cidade B", "Secretário B", 48, "http://b.exemplo.com");

        when(contratoRepository.findAll())
                .thenReturn(List.of(contrato1, contrato2));

        // Act
        List<Contrato> resultado = contratoService.listar();

        // Assert
        assertEquals(2, resultado.size());
        assertEquals(List.of(contrato1, contrato2), resultado);

        verify(contratoRepository).findAll();
    }

    @Test
    void deveBuscarContratoPorIdComSucesso() {

        // Arrange
        Long id = 1L;
        Contrato contrato = criarContrato(id, "Cidade Teste", "Secretário Teste", 24, "http://portal.exemplo.com");

        when(contratoRepository.findById(id))
                .thenReturn(Optional.of(contrato));

        // Act
        Contrato resultado = contratoService.buscarPorId(id);

        // Assert
        assertSame(contrato, resultado);

        verify(contratoRepository).findById(id);
    }

    @Test
    void deveLancarNotFoundQuandoContratoNaoExistir() {

        // Arrange
        Long id = 99L;

        when(contratoRepository.findById(id))
                .thenReturn(Optional.empty());

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> contratoService.buscarPorId(id)
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Contrato não encontrado", exception.getReason());
    }

    @Test
    void deveAtualizarCamposDoContratoExistente() {

        // Arrange
        Long id = 1L;
        Contrato contratoExistente =
                criarContrato(id, "Cidade Antiga", "Secretário Antigo", 12, "http://antigo.exemplo.com");

        Contrato novosDados =
                criarContrato(null, "Cidade Nova", "Secretário Novo", 48, "http://novo.exemplo.com");

        when(contratoRepository.findById(id))
                .thenReturn(Optional.of(contratoExistente));

        when(contratoRepository.save(any(Contrato.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Contrato resultado = contratoService.atualizar(id, novosDados);

        // Assert
        assertSame(contratoExistente, resultado);
        assertEquals(id, resultado.getId());
        assertEquals("Cidade Nova", resultado.getCidade());
        assertEquals("Secretário Novo", resultado.getSecretarioResponsavel());
        assertEquals(48, resultado.getSlaHoras());
        assertEquals("http://novo.exemplo.com", resultado.getLinkPortalChamados());

        verify(contratoRepository).save(contratoExistente);
    }

    @Test
    void deveExcluirContratoExistente() {

        // Arrange
        Long id = 1L;
        Contrato contrato = criarContrato(id, "Cidade Teste", "Secretário Teste", 24, "http://portal.exemplo.com");

        when(contratoRepository.findById(id))
                .thenReturn(Optional.of(contrato));

        // Act
        contratoService.excluir(id);

        // Assert
        verify(contratoRepository).findById(id);
        verify(contratoRepository).delete(contrato);
    }

    // ---------------------------------------------------------------
    // Fixtures auxiliares
    // ---------------------------------------------------------------

    private Contrato criarContrato(
            Long id,
            String cidade,
            String secretarioResponsavel,
            Integer slaHoras,
            String linkPortalChamados
    ) {
        Contrato contrato = new Contrato();
        contrato.setId(id);
        contrato.setCidade(cidade);
        contrato.setSecretarioResponsavel(secretarioResponsavel);
        contrato.setSlaHoras(slaHoras);
        contrato.setLinkPortalChamados(linkPortalChamados);
        return contrato;
    }
}
