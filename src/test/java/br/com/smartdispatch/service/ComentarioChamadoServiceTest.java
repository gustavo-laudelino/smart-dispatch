package br.com.smartdispatch.service;

import br.com.smartdispatch.dto.ComentarioChamadoRequest;
import br.com.smartdispatch.dto.ComentarioChamadoResponse;
import br.com.smartdispatch.enums.StatusChamado;
import br.com.smartdispatch.model.Chamado;
import br.com.smartdispatch.model.ComentarioChamado;
import br.com.smartdispatch.model.OrdemServico;
import br.com.smartdispatch.model.Usuario;
import br.com.smartdispatch.repository.ComentarioChamadoRepository;
import br.com.smartdispatch.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComentarioChamadoServiceTest {

    @Mock
    private ComentarioChamadoRepository comentarioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ChamadoService chamadoService;

    @Mock
    private OrdemServicoService ordemServicoService;

    @InjectMocks
    private ComentarioChamadoService comentarioChamadoService;

    @Test
    void deveLancarBadRequestQuandoDadosComentarioNaoForemInformados() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Authentication authentication = criarAuthenticationJwt(20L, "ROLE_TECNICO");

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> comentarioChamadoService.criar(contratoId, chamadoId, null, authentication)
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Os dados do comentário devem ser informados", exception.getReason());

        verifyNoInteractions(chamadoService);
        verifyNoInteractions(usuarioRepository);
        verifyNoInteractions(ordemServicoService);
        verifyNoInteractions(comentarioRepository);
    }

    @Test
    void deveLancarBadRequestQuandoTextoComentarioNaoForInformado() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Authentication authentication = criarAuthenticationJwt(20L, "ROLE_TECNICO");

        ComentarioChamadoRequest requestTextoNulo = new ComentarioChamadoRequest();
        requestTextoNulo.setTexto(null);

        ComentarioChamadoRequest requestTextoEmBranco = new ComentarioChamadoRequest();
        requestTextoEmBranco.setTexto("   ");

        // Act & Assert
        assertBadRequestNaCriacao(
                contratoId, chamadoId, requestTextoNulo, authentication,
                "O texto do comentário deve ser informado"
        );
        assertBadRequestNaCriacao(
                contratoId, chamadoId, requestTextoEmBranco, authentication,
                "O texto do comentário deve ser informado"
        );

        verifyNoInteractions(chamadoService);
        verifyNoInteractions(usuarioRepository);
        verifyNoInteractions(ordemServicoService);
        verifyNoInteractions(comentarioRepository);
    }

    @Test
    void deveLancarBadRequestQuandoComentarioUltrapassarLimite() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Authentication authentication = criarAuthenticationJwt(20L, "ROLE_TECNICO");

        ComentarioChamadoRequest request = new ComentarioChamadoRequest();
        request.setTexto(" " + "a".repeat(2001) + " ");

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> comentarioChamadoService.criar(contratoId, chamadoId, request, authentication)
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("O comentário deve possuir no máximo 2000 caracteres", exception.getReason());

        verifyNoInteractions(chamadoService);
        verifyNoInteractions(usuarioRepository);
        verifyNoInteractions(ordemServicoService);
        verifyNoInteractions(comentarioRepository);
    }

    @Test
    void deveBloquearComentarioEmChamadoFinalizadoParaNaoGestor() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;

        Chamado chamado = criarChamado(chamadoId, StatusChamado.FINALIZADO);

        when(chamadoService.buscarEntidadePorId(contratoId, chamadoId))
                .thenReturn(chamado);

        Authentication authentication = criarAuthenticationJwt(20L, "ROLE_TECNICO");

        ComentarioChamadoRequest request = new ComentarioChamadoRequest();
        request.setTexto("Comentário válido");

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> comentarioChamadoService.criar(contratoId, chamadoId, request, authentication)
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Chamado finalizado não permite novos comentários", exception.getReason());

        verifyNoInteractions(usuarioRepository);
        verifyNoInteractions(ordemServicoService);
        verifyNoInteractions(comentarioRepository);
    }

    @Test
    void deveLancarUnauthorizedQuandoAuthenticationNaoForJwt() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;

        Chamado chamado = criarChamado(chamadoId, StatusChamado.ABERTO);

        when(chamadoService.buscarEntidadePorId(contratoId, chamadoId))
                .thenReturn(chamado);

        Authentication authentication = new TestingAuthenticationToken(
                "usuario", null, List.of(new SimpleGrantedAuthority("ROLE_TECNICO"))
        );

        ComentarioChamadoRequest request = new ComentarioChamadoRequest();
        request.setTexto("Comentário válido");

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> comentarioChamadoService.criar(contratoId, chamadoId, request, authentication)
        );

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Usuário não autenticado", exception.getReason());

        verify(chamadoService).buscarEntidadePorId(contratoId, chamadoId);
        verifyNoInteractions(usuarioRepository);
        verifyNoInteractions(ordemServicoService);
        verifyNoInteractions(comentarioRepository);
    }

    @Test
    void deveLancarNotFoundQuandoAutorNaoExistir() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long usuarioId = 20L;

        Chamado chamado = criarChamado(chamadoId, StatusChamado.ABERTO);

        when(chamadoService.buscarEntidadePorId(contratoId, chamadoId))
                .thenReturn(chamado);

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.empty());

        Authentication authentication = criarAuthenticationJwt(usuarioId, "ROLE_TECNICO");

        ComentarioChamadoRequest request = new ComentarioChamadoRequest();
        request.setTexto("Comentário válido");

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> comentarioChamadoService.criar(contratoId, chamadoId, request, authentication)
        );

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Autor não encontrado", exception.getReason());

        verifyNoInteractions(ordemServicoService);
        verifyNoInteractions(comentarioRepository);
    }

    @Test
    void deveCriarComentarioSemOrdemServico() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long usuarioId = 20L;

        Chamado chamado = criarChamado(chamadoId, StatusChamado.ABERTO);
        Usuario autor = criarUsuario(usuarioId, "Autor Teste");

        when(chamadoService.buscarEntidadePorId(contratoId, chamadoId))
                .thenReturn(chamado);

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(autor));

        when(comentarioRepository.save(any(ComentarioChamado.class)))
                .thenAnswer(invocation -> {
                    ComentarioChamado comentario = invocation.getArgument(0);
                    comentario.setId(100L);
                    return comentario;
                });

        Authentication authentication = criarAuthenticationJwt(usuarioId, "ROLE_TECNICO");

        ComentarioChamadoRequest request = new ComentarioChamadoRequest();
        request.setOrdemServicoId(null);
        request.setTexto("  Comentário de teste  ");

        LocalDateTime antes = LocalDateTime.now();

        // Act
        ComentarioChamadoResponse response =
                comentarioChamadoService.criar(contratoId, chamadoId, request, authentication);

        LocalDateTime depois = LocalDateTime.now();

        // Assert
        ArgumentCaptor<ComentarioChamado> captor = ArgumentCaptor.forClass(ComentarioChamado.class);
        verify(comentarioRepository).save(captor.capture());

        ComentarioChamado salvo = captor.getValue();
        assertSame(chamado, salvo.getChamado());
        assertSame(autor, salvo.getAutor());
        assertNull(salvo.getOrdemServico());
        assertEquals("Comentário de teste", salvo.getTexto());
        assertNotNull(salvo.getDataCriacao());
        assertFalse(salvo.getDataCriacao().isBefore(antes));
        assertFalse(salvo.getDataCriacao().isAfter(depois));

        assertEquals(100L, response.getId());
        assertEquals(chamadoId, response.getChamadoId());
        assertEquals(usuarioId, response.getAutorId());
        assertEquals("Autor Teste", response.getAutorNome());
        assertNull(response.getOrdemServicoId());
        assertNull(response.getNumeroOrdemServico());
        assertEquals("Comentário de teste", response.getTexto());
        assertEquals(salvo.getDataCriacao(), response.getDataCriacao());

        verifyNoInteractions(ordemServicoService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_CTO"})
    void deveCriarComentarioVinculadoAOrdemEmChamadoFinalizadoParaGestor(String role) {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;
        Long usuarioId = 30L;
        Long ordemServicoId = 40L;

        Chamado chamado = criarChamado(chamadoId, StatusChamado.FINALIZADO);
        Usuario autor = criarUsuario(usuarioId, "Gestor Teste");
        OrdemServico ordemServico = criarOrdemServico(ordemServicoId, "OS-100");

        when(chamadoService.buscarEntidadePorId(contratoId, chamadoId))
                .thenReturn(chamado);

        when(usuarioRepository.findById(usuarioId))
                .thenReturn(Optional.of(autor));

        when(ordemServicoService.buscarEntidadePorId(contratoId, chamadoId, ordemServicoId))
                .thenReturn(ordemServico);

        when(comentarioRepository.save(any(ComentarioChamado.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Authentication authentication = criarAuthenticationJwt(usuarioId, role);

        ComentarioChamadoRequest request = new ComentarioChamadoRequest();
        request.setOrdemServicoId(ordemServicoId);
        request.setTexto("Comentário do gestor");

        // Act
        ComentarioChamadoResponse response =
                comentarioChamadoService.criar(contratoId, chamadoId, request, authentication);

        // Assert
        assertEquals(ordemServicoId, response.getOrdemServicoId());
        assertEquals("OS-100", response.getNumeroOrdemServico());
        assertEquals(chamadoId, response.getChamadoId());
        assertEquals(usuarioId, response.getAutorId());
        assertEquals("Gestor Teste", response.getAutorNome());
        assertEquals("Comentário do gestor", response.getTexto());

        verify(ordemServicoService).buscarEntidadePorId(contratoId, chamadoId, ordemServicoId);
    }

    @Test
    void deveListarComentariosDoChamadoComMapeamentoCompleto() {

        // Arrange
        Long contratoId = 1L;
        Long chamadoId = 10L;

        Chamado chamado = criarChamado(chamadoId, StatusChamado.ABERTO);
        Usuario autor1 = criarUsuario(20L, "Autor Um");
        Usuario autor2 = criarUsuario(21L, "Autor Dois");
        OrdemServico ordemServico = criarOrdemServico(40L, "OS-200");

        ComentarioChamado comentario1 = new ComentarioChamado();
        comentario1.setId(1L);
        comentario1.setChamado(chamado);
        comentario1.setAutor(autor1);
        comentario1.setOrdemServico(null);
        comentario1.setTexto("Primeiro comentário");
        comentario1.setDataCriacao(LocalDateTime.of(2026, 1, 1, 10, 0));

        ComentarioChamado comentario2 = new ComentarioChamado();
        comentario2.setId(2L);
        comentario2.setChamado(chamado);
        comentario2.setAutor(autor2);
        comentario2.setOrdemServico(ordemServico);
        comentario2.setTexto("Segundo comentário");
        comentario2.setDataCriacao(LocalDateTime.of(2026, 1, 1, 11, 0));

        when(chamadoService.buscarEntidadePorId(contratoId, chamadoId))
                .thenReturn(chamado);

        when(comentarioRepository.findByChamadoIdOrderByDataCriacaoAsc(chamadoId))
                .thenReturn(List.of(comentario1, comentario2));

        // Act
        List<ComentarioChamadoResponse> resultado =
                comentarioChamadoService.listarPorChamado(contratoId, chamadoId);

        // Assert
        assertEquals(2, resultado.size());

        ComentarioChamadoResponse response1 = resultado.get(0);
        assertEquals(1L, response1.getId());
        assertEquals(chamadoId, response1.getChamadoId());
        assertEquals(20L, response1.getAutorId());
        assertEquals("Autor Um", response1.getAutorNome());
        assertNull(response1.getOrdemServicoId());
        assertNull(response1.getNumeroOrdemServico());
        assertEquals("Primeiro comentário", response1.getTexto());
        assertEquals(comentario1.getDataCriacao(), response1.getDataCriacao());

        ComentarioChamadoResponse response2 = resultado.get(1);
        assertEquals(2L, response2.getId());
        assertEquals(40L, response2.getOrdemServicoId());
        assertEquals("OS-200", response2.getNumeroOrdemServico());
        assertEquals(21L, response2.getAutorId());
        assertEquals("Segundo comentário", response2.getTexto());
        assertEquals(comentario2.getDataCriacao(), response2.getDataCriacao());

        verify(comentarioRepository).findByChamadoIdOrderByDataCriacaoAsc(chamadoId);
    }

    // ---------------------------------------------------------------
    // Fixtures auxiliares
    // ---------------------------------------------------------------

    private Chamado criarChamado(Long id, StatusChamado status) {
        Chamado chamado = new Chamado();
        chamado.setId(id);
        chamado.setStatus(status);
        return chamado;
    }

    private Usuario criarUsuario(Long id, String nome) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        return usuario;
    }

    private OrdemServico criarOrdemServico(Long id, String numeroOrdemServico) {
        OrdemServico ordemServico = new OrdemServico();
        ordemServico.setId(id);
        ordemServico.setNumeroOrdemServico(numeroOrdemServico);
        return ordemServico;
    }

    private Jwt criarJwt(Long usuarioId) {
        return Jwt.withTokenValue("token-teste")
                .header("alg", "HS256")
                .claim("sub", "usuario@teste.com")
                .claim("usuarioId", usuarioId)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private JwtAuthenticationToken criarAuthenticationJwt(Long usuarioId, String... authorities) {
        List<GrantedAuthority> grantedAuthorities = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new JwtAuthenticationToken(criarJwt(usuarioId), grantedAuthorities);
    }

    private void assertBadRequestNaCriacao(
            Long contratoId,
            Long chamadoId,
            ComentarioChamadoRequest request,
            Authentication authentication,
            String mensagemEsperada
    ) {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> comentarioChamadoService.criar(contratoId, chamadoId, request, authentication)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(mensagemEsperada, exception.getReason());
    }
}
