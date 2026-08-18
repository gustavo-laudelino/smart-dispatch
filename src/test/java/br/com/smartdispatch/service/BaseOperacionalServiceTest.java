package br.com.smartdispatch.service;

import br.com.smartdispatch.model.BaseOperacional;
import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.repository.BaseOperacionalRepository;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseOperacionalServiceTest {

    @Mock
    private BaseOperacionalRepository baseRepository;

    @Mock
    private ContratoService contratoService;

    @InjectMocks
    private BaseOperacionalService baseOperacionalService;

    @Test
    void deveCriarBaseAssociadaAoContrato() {

        // Arrange
        Long contratoId = 1L;
        Contrato contrato = criarContrato(contratoId, "Cidade Teste");

        BaseOperacional base = criarBase(
                null, null, "Base Central", "Rua A", "12345678", "Centro", "Cidade Teste", -22.9, -47.0
        );

        when(contratoService.buscarPorId(contratoId))
                .thenReturn(contrato);

        when(baseRepository.save(base))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BaseOperacional resultado = baseOperacionalService.criar(contratoId, base);

        // Assert
        assertSame(base, resultado);
        assertSame(contrato, resultado.getContrato());

        verify(contratoService).buscarPorId(contratoId);
        verify(baseRepository).save(base);
    }

    @Test
    void deveListarBasesDoContrato() {

        // Arrange
        Long contratoId = 1L;
        Contrato contrato = criarContrato(contratoId, "Cidade Teste");

        BaseOperacional base1 = criarBase(
                1L, contrato, "Base 1", "Rua A", "12345678", "Centro", "Cidade Teste", -22.9, -47.0
        );
        BaseOperacional base2 = criarBase(
                2L, contrato, "Base 2", "Rua B", "87654321", "Bairro B", "Cidade Teste", -22.8, -47.1
        );

        when(contratoService.buscarPorId(contratoId))
                .thenReturn(contrato);

        when(baseRepository.findByContratoId(contratoId))
                .thenReturn(List.of(base1, base2));

        // Act
        List<BaseOperacional> resultado = baseOperacionalService.listarPorContrato(contratoId);

        // Assert
        assertEquals(2, resultado.size());
        assertEquals(List.of(base1, base2), resultado);

        verify(contratoService).buscarPorId(contratoId);
        verify(baseRepository).findByContratoId(contratoId);
    }

    @Test
    void deveBuscarBasePorIdDentroDoContrato() {

        // Arrange
        Long contratoId = 1L;
        Long baseId = 2L;
        Contrato contrato = criarContrato(contratoId, "Cidade Teste");
        BaseOperacional base = criarBase(
                baseId, contrato, "Base Central", "Rua A", "12345678", "Centro", "Cidade Teste", -22.9, -47.0
        );

        when(baseRepository.findByIdAndContratoId(baseId, contratoId))
                .thenReturn(Optional.of(base));

        // Act
        BaseOperacional resultado = baseOperacionalService.buscarPorId(contratoId, baseId);

        // Assert
        assertSame(base, resultado);

        verify(baseRepository).findByIdAndContratoId(baseId, contratoId);
        verifyNoInteractions(contratoService);
    }

    @Test
    void deveLancarNotFoundQuandoBaseNaoPertencerAoContrato() {

        // Arrange
        Long contratoId = 1L;
        Long baseId = 99L;

        when(baseRepository.findByIdAndContratoId(baseId, contratoId))
                .thenReturn(Optional.empty());

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> baseOperacionalService.buscarPorId(contratoId, baseId)
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Base operacional não encontrada neste contrato", exception.getReason());
    }

    @Test
    void deveAtualizarCamposDaBasePreservandoContrato() {

        // Arrange
        Long contratoId = 1L;
        Long baseId = 2L;

        Contrato contratoOriginal = criarContrato(contratoId, "Cidade Original");
        BaseOperacional baseExistente = criarBase(
                baseId, contratoOriginal, "Base Antiga", "Rua Antiga", "11111111", "Bairro Antigo",
                "Cidade Antiga", -22.9, -47.0
        );

        Contrato outroContrato = criarContrato(99L, "Cidade Outra");
        BaseOperacional novosDados = criarBase(
                null, outroContrato, "Base Nova", "Rua Nova", "22222222", "Bairro Novo",
                "Cidade Nova", -23.0, -48.0
        );

        when(baseRepository.findByIdAndContratoId(baseId, contratoId))
                .thenReturn(Optional.of(baseExistente));

        when(baseRepository.save(any(BaseOperacional.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BaseOperacional resultado = baseOperacionalService.atualizar(contratoId, baseId, novosDados);

        // Assert
        assertSame(baseExistente, resultado);
        assertEquals(baseId, resultado.getId());
        assertSame(contratoOriginal, resultado.getContrato());
        assertEquals("Base Nova", resultado.getNome());
        assertEquals("Rua Nova", resultado.getEndereco());
        assertEquals("22222222", resultado.getCep());
        assertEquals("Bairro Novo", resultado.getBairro());
        assertEquals("Cidade Nova", resultado.getCidade());
        assertEquals(-23.0, resultado.getLatitude());
        assertEquals(-48.0, resultado.getLongitude());

        verify(baseRepository).save(baseExistente);
    }

    @Test
    void deveExcluirBaseDoContrato() {

        // Arrange
        Long contratoId = 1L;
        Long baseId = 2L;
        Contrato contrato = criarContrato(contratoId, "Cidade Teste");
        BaseOperacional base = criarBase(
                baseId, contrato, "Base Central", "Rua A", "12345678", "Centro", "Cidade Teste", -22.9, -47.0
        );

        when(baseRepository.findByIdAndContratoId(baseId, contratoId))
                .thenReturn(Optional.of(base));

        // Act
        baseOperacionalService.excluir(contratoId, baseId);

        // Assert
        verify(baseRepository).findByIdAndContratoId(baseId, contratoId);
        verify(baseRepository).delete(base);
    }

    // ---------------------------------------------------------------
    // Fixtures auxiliares
    // ---------------------------------------------------------------

    private Contrato criarContrato(Long id, String cidade) {
        Contrato contrato = new Contrato();
        contrato.setId(id);
        contrato.setCidade(cidade);
        return contrato;
    }

    private BaseOperacional criarBase(
            Long id,
            Contrato contrato,
            String nome,
            String endereco,
            String cep,
            String bairro,
            String cidade,
            Double latitude,
            Double longitude
    ) {
        BaseOperacional base = new BaseOperacional();
        base.setId(id);
        base.setContrato(contrato);
        base.setNome(nome);
        base.setEndereco(endereco);
        base.setCep(cep);
        base.setBairro(bairro);
        base.setCidade(cidade);
        base.setLatitude(latitude);
        base.setLongitude(longitude);
        return base;
    }
}
