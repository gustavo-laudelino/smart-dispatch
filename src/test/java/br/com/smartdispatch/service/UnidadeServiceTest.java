package br.com.smartdispatch.service;

import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.model.Unidade;
import br.com.smartdispatch.repository.UnidadeRepository;
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
class UnidadeServiceTest {

    @Mock
    private UnidadeRepository unidadeRepository;

    @Mock
    private ContratoService contratoService;

    @InjectMocks
    private UnidadeService unidadeService;

    @Test
    void deveCriarUnidadeAssociadaAoContrato() {

        // Arrange
        Long contratoId = 1L;
        Contrato contrato = criarContrato(contratoId);

        Unidade unidade = criarUnidade(
                null, null, "Unidade Central", "Rua A", "12345678", "Centro", "Cidade Teste", -22.9, -47.0
        );

        when(contratoService.buscarPorId(contratoId))
                .thenReturn(contrato);

        when(unidadeRepository.save(unidade))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Unidade resultado = unidadeService.criar(contratoId, unidade);

        // Assert
        assertSame(unidade, resultado);
        assertSame(contrato, resultado.getContrato());

        verify(contratoService).buscarPorId(contratoId);
        verify(unidadeRepository).save(unidade);
    }

    @Test
    void deveListarUnidadesDoContrato() {

        // Arrange
        Long contratoId = 1L;
        Contrato contrato = criarContrato(contratoId);

        Unidade unidade1 = criarUnidade(
                1L, contrato, "Unidade 1", "Rua A", "12345678", "Centro", "Cidade Teste", -22.9, -47.0
        );
        Unidade unidade2 = criarUnidade(
                2L, contrato, "Unidade 2", "Rua B", "87654321", "Bairro B", "Cidade Teste", -22.8, -47.1
        );

        when(contratoService.buscarPorId(contratoId))
                .thenReturn(contrato);

        when(unidadeRepository.findByContratoId(contratoId))
                .thenReturn(List.of(unidade1, unidade2));

        // Act
        List<Unidade> resultado = unidadeService.listarPorContrato(contratoId);

        // Assert
        assertEquals(2, resultado.size());
        assertEquals(List.of(unidade1, unidade2), resultado);

        verify(contratoService).buscarPorId(contratoId);
        verify(unidadeRepository).findByContratoId(contratoId);
    }

    @Test
    void deveBuscarUnidadePorIdDentroDoContrato() {

        // Arrange
        Long contratoId = 1L;
        Long unidadeId = 2L;
        Contrato contrato = criarContrato(contratoId);
        Unidade unidade = criarUnidade(
                unidadeId, contrato, "Unidade Central", "Rua A", "12345678", "Centro", "Cidade Teste", -22.9, -47.0
        );

        when(unidadeRepository.findByIdAndContratoId(unidadeId, contratoId))
                .thenReturn(Optional.of(unidade));

        // Act
        Unidade resultado = unidadeService.buscarPorId(contratoId, unidadeId);

        // Assert
        assertSame(unidade, resultado);

        verify(unidadeRepository).findByIdAndContratoId(unidadeId, contratoId);
        verifyNoInteractions(contratoService);
    }

    @Test
    void deveLancarNotFoundQuandoUnidadeNaoPertencerAoContrato() {

        // Arrange
        Long contratoId = 1L;
        Long unidadeId = 99L;

        when(unidadeRepository.findByIdAndContratoId(unidadeId, contratoId))
                .thenReturn(Optional.empty());

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> unidadeService.buscarPorId(contratoId, unidadeId)
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Unidade não encontrada neste contrato", exception.getReason());
    }

    @Test
    void deveAtualizarCamposDaUnidadePreservandoContrato() {

        // Arrange
        Long contratoId = 1L;
        Long unidadeId = 2L;

        Contrato contratoOriginal = criarContrato(contratoId);
        Unidade unidadeExistente = criarUnidade(
                unidadeId, contratoOriginal, "Unidade Antiga", "Rua Antiga", "11111111", "Bairro Antigo",
                "Cidade Antiga", -22.9, -47.0
        );

        Contrato outroContrato = criarContrato(99L);
        Unidade novosDados = criarUnidade(
                null, outroContrato, "Unidade Nova", "Rua Nova", "22222222", "Bairro Novo",
                "Cidade Nova", -23.0, -48.0
        );

        when(unidadeRepository.findByIdAndContratoId(unidadeId, contratoId))
                .thenReturn(Optional.of(unidadeExistente));

        when(unidadeRepository.save(any(Unidade.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Unidade resultado = unidadeService.atualizar(contratoId, unidadeId, novosDados);

        // Assert
        assertSame(unidadeExistente, resultado);
        assertEquals(unidadeId, resultado.getId());
        assertSame(contratoOriginal, resultado.getContrato());
        assertEquals("Unidade Nova", resultado.getNome());
        assertEquals("Rua Nova", resultado.getEndereco());
        assertEquals("22222222", resultado.getCep());
        assertEquals("Bairro Novo", resultado.getBairro());
        assertEquals("Cidade Nova", resultado.getCidade());
        assertEquals(-23.0, resultado.getLatitude());
        assertEquals(-48.0, resultado.getLongitude());

        verify(unidadeRepository).save(unidadeExistente);
    }

    @Test
    void deveExcluirUnidadeDoContrato() {

        // Arrange
        Long contratoId = 1L;
        Long unidadeId = 2L;
        Contrato contrato = criarContrato(contratoId);
        Unidade unidade = criarUnidade(
                unidadeId, contrato, "Unidade Central", "Rua A", "12345678", "Centro", "Cidade Teste", -22.9, -47.0
        );

        when(unidadeRepository.findByIdAndContratoId(unidadeId, contratoId))
                .thenReturn(Optional.of(unidade));

        // Act
        unidadeService.excluir(contratoId, unidadeId);

        // Assert
        verify(unidadeRepository).findByIdAndContratoId(unidadeId, contratoId);
        verify(unidadeRepository).delete(unidade);
    }

    // ---------------------------------------------------------------
    // Fixtures auxiliares
    // ---------------------------------------------------------------

    private Contrato criarContrato(Long id) {
        Contrato contrato = new Contrato();
        contrato.setId(id);
        return contrato;
    }

    private Unidade criarUnidade(
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
        Unidade unidade = new Unidade();
        unidade.setId(id);
        unidade.setContrato(contrato);
        unidade.setNome(nome);
        unidade.setEndereco(endereco);
        unidade.setCep(cep);
        unidade.setBairro(bairro);
        unidade.setCidade(cidade);
        unidade.setLatitude(latitude);
        unidade.setLongitude(longitude);
        return unidade;
    }
}
