package br.com.smartdispatch.service;

import br.com.smartdispatch.dto.TecnicoResponse;
import br.com.smartdispatch.enums.PerfilUsuario;
import br.com.smartdispatch.model.BaseOperacional;
import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.model.Tecnico;
import br.com.smartdispatch.model.Usuario;
import br.com.smartdispatch.repository.TecnicoRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TecnicoServiceTest {

    @Mock
    private TecnicoRepository tecnicoRepository;

    @Mock
    private BaseOperacionalService baseOperacionalService;

    @InjectMocks
    private TecnicoService tecnicoService;

    // ---------------------------------------------------------------
    // listar()
    // ---------------------------------------------------------------

    @Test
    void deveListarTecnicosDaBaseComMapeamentoCompleto() {

        // Arrange
        Long contratoId = 1L;
        Long baseId = 2L;

        BaseOperacional base = criarBaseOperacional(baseId, contratoId, "Base Central", "Cidade Teste");

        Usuario usuario1 = criarUsuario(
                10L, "Técnico Um", "tecnico1@empresa.com", "11988887777", PerfilUsuario.TECNICO
        );
        Tecnico tecnico1 = criarTecnico(20L, usuario1, base, true);

        Usuario usuario2 = criarUsuario(
                11L, "Técnico Dois", "tecnico2@empresa.com", "11977776666", PerfilUsuario.TECNICO_INTERNO
        );
        Tecnico tecnico2 = criarTecnico(21L, usuario2, base, false);

        when(baseOperacionalService.buscarPorId(contratoId, baseId))
                .thenReturn(base);

        when(tecnicoRepository.findByBaseOperacionalId(baseId))
                .thenReturn(List.of(tecnico1, tecnico2));

        // Act
        List<TecnicoResponse> resultado = tecnicoService.listar(contratoId, baseId);

        // Assert
        assertEquals(2, resultado.size());

        TecnicoResponse response1 = resultado.get(0);
        assertEquals(tecnico1.getId(), response1.getId());
        assertEquals(usuario1.getNome(), response1.getNome());
        assertEquals(usuario1.getEmail(), response1.getEmail());
        assertEquals(usuario1.getTelefone(), response1.getTelefone());
        assertEquals(usuario1.getPerfil().name(), response1.getPerfil());
        assertTrue(response1.isAtivo());
        assertEquals(base.getId(), response1.getBaseId());
        assertEquals(base.getNome(), response1.getBaseNome());
        assertEquals(contratoId, response1.getContratoId());
        assertEquals(base.getContrato().getCidade(), response1.getContratoCidade());

        TecnicoResponse response2 = resultado.get(1);
        assertEquals(tecnico2.getId(), response2.getId());
        assertEquals(PerfilUsuario.TECNICO_INTERNO.name(), response2.getPerfil());
        assertFalse(response2.isAtivo());

        verify(baseOperacionalService).buscarPorId(contratoId, baseId);
        verify(tecnicoRepository).findByBaseOperacionalId(baseId);
    }

    // ---------------------------------------------------------------
    // buscarPorId()
    // ---------------------------------------------------------------

    @Test
    void deveBuscarTecnicoPorIdNaBaseComSucesso() {

        // Arrange
        Long contratoId = 1L;
        Long baseId = 2L;
        Long tecnicoId = 20L;

        BaseOperacional base = criarBaseOperacional(baseId, contratoId, "Base Central", "Cidade Teste");
        Usuario usuario = criarUsuario(
                10L, "Técnico Um", "tecnico1@empresa.com", "11988887777", PerfilUsuario.TECNICO
        );
        Tecnico tecnico = criarTecnico(tecnicoId, usuario, base, true);

        when(baseOperacionalService.buscarPorId(contratoId, baseId))
                .thenReturn(base);

        when(tecnicoRepository.findByIdAndBaseOperacionalId(tecnicoId, baseId))
                .thenReturn(Optional.of(tecnico));

        // Act
        TecnicoResponse response = tecnicoService.buscarPorId(contratoId, baseId, tecnicoId);

        // Assert
        assertEquals(tecnicoId, response.getId());
        assertEquals(usuario.getNome(), response.getNome());
        assertTrue(response.isAtivo());
        assertEquals(baseId, response.getBaseId());

        verify(baseOperacionalService).buscarPorId(contratoId, baseId);
        verify(tecnicoRepository).findByIdAndBaseOperacionalId(tecnicoId, baseId);
    }

    @Test
    void deveLancarNotFoundQuandoTecnicoNaoPertenceABase() {

        // Arrange
        Long contratoId = 1L;
        Long baseId = 2L;
        Long tecnicoId = 99L;

        BaseOperacional base = criarBaseOperacional(baseId, contratoId, "Base Central", "Cidade Teste");

        when(baseOperacionalService.buscarPorId(contratoId, baseId))
                .thenReturn(base);

        when(tecnicoRepository.findByIdAndBaseOperacionalId(tecnicoId, baseId))
                .thenReturn(Optional.empty());

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tecnicoService.buscarPorId(contratoId, baseId, tecnicoId)
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals(
                "Técnico não encontrado nesta base operacional",
                exception.getReason()
        );
    }

    // ---------------------------------------------------------------
    // buscarEntidadePorId()
    // ---------------------------------------------------------------

    @Test
    void deveBuscarEntidadeAtivaPorIdNoContrato() {

        // Arrange
        Long contratoId = 1L;
        Long tecnicoId = 20L;

        BaseOperacional base = criarBaseOperacional(2L, contratoId, "Base Central", "Cidade Teste");
        Usuario usuario = criarUsuario(
                10L, "Técnico Um", "tecnico1@empresa.com", "11988887777", PerfilUsuario.TECNICO
        );
        Tecnico tecnico = criarTecnico(tecnicoId, usuario, base, true);

        when(tecnicoRepository.findByIdAndBaseOperacionalContratoId(tecnicoId, contratoId))
                .thenReturn(Optional.of(tecnico));

        // Act
        Tecnico resultado = tecnicoService.buscarEntidadePorId(contratoId, tecnicoId);

        // Assert
        assertEquals(tecnico, resultado);

        verify(tecnicoRepository).findByIdAndBaseOperacionalContratoId(tecnicoId, contratoId);
    }

    @Test
    void deveLancarNotFoundQuandoTecnicoNaoPertenceAoContrato() {

        // Arrange
        Long contratoId = 1L;
        Long tecnicoId = 99L;

        when(tecnicoRepository.findByIdAndBaseOperacionalContratoId(tecnicoId, contratoId))
                .thenReturn(Optional.empty());

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tecnicoService.buscarEntidadePorId(contratoId, tecnicoId)
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals(
                "Técnico não encontrado neste contrato",
                exception.getReason()
        );
    }

    @Test
    void deveLancarConflitoQuandoTecnicoDoContratoEstiverInativo() {

        // Arrange
        Long contratoId = 1L;
        Long tecnicoId = 20L;

        BaseOperacional base = criarBaseOperacional(2L, contratoId, "Base Central", "Cidade Teste");
        Usuario usuario = criarUsuario(
                10L, "Técnico Um", "tecnico1@empresa.com", "11988887777", PerfilUsuario.TECNICO
        );
        Tecnico tecnico = criarTecnico(tecnicoId, usuario, base, false);

        when(tecnicoRepository.findByIdAndBaseOperacionalContratoId(tecnicoId, contratoId))
                .thenReturn(Optional.of(tecnico));

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tecnicoService.buscarEntidadePorId(contratoId, tecnicoId)
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Não é possível atribuir uma ordem de serviço a um técnico inativo",
                exception.getReason()
        );
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

    private BaseOperacional criarBaseOperacional(
            Long id,
            Long contratoId,
            String nome,
            String cidadeContrato
    ) {
        BaseOperacional base = new BaseOperacional();
        base.setId(id);
        base.setNome(nome);
        base.setContrato(criarContrato(contratoId, cidadeContrato));
        return base;
    }

    private Usuario criarUsuario(
            Long id,
            String nome,
            String email,
            String telefone,
            PerfilUsuario perfil
    ) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setTelefone(telefone);
        usuario.setPerfil(perfil);
        return usuario;
    }

    private Tecnico criarTecnico(Long id, Usuario usuario, BaseOperacional base, boolean ativo) {
        Tecnico tecnico = new Tecnico();
        tecnico.setId(id);
        tecnico.setUsuario(usuario);
        tecnico.setBaseOperacional(base);
        tecnico.setAtivo(ativo);
        return tecnico;
    }
}
