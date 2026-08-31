package br.com.smartdispatch.service;

import br.com.smartdispatch.dto.AlterarSenhaRequest;
import br.com.smartdispatch.dto.AtualizarStatusUsuarioRequest;
import br.com.smartdispatch.dto.AtualizarUsuarioRequest;
import br.com.smartdispatch.dto.CriarUsuarioRequest;
import br.com.smartdispatch.dto.UsuarioResponse;
import br.com.smartdispatch.enums.PerfilUsuario;
import br.com.smartdispatch.model.BaseOperacional;
import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.model.Tecnico;
import br.com.smartdispatch.model.Usuario;
import br.com.smartdispatch.repository.TecnicoRepository;
import br.com.smartdispatch.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TecnicoRepository tecnicoRepository;

    @Mock
    private BaseOperacionalService baseOperacionalService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    // ---------------------------------------------------------------
    // criar()
    // ---------------------------------------------------------------

    @Test
    void deveCriarUsuarioGestorComDadosNormalizadosESenhaPadrao() {

        // Arrange
        CriarUsuarioRequest request = new CriarUsuarioRequest();
        request.setNome("  Gestor Teste  ");
        request.setEmail("  GESTOR@EMPRESA.COM  ");
        request.setTelefone("11988887777");
        request.setPerfil(PerfilUsuario.CTO);

        when(usuarioRepository.existsByEmail("gestor@empresa.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("cto"))
                .thenReturn("hash-cto");

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> {
                    Usuario usuario = invocation.getArgument(0);
                    usuario.setId(1L);
                    return usuario;
                });

        // Act
        UsuarioResponse response = usuarioService.criar(request);

        // Assert
        assertEquals("Gestor Teste", response.getNome());
        assertEquals("gestor@empresa.com", response.getEmail());
        assertEquals(PerfilUsuario.CTO, response.getPerfil());
        assertTrue(response.isAtivo());
        assertNull(response.getContratoId());
        assertNull(response.getBaseOperacionalId());

        verify(usuarioRepository).existsByEmail("gestor@empresa.com");

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());

        Usuario usuarioSalvo = usuarioCaptor.getValue();
        assertEquals("Gestor Teste", usuarioSalvo.getNome());
        assertEquals("gestor@empresa.com", usuarioSalvo.getEmail());
        assertEquals("hash-cto", usuarioSalvo.getSenha());
        assertTrue(usuarioSalvo.isAtivo());

        verifyNoInteractions(baseOperacionalService);
        verifyNoInteractions(tecnicoRepository);
    }

    @ParameterizedTest
    @EnumSource(value = PerfilUsuario.class, names = {"TECNICO", "TECNICO_INTERNO"})
    void deveCriarUsuarioTecnicoComVinculoOperacional(PerfilUsuario perfil) {

        // Arrange
        Long contratoId = 1L;
        Long baseId = 2L;

        CriarUsuarioRequest request = new CriarUsuarioRequest();
        request.setNome("Técnico Teste");
        request.setEmail("tecnico@empresa.com");
        request.setPerfil(perfil);
        request.setContratoId(contratoId);
        request.setBaseOperacionalId(baseId);

        BaseOperacional base = criarBaseOperacional(baseId, contratoId, "Base Central");

        when(usuarioRepository.existsByEmail("tecnico@empresa.com"))
                .thenReturn(false);

        when(baseOperacionalService.buscarPorId(contratoId, baseId))
                .thenReturn(base);

        when(passwordEncoder.encode("cto"))
                .thenReturn("hash-cto");

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> {
                    Usuario usuario = invocation.getArgument(0);
                    usuario.setId(10L);
                    return usuario;
                });

        AtomicReference<Tecnico> tecnicoCriado = new AtomicReference<>();

        when(tecnicoRepository.save(any(Tecnico.class)))
                .thenAnswer(invocation -> {
                    Tecnico tecnico = invocation.getArgument(0);
                    tecnicoCriado.set(tecnico);
                    return tecnico;
                });

        when(tecnicoRepository.findByUsuarioId(10L))
                .thenAnswer(invocation -> Optional.ofNullable(tecnicoCriado.get()));

        // Act
        UsuarioResponse response = usuarioService.criar(request);

        // Assert
        assertTrue(response.isAtivo());
        assertEquals(perfil, response.getPerfil());
        assertEquals(contratoId, response.getContratoId());
        assertEquals(baseId, response.getBaseOperacionalId());

        verify(passwordEncoder).encode("cto");

        ArgumentCaptor<Tecnico> tecnicoCaptor = ArgumentCaptor.forClass(Tecnico.class);
        verify(tecnicoRepository).save(tecnicoCaptor.capture());

        Tecnico tecnicoSalvo = tecnicoCaptor.getValue();
        assertEquals(10L, tecnicoSalvo.getUsuario().getId());
        assertEquals(base, tecnicoSalvo.getBaseOperacional());
        assertTrue(tecnicoSalvo.isAtivo());
    }

    @Test
    void deveLancarBadRequestQuandoDadosObrigatoriosDeCriacaoForemInvalidos() {

        // Arrange
        CriarUsuarioRequest requestNomeNulo = new CriarUsuarioRequest();
        requestNomeNulo.setNome(null);
        requestNomeNulo.setEmail("valido@empresa.com");
        requestNomeNulo.setPerfil(PerfilUsuario.CTO);

        CriarUsuarioRequest requestNomeEmBranco = new CriarUsuarioRequest();
        requestNomeEmBranco.setNome("   ");
        requestNomeEmBranco.setEmail("valido@empresa.com");
        requestNomeEmBranco.setPerfil(PerfilUsuario.CTO);

        CriarUsuarioRequest requestEmailNulo = new CriarUsuarioRequest();
        requestEmailNulo.setNome("Nome Válido");
        requestEmailNulo.setEmail(null);
        requestEmailNulo.setPerfil(PerfilUsuario.CTO);

        CriarUsuarioRequest requestEmailEmBranco = new CriarUsuarioRequest();
        requestEmailEmBranco.setNome("Nome Válido");
        requestEmailEmBranco.setEmail("   ");
        requestEmailEmBranco.setPerfil(PerfilUsuario.CTO);

        CriarUsuarioRequest requestPerfilNulo = new CriarUsuarioRequest();
        requestPerfilNulo.setNome("Nome Válido");
        requestPerfilNulo.setEmail("valido@empresa.com");
        requestPerfilNulo.setPerfil(null);

        // Act & Assert
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> usuarioService.criar(requestNomeNulo)).getStatusCode());

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> usuarioService.criar(requestNomeEmBranco)).getStatusCode());

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> usuarioService.criar(requestEmailNulo)).getStatusCode());

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> usuarioService.criar(requestEmailEmBranco)).getStatusCode());

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> usuarioService.criar(requestPerfilNulo)).getStatusCode());

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(tecnicoRepository, never()).save(any(Tecnico.class));
    }

    @Test
    void deveLancarConflitoQuandoEmailJaExistirConsiderandoNormalizacao() {

        // Arrange
        CriarUsuarioRequest request = new CriarUsuarioRequest();
        request.setNome("Usuário Teste");
        request.setEmail("  USUARIO@EMAIL.COM  ");
        request.setPerfil(PerfilUsuario.CTO);

        when(usuarioRepository.existsByEmail("usuario@email.com"))
                .thenReturn(true);

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.criar(request)
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Já existe um usuário cadastrado com este e-mail",
                exception.getReason()
        );

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void deveExigirContratoEBaseParaCriacaoDeTecnico() {

        // Arrange
        CriarUsuarioRequest requestSemBase = new CriarUsuarioRequest();
        requestSemBase.setNome("Técnico Teste");
        requestSemBase.setEmail("tecnico1@empresa.com");
        requestSemBase.setPerfil(PerfilUsuario.TECNICO);
        requestSemBase.setContratoId(1L);
        requestSemBase.setBaseOperacionalId(null);

        CriarUsuarioRequest requestSemContrato = new CriarUsuarioRequest();
        requestSemContrato.setNome("Técnico Teste");
        requestSemContrato.setEmail("tecnico2@empresa.com");
        requestSemContrato.setPerfil(PerfilUsuario.TECNICO);
        requestSemContrato.setContratoId(null);
        requestSemContrato.setBaseOperacionalId(2L);

        when(usuarioRepository.existsByEmail(anyString()))
                .thenReturn(false);

        // Act
        ResponseStatusException excecaoSemBase = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.criar(requestSemBase)
        );

        ResponseStatusException excecaoSemContrato = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.criar(requestSemContrato)
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, excecaoSemBase.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, excecaoSemContrato.getStatusCode());

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(tecnicoRepository, never()).save(any(Tecnico.class));
        verifyNoInteractions(baseOperacionalService);
    }

    // ---------------------------------------------------------------
    // buscarPorId()
    // ---------------------------------------------------------------

    @Test
    void deveLancarNotFoundAoBuscarUsuarioInexistente() {

        // Arrange
        Long usuarioId = 99L;

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.empty());

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.buscarPorId(usuarioId)
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Usuário não encontrado", exception.getReason());

        verifyNoInteractions(tecnicoRepository);
    }

    // ---------------------------------------------------------------
    // atualizar()
    // ---------------------------------------------------------------

    @Test
    void deveLancarNotFoundAoAtualizarUsuarioInexistente() {

        // Arrange
        Long usuarioId = 99L;

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.empty());

        AtualizarUsuarioRequest request = new AtualizarUsuarioRequest();
        request.setPerfil(PerfilUsuario.CTO);
        request.setNome("Nome");
        request.setEmail("email@empresa.com");

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.atualizar(usuarioId, request)
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Usuário não encontrado", exception.getReason());

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void deveLancarBadRequestQuandoDadosObrigatoriosDeAtualizacaoForemInvalidos() {

        // Arrange
        Long usuarioId = 1L;
        Usuario usuarioExistente =
                criarUsuario(usuarioId, "Nome Atual", "atual@empresa.com", PerfilUsuario.CTO, true);

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(usuarioExistente));

        AtualizarUsuarioRequest requestPerfilNulo = new AtualizarUsuarioRequest();
        requestPerfilNulo.setPerfil(null);

        AtualizarUsuarioRequest requestNomeNulo = new AtualizarUsuarioRequest();
        requestNomeNulo.setPerfil(PerfilUsuario.CTO);
        requestNomeNulo.setNome(null);
        requestNomeNulo.setEmail("valido@empresa.com");

        AtualizarUsuarioRequest requestNomeEmBranco = new AtualizarUsuarioRequest();
        requestNomeEmBranco.setPerfil(PerfilUsuario.CTO);
        requestNomeEmBranco.setNome("   ");
        requestNomeEmBranco.setEmail("valido@empresa.com");

        AtualizarUsuarioRequest requestEmailNulo = new AtualizarUsuarioRequest();
        requestEmailNulo.setPerfil(PerfilUsuario.CTO);
        requestEmailNulo.setNome("Nome Válido");
        requestEmailNulo.setEmail(null);

        AtualizarUsuarioRequest requestEmailEmBranco = new AtualizarUsuarioRequest();
        requestEmailEmBranco.setPerfil(PerfilUsuario.CTO);
        requestEmailEmBranco.setNome("Nome Válido");
        requestEmailEmBranco.setEmail("   ");

        // Act & Assert
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> usuarioService.atualizar(usuarioId, requestPerfilNulo)).getStatusCode());

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> usuarioService.atualizar(usuarioId, requestNomeNulo)).getStatusCode());

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> usuarioService.atualizar(usuarioId, requestNomeEmBranco)).getStatusCode());

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> usuarioService.atualizar(usuarioId, requestEmailNulo)).getStatusCode());

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> usuarioService.atualizar(usuarioId, requestEmailEmBranco)).getStatusCode());

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void deveLancarConflitoQuandoEmailPertencerAOutroUsuario() {

        // Arrange
        Long usuarioId = 1L;
        Usuario usuarioExistente =
                criarUsuario(usuarioId, "Nome Atual", "atual@empresa.com", PerfilUsuario.CTO, true);

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(usuarioExistente));

        when(usuarioRepository.existsByEmailAndIdNot("outro@empresa.com", usuarioId))
                .thenReturn(true);

        AtualizarUsuarioRequest request = new AtualizarUsuarioRequest();
        request.setPerfil(PerfilUsuario.CTO);
        request.setNome("Nome Atual");
        request.setEmail("  OUTRO@EMPRESA.COM  ");

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.atualizar(usuarioId, request)
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Já existe outro usuário cadastrado com este e-mail",
                exception.getReason()
        );

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void deveExigirContratoEBaseInformadosJuntosParaPerfilTecnico() {

        // Arrange
        Long usuarioId = 1L;
        Usuario usuarioExistente =
                criarUsuario(usuarioId, "Técnico Atual", "tecnico@empresa.com", PerfilUsuario.TECNICO, true);

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(usuarioExistente));

        when(usuarioRepository.existsByEmailAndIdNot("tecnico@empresa.com", usuarioId))
                .thenReturn(false);

        AtualizarUsuarioRequest requestSomenteContrato = new AtualizarUsuarioRequest();
        requestSomenteContrato.setPerfil(PerfilUsuario.TECNICO);
        requestSomenteContrato.setNome("Técnico Atual");
        requestSomenteContrato.setEmail("tecnico@empresa.com");
        requestSomenteContrato.setContratoId(1L);
        requestSomenteContrato.setBaseOperacionalId(null);

        AtualizarUsuarioRequest requestSomenteBase = new AtualizarUsuarioRequest();
        requestSomenteBase.setPerfil(PerfilUsuario.TECNICO);
        requestSomenteBase.setNome("Técnico Atual");
        requestSomenteBase.setEmail("tecnico@empresa.com");
        requestSomenteBase.setContratoId(null);
        requestSomenteBase.setBaseOperacionalId(2L);

        // Act
        ResponseStatusException excecaoSomenteContrato = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.atualizar(usuarioId, requestSomenteContrato)
        );

        ResponseStatusException excecaoSomenteBase = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.atualizar(usuarioId, requestSomenteBase)
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, excecaoSomenteContrato.getStatusCode());
        assertEquals(
                "Contrato e base operacional devem ser informados juntos",
                excecaoSomenteContrato.getReason()
        );

        assertEquals(HttpStatus.BAD_REQUEST, excecaoSomenteBase.getStatusCode());
        assertEquals(
                "Contrato e base operacional devem ser informados juntos",
                excecaoSomenteBase.getReason()
        );

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verifyNoInteractions(tecnicoRepository);
    }

    @Test
    void deveLancarConflitoQuandoUsuarioTecnicoNaoPossuirVinculoOperacional() {

        // Arrange
        Long usuarioId = 1L;
        Usuario usuarioExistente =
                criarUsuario(usuarioId, "Técnico Atual", "tecnico@empresa.com", PerfilUsuario.TECNICO, true);

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(usuarioExistente));

        when(usuarioRepository.existsByEmailAndIdNot("tecnico@empresa.com", usuarioId))
                .thenReturn(false);

        when(tecnicoRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.empty());

        AtualizarUsuarioRequest request = new AtualizarUsuarioRequest();
        request.setPerfil(PerfilUsuario.TECNICO);
        request.setNome("Técnico Atual");
        request.setEmail("tecnico@empresa.com");
        request.setContratoId(null);
        request.setBaseOperacionalId(null);

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.atualizar(usuarioId, request)
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Usuário técnico não possui vínculo operacional",
                exception.getReason()
        );

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void deveTransformarGestorEmTecnicoCriandoVinculoQuandoNaoExisteHistorico() {

        // Arrange
        Long usuarioId = 1L;
        Long contratoId = 5L;
        Long baseId = 6L;

        Usuario usuarioExistente =
                criarUsuario(usuarioId, "Gestor Antigo", "gestor@empresa.com", PerfilUsuario.CTO, false);

        BaseOperacional novaBase = criarBaseOperacional(baseId, contratoId, "Base Nova");

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(usuarioExistente));

        when(usuarioRepository.existsByEmailAndIdNot("gestor@empresa.com", usuarioId))
                .thenReturn(false);

        when(baseOperacionalService.buscarPorId(contratoId, baseId))
                .thenReturn(novaBase);

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AtomicReference<Tecnico> tecnicoCriado = new AtomicReference<>();

        when(tecnicoRepository.findByUsuarioId(usuarioId))
                .thenAnswer(invocation -> Optional.ofNullable(tecnicoCriado.get()));

        when(tecnicoRepository.save(any(Tecnico.class)))
                .thenAnswer(invocation -> {
                    Tecnico tecnico = invocation.getArgument(0);
                    tecnicoCriado.set(tecnico);
                    return tecnico;
                });

        AtualizarUsuarioRequest request = new AtualizarUsuarioRequest();
        request.setPerfil(PerfilUsuario.TECNICO);
        request.setNome("Gestor Antigo");
        request.setEmail("gestor@empresa.com");
        request.setContratoId(contratoId);
        request.setBaseOperacionalId(baseId);

        // Act
        UsuarioResponse response = usuarioService.atualizar(usuarioId, request);

        // Assert
        assertEquals(PerfilUsuario.TECNICO, response.getPerfil());
        assertFalse(response.isAtivo());
        assertEquals(contratoId, response.getContratoId());
        assertEquals(baseId, response.getBaseOperacionalId());

        ArgumentCaptor<Tecnico> tecnicoCaptor = ArgumentCaptor.forClass(Tecnico.class);
        verify(tecnicoRepository).save(tecnicoCaptor.capture());

        Tecnico tecnicoSalvo = tecnicoCaptor.getValue();
        assertEquals(usuarioExistente, tecnicoSalvo.getUsuario());
        assertEquals(novaBase, tecnicoSalvo.getBaseOperacional());
        assertFalse(tecnicoSalvo.isAtivo());

        verify(usuarioRepository).save(usuarioExistente);
    }

    @Test
    void deveReativarVinculoHistoricoAoTransformarGestorEmTecnico() {

        // Arrange
        Long usuarioId = 1L;
        Long contratoId = 5L;
        Long baseId = 6L;

        Usuario usuarioExistente =
                criarUsuario(usuarioId, "Gestor Atual", "gestor@empresa.com", PerfilUsuario.CTO, true);

        BaseOperacional baseAntiga = criarBaseOperacional(7L, 8L, "Base Antiga");
        Tecnico tecnicoHistorico = criarTecnico(20L, usuarioExistente, baseAntiga, false);

        BaseOperacional novaBase = criarBaseOperacional(baseId, contratoId, "Base Nova");

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(usuarioExistente));

        when(usuarioRepository.existsByEmailAndIdNot("gestor@empresa.com", usuarioId))
                .thenReturn(false);

        when(tecnicoRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(tecnicoHistorico));

        when(baseOperacionalService.buscarPorId(contratoId, baseId))
                .thenReturn(novaBase);

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AtualizarUsuarioRequest request = new AtualizarUsuarioRequest();
        request.setPerfil(PerfilUsuario.TECNICO);
        request.setNome("Gestor Atual");
        request.setEmail("gestor@empresa.com");
        request.setContratoId(contratoId);
        request.setBaseOperacionalId(baseId);

        // Act
        UsuarioResponse response = usuarioService.atualizar(usuarioId, request);

        // Assert
        assertEquals(PerfilUsuario.TECNICO, response.getPerfil());
        assertEquals(contratoId, response.getContratoId());
        assertEquals(baseId, response.getBaseOperacionalId());

        assertEquals(novaBase, tecnicoHistorico.getBaseOperacional());
        assertTrue(tecnicoHistorico.isAtivo());
    }

    @Test
    void deveExigirContratoEBaseAoTransformarGestorEmTecnico() {

        // Arrange
        Long usuarioId = 1L;
        Usuario usuarioExistente =
                criarUsuario(usuarioId, "Gestor Atual", "gestor@empresa.com", PerfilUsuario.CTO, true);

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(usuarioExistente));

        when(usuarioRepository.existsByEmailAndIdNot("gestor@empresa.com", usuarioId))
                .thenReturn(false);

        AtualizarUsuarioRequest request = new AtualizarUsuarioRequest();
        request.setPerfil(PerfilUsuario.TECNICO);
        request.setNome("Gestor Atual");
        request.setEmail("gestor@empresa.com");
        request.setContratoId(null);
        request.setBaseOperacionalId(null);

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.atualizar(usuarioId, request)
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
                "Contrato e base operacional devem ser informados ao transformar um usuário em técnico",
                exception.getReason()
        );

        verifyNoInteractions(baseOperacionalService);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void deveDesativarVinculoAoTransformarTecnicoEmGestor() {

        // Arrange
        Long usuarioId = 1L;

        Usuario usuarioExistente =
                criarUsuario(usuarioId, "Técnico Atual", "tecnico@empresa.com", PerfilUsuario.TECNICO, true);

        BaseOperacional base = criarBaseOperacional(6L, 5L, "Base Atual");
        Tecnico tecnicoExistente = criarTecnico(20L, usuarioExistente, base, true);

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(usuarioExistente));

        when(usuarioRepository.existsByEmailAndIdNot("novoemail@empresa.com", usuarioId))
                .thenReturn(false);

        when(tecnicoRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(tecnicoExistente));

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AtualizarUsuarioRequest request = new AtualizarUsuarioRequest();
        request.setPerfil(PerfilUsuario.CTO);
        request.setNome("  Novo Nome Gestor  ");
        request.setEmail("  NOVOEMAIL@EMPRESA.COM  ");

        // Act
        UsuarioResponse response = usuarioService.atualizar(usuarioId, request);

        // Assert
        assertEquals(PerfilUsuario.CTO, response.getPerfil());
        assertTrue(response.isAtivo());
        assertEquals("Novo Nome Gestor", response.getNome());
        assertEquals("novoemail@empresa.com", response.getEmail());
        assertNull(response.getContratoId());
        assertNull(response.getBaseOperacionalId());

        assertFalse(tecnicoExistente.isAtivo());
    }

    @Test
    void deveAlterarBaseDeTecnicoMantendoVinculo() {

        // Arrange
        Long usuarioId = 1L;
        Long novoContratoId = 5L;
        Long novaBaseId = 6L;

        Usuario usuarioExistente =
                criarUsuario(usuarioId, "Técnico Atual", "tecnico@empresa.com", PerfilUsuario.TECNICO, true);

        BaseOperacional baseAntiga = criarBaseOperacional(7L, 8L, "Base Antiga");
        Tecnico tecnicoExistente = criarTecnico(20L, usuarioExistente, baseAntiga, true);

        BaseOperacional novaBase = criarBaseOperacional(novaBaseId, novoContratoId, "Base Nova");

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(usuarioExistente));

        when(usuarioRepository.existsByEmailAndIdNot("tecnico@empresa.com", usuarioId))
                .thenReturn(false);

        when(tecnicoRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(tecnicoExistente));

        when(baseOperacionalService.buscarPorId(novoContratoId, novaBaseId))
                .thenReturn(novaBase);

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AtualizarUsuarioRequest request = new AtualizarUsuarioRequest();
        request.setPerfil(PerfilUsuario.TECNICO_INTERNO);
        request.setNome("Técnico Atual");
        request.setEmail("tecnico@empresa.com");
        request.setContratoId(novoContratoId);
        request.setBaseOperacionalId(novaBaseId);

        // Act
        UsuarioResponse response = usuarioService.atualizar(usuarioId, request);

        // Assert
        assertEquals(PerfilUsuario.TECNICO_INTERNO, response.getPerfil());
        assertEquals(novoContratoId, response.getContratoId());
        assertEquals(novaBaseId, response.getBaseOperacionalId());

        assertEquals(novaBase, tecnicoExistente.getBaseOperacional());
    }

    @Test
    void deveManterVinculoOperacionalAoAtualizarTecnicoSemInformarNovaBase() {

        // Arrange
        Long usuarioId = 1L;

        Usuario usuarioExistente =
                criarUsuario(usuarioId, "Técnico Atual", "tecnico@empresa.com", PerfilUsuario.TECNICO, true);

        BaseOperacional baseAtual = criarBaseOperacional(7L, 8L, "Base Atual");
        Tecnico tecnicoExistente = criarTecnico(20L, usuarioExistente, baseAtual, true);

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(usuarioExistente));

        when(usuarioRepository.existsByEmailAndIdNot("tecnico@empresa.com", usuarioId))
                .thenReturn(false);

        when(tecnicoRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(tecnicoExistente));

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AtualizarUsuarioRequest request = new AtualizarUsuarioRequest();
        request.setPerfil(PerfilUsuario.TECNICO);
        request.setNome("Técnico Atual");
        request.setEmail("tecnico@empresa.com");
        request.setContratoId(null);
        request.setBaseOperacionalId(null);

        // Act
        UsuarioResponse response = usuarioService.atualizar(usuarioId, request);

        // Assert
        assertEquals(PerfilUsuario.TECNICO, response.getPerfil());
        assertEquals(baseAtual.getId(), response.getBaseOperacionalId());
        assertTrue(tecnicoExistente.isAtivo());
        assertEquals(baseAtual, tecnicoExistente.getBaseOperacional());

        verifyNoInteractions(baseOperacionalService);
    }

    // ---------------------------------------------------------------
    // atualizarStatus()
    // ---------------------------------------------------------------

    @Test
    void deveLancarBadRequestQuandoAtivoNaoForInformado() {

        // Arrange
        Long usuarioId = 1L;
        AtualizarStatusUsuarioRequest request = new AtualizarStatusUsuarioRequest();
        request.setAtivo(null);

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.atualizarStatus(usuarioId, request)
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("O campo ativo deve ser informado", exception.getReason());

        verifyNoInteractions(usuarioRepository);
        verifyNoInteractions(tecnicoRepository);
    }

    @Test
    void deveLancarNotFoundAoAtualizarStatusDeUsuarioInexistente() {

        // Arrange
        Long usuarioId = 99L;

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.empty());

        AtualizarStatusUsuarioRequest request = new AtualizarStatusUsuarioRequest();
        request.setAtivo(true);

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.atualizarStatus(usuarioId, request)
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Usuário não encontrado", exception.getReason());

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verifyNoInteractions(tecnicoRepository);
    }

    @ParameterizedTest
    @CsvSource({
            "TECNICO, true",
            "TECNICO, false",
            "TECNICO_INTERNO, true",
            "TECNICO_INTERNO, false"
    })
    void deveSincronizarStatusDeUsuarioTecnicoComVinculoOperacional(PerfilUsuario perfil, boolean ativo) {

        // Arrange
        Long usuarioId = 1L;

        Usuario usuarioExistente = criarUsuario(
                usuarioId, "Técnico Teste", "tecnico@empresa.com", perfil, !ativo
        );

        BaseOperacional base = criarBaseOperacional(6L, 5L, "Base Atual");
        Tecnico tecnicoExistente = criarTecnico(20L, usuarioExistente, base, !ativo);

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(usuarioExistente));

        when(tecnicoRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(tecnicoExistente));

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AtualizarStatusUsuarioRequest request = new AtualizarStatusUsuarioRequest();
        request.setAtivo(ativo);

        // Act
        UsuarioResponse response = usuarioService.atualizarStatus(usuarioId, request);

        // Assert
        assertEquals(ativo, response.isAtivo());
        assertEquals(ativo, usuarioExistente.isAtivo());
        assertEquals(ativo, tecnicoExistente.isAtivo());
        assertEquals(base.getId(), response.getBaseOperacionalId());

        verify(usuarioRepository).save(usuarioExistente);
    }

    @Test
    void deveLancarConflitoAoAtualizarStatusDeTecnicoSemVinculo() {

        // Arrange
        Long usuarioId = 1L;

        Usuario usuarioExistente =
                criarUsuario(usuarioId, "Técnico Teste", "tecnico@empresa.com", PerfilUsuario.TECNICO, true);

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(usuarioExistente));

        when(tecnicoRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.empty());

        AtualizarStatusUsuarioRequest request = new AtualizarStatusUsuarioRequest();
        request.setAtivo(false);

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.atualizarStatus(usuarioId, request)
        );

        // Assert
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(
                "Usuário técnico não possui vínculo operacional",
                exception.getReason()
        );

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void deveAtualizarStatusDeGestorSemAlterarVinculoOperacional() {

        // Arrange
        Long usuarioId = 1L;

        Usuario usuarioExistente =
                criarUsuario(usuarioId, "Gestor Teste", "gestor@empresa.com", PerfilUsuario.CTO, true);

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(usuarioExistente));

        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AtualizarStatusUsuarioRequest request = new AtualizarStatusUsuarioRequest();
        request.setAtivo(false);

        // Act
        UsuarioResponse response = usuarioService.atualizarStatus(usuarioId, request);

        // Assert
        assertFalse(response.isAtivo());
        assertFalse(usuarioExistente.isAtivo());

        verifyNoInteractions(tecnicoRepository);
        verify(usuarioRepository).save(usuarioExistente);
    }

    // ---------------------------------------------------------------
    // resetarSenha()
    // ---------------------------------------------------------------

    @Test
    void deveLancarNotFoundAoResetarSenhaDeUsuarioInexistente() {

        // Arrange
        Long usuarioId = 99L;

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.empty());

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.resetarSenha(usuarioId)
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Usuário não encontrado", exception.getReason());

        verifyNoInteractions(passwordEncoder);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void deveResetarSenhaParaPadraoCto() {

        // Arrange
        Long usuarioId = 1L;
        Usuario usuarioExistente =
                criarUsuario(usuarioId, "Usuário Teste", "usuario@empresa.com", PerfilUsuario.CTO, true);

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(usuarioExistente));

        when(passwordEncoder.encode("cto"))
                .thenReturn("hash-cto-conhecido");

        // Act
        usuarioService.resetarSenha(usuarioId);

        // Assert
        assertEquals("hash-cto-conhecido", usuarioExistente.getSenha());

        verify(passwordEncoder).encode("cto");
        verify(usuarioRepository).save(usuarioExistente);
    }

    // ---------------------------------------------------------------
    // alterarPropriaSenha()
    // ---------------------------------------------------------------

    @Test
    void deveLancarBadRequestQuandoAlgumaSenhaObrigatoriaNaoForInformada() {

        // Arrange
        Authentication authentication = criarAuthenticationJwt(1L);

        AlterarSenhaRequest requestSenhaAtualNula = new AlterarSenhaRequest();
        requestSenhaAtualNula.setSenhaAtual(null);
        requestSenhaAtualNula.setNovaSenha("novaSenha123");
        requestSenhaAtualNula.setConfirmacaoSenha("novaSenha123");

        AlterarSenhaRequest requestSenhaAtualEmBranco = new AlterarSenhaRequest();
        requestSenhaAtualEmBranco.setSenhaAtual("   ");
        requestSenhaAtualEmBranco.setNovaSenha("novaSenha123");
        requestSenhaAtualEmBranco.setConfirmacaoSenha("novaSenha123");

        AlterarSenhaRequest requestNovaSenhaNula = new AlterarSenhaRequest();
        requestNovaSenhaNula.setSenhaAtual("senhaAtual123");
        requestNovaSenhaNula.setNovaSenha(null);
        requestNovaSenhaNula.setConfirmacaoSenha("novaSenha123");

        AlterarSenhaRequest requestNovaSenhaEmBranco = new AlterarSenhaRequest();
        requestNovaSenhaEmBranco.setSenhaAtual("senhaAtual123");
        requestNovaSenhaEmBranco.setNovaSenha("   ");
        requestNovaSenhaEmBranco.setConfirmacaoSenha("novaSenha123");

        AlterarSenhaRequest requestConfirmacaoNula = new AlterarSenhaRequest();
        requestConfirmacaoNula.setSenhaAtual("senhaAtual123");
        requestConfirmacaoNula.setNovaSenha("novaSenha123");
        requestConfirmacaoNula.setConfirmacaoSenha(null);

        AlterarSenhaRequest requestConfirmacaoEmBranco = new AlterarSenhaRequest();
        requestConfirmacaoEmBranco.setSenhaAtual("senhaAtual123");
        requestConfirmacaoEmBranco.setNovaSenha("novaSenha123");
        requestConfirmacaoEmBranco.setConfirmacaoSenha("   ");

        // Act & Assert
        assertBadRequestSenhasObrigatorias(requestSenhaAtualNula, authentication);
        assertBadRequestSenhasObrigatorias(requestSenhaAtualEmBranco, authentication);
        assertBadRequestSenhasObrigatorias(requestNovaSenhaNula, authentication);
        assertBadRequestSenhasObrigatorias(requestNovaSenhaEmBranco, authentication);
        assertBadRequestSenhasObrigatorias(requestConfirmacaoNula, authentication);
        assertBadRequestSenhasObrigatorias(requestConfirmacaoEmBranco, authentication);

        verifyNoInteractions(usuarioRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void deveLancarBadRequestQuandoConfirmacaoNaoCorresponderANovaSenha() {

        // Arrange
        Authentication authentication = criarAuthenticationJwt(1L);

        AlterarSenhaRequest request = new AlterarSenhaRequest();
        request.setSenhaAtual("senhaAtual123");
        request.setNovaSenha("novaSenha123");
        request.setConfirmacaoSenha("outraSenha456");

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.alterarPropriaSenha(request, authentication)
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
                "A confirmação da senha não corresponde à nova senha",
                exception.getReason()
        );

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void deveLancarUnauthorizedQuandoAuthenticationNaoForJwt() {

        // Arrange
        Authentication authentication = new TestingAuthenticationToken("usuario", null);

        AlterarSenhaRequest request = new AlterarSenhaRequest();
        request.setSenhaAtual("senhaAtual123");
        request.setNovaSenha("novaSenha123");
        request.setConfirmacaoSenha("novaSenha123");

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.alterarPropriaSenha(request, authentication)
        );

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Usuário não autenticado", exception.getReason());

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void deveLancarUnauthorizedQuandoJwtNaoPossuirUsuarioId() {

        // Arrange
        Authentication authentication = criarAuthenticationJwt(null);

        AlterarSenhaRequest request = new AlterarSenhaRequest();
        request.setSenhaAtual("senhaAtual123");
        request.setNovaSenha("novaSenha123");
        request.setConfirmacaoSenha("novaSenha123");

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.alterarPropriaSenha(request, authentication)
        );

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Usuário não identificado", exception.getReason());

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void deveLancarNotFoundQuandoUsuarioDoJwtNaoExistir() {

        // Arrange
        Long usuarioId = 1L;
        Authentication authentication = criarAuthenticationJwt(usuarioId);

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.empty());

        AlterarSenhaRequest request = new AlterarSenhaRequest();
        request.setSenhaAtual("senhaAtual123");
        request.setNovaSenha("novaSenha123");
        request.setConfirmacaoSenha("novaSenha123");

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.alterarPropriaSenha(request, authentication)
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void deveLancarBadRequestQuandoSenhaAtualEstiverIncorreta() {

        // Arrange
        Long usuarioId = 1L;
        Authentication authentication = criarAuthenticationJwt(usuarioId);

        Usuario usuarioExistente =
                criarUsuario(usuarioId, "Usuário Teste", "usuario@empresa.com", PerfilUsuario.CTO, true);

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(usuarioExistente));

        when(passwordEncoder.matches("senhaAtualErrada", usuarioExistente.getSenha()))
                .thenReturn(false);

        AlterarSenhaRequest request = new AlterarSenhaRequest();
        request.setSenhaAtual("senhaAtualErrada");
        request.setNovaSenha("novaSenha123");
        request.setConfirmacaoSenha("novaSenha123");

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.alterarPropriaSenha(request, authentication)
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Senha atual incorreta", exception.getReason());

        verify(passwordEncoder, never()).encode(anyString());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void deveAlterarPropriaSenhaComSucesso() {

        // Arrange
        Long usuarioId = 1L;
        Authentication authentication = criarAuthenticationJwt(usuarioId);

        Usuario usuarioExistente =
                criarUsuario(usuarioId, "Usuário Teste", "usuario@empresa.com", PerfilUsuario.CTO, true);

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(usuarioExistente));

        when(passwordEncoder.matches("senhaAtual123", "hash-atual"))
                .thenReturn(true);

        when(passwordEncoder.encode("novaSenha123"))
                .thenReturn("novo-hash");

        AlterarSenhaRequest request = new AlterarSenhaRequest();
        request.setSenhaAtual("senhaAtual123");
        request.setNovaSenha("novaSenha123");
        request.setConfirmacaoSenha("novaSenha123");

        // Act
        usuarioService.alterarPropriaSenha(request, authentication);

        // Assert
        assertEquals("novo-hash", usuarioExistente.getSenha());

        verify(passwordEncoder).matches("senhaAtual123", "hash-atual");
        verify(passwordEncoder).encode("novaSenha123");
        verify(usuarioRepository).save(usuarioExistente);
    }

    // ---------------------------------------------------------------
    // Fixtures auxiliares
    // ---------------------------------------------------------------

    private Usuario criarUsuario(
            Long id,
            String nome,
            String email,
            PerfilUsuario perfil,
            boolean ativo
    ) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setPerfil(perfil);
        usuario.setSenha("hash-atual");
        usuario.setAtivo(ativo);
        return usuario;
    }

    private Contrato criarContrato(Long id) {
        Contrato contrato = new Contrato();
        contrato.setId(id);
        contrato.setCidade("Cidade Teste");
        return contrato;
    }

    private BaseOperacional criarBaseOperacional(Long id, Long contratoId, String nome) {
        BaseOperacional base = new BaseOperacional();
        base.setId(id);
        base.setNome(nome);
        base.setContrato(criarContrato(contratoId));
        return base;
    }

    private Tecnico criarTecnico(Long id, Usuario usuario, BaseOperacional base, boolean ativo) {
        Tecnico tecnico = new Tecnico();
        tecnico.setId(id);
        tecnico.setUsuario(usuario);
        tecnico.setBaseOperacional(base);
        tecnico.setAtivo(ativo);
        return tecnico;
    }

    private Jwt criarJwt(Long usuarioId) {
        Jwt.Builder builder = Jwt.withTokenValue("token-teste")
                .header("alg", "HS256")
                .claim("sub", "usuario@teste.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));

        if (usuarioId != null) {
            builder.claim("usuarioId", usuarioId);
        }

        return builder.build();
    }

    private JwtAuthenticationToken criarAuthenticationJwt(Long usuarioId) {
        return new JwtAuthenticationToken(criarJwt(usuarioId));
    }

    private void assertBadRequestSenhasObrigatorias(
            AlterarSenhaRequest request,
            Authentication authentication
    ) {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.alterarPropriaSenha(request, authentication)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
                "Todas as senhas devem ser informadas",
                exception.getReason()
        );
    }
}
