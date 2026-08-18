package br.com.smartdispatch.service;

import br.com.smartdispatch.dto.StatusChamadoRequest;
import br.com.smartdispatch.enums.StatusChamado;
import br.com.smartdispatch.model.BaseOperacional;
import br.com.smartdispatch.model.Contrato;
import br.com.smartdispatch.model.Tecnico;
import br.com.smartdispatch.repository.OrdemServicoRepository;
import br.com.smartdispatch.repository.TecnicoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutorizacaoServiceTest {

    @Mock
    private OrdemServicoRepository ordemServicoRepository;

    @Mock
    private TecnicoRepository tecnicoRepository;

    @InjectMocks
    private AutorizacaoService autorizacaoService;

    // ---------------------------------------------------------------
    // podeAlterarStatusChamado()
    // ---------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_CTO"})
    void devePermitirAlteracaoDeStatusParaGestor(String role) {

        // Arrange
        Long contratoId = 1L;

        Authentication authentication = new TestingAuthenticationToken(
                "usuario", null, List.of(new SimpleGrantedAuthority(role))
        );

        StatusChamadoRequest request = new StatusChamadoRequest();
        request.setStatus(StatusChamado.FINALIZADO);

        // Act
        boolean resultado =
                autorizacaoService.podeAlterarStatusChamado(authentication, contratoId, request);

        // Assert
        assertTrue(resultado);

        verifyNoInteractions(tecnicoRepository);
    }

    @Test
    void deveNegarAlteracaoQuandoTecnicoNaoPertenceAoContrato() {

        // Arrange
        Long contratoId = 1L;
        Long usuarioId = 10L;

        Authentication authentication = criarAuthenticationJwt(usuarioId, "ROLE_TECNICO");

        when(tecnicoRepository.existsByUsuarioIdAndBaseOperacionalContratoId(usuarioId, contratoId))
                .thenReturn(false);

        StatusChamadoRequest request = new StatusChamadoRequest();
        request.setStatus(StatusChamado.PENDENTE);

        // Act
        boolean resultado =
                autorizacaoService.podeAlterarStatusChamado(authentication, contratoId, request);

        // Assert
        assertFalse(resultado);
    }

    @Test
    void devePermitirValidacaoQuandoRequestOuStatusNuloParaTecnicoDoContrato() {

        // Arrange
        Long contratoId = 1L;
        Long usuarioId = 10L;

        Authentication authentication = criarAuthenticationJwt(usuarioId, "ROLE_TECNICO");

        when(tecnicoRepository.existsByUsuarioIdAndBaseOperacionalContratoId(usuarioId, contratoId))
                .thenReturn(true);

        StatusChamadoRequest requestComStatusNulo = new StatusChamadoRequest();
        requestComStatusNulo.setStatus(null);

        // Act
        boolean resultadoRequestNulo =
                autorizacaoService.podeAlterarStatusChamado(authentication, contratoId, null);

        boolean resultadoStatusNulo =
                autorizacaoService.podeAlterarStatusChamado(authentication, contratoId, requestComStatusNulo);

        // Assert
        assertTrue(resultadoRequestNulo);
        assertTrue(resultadoStatusNulo);
    }

    @Test
    void deveNegarFinalizacaoParaTecnicoDoContrato() {

        // Arrange
        Long contratoId = 1L;
        Long usuarioId = 10L;

        Authentication authentication = criarAuthenticationJwt(usuarioId, "ROLE_TECNICO");

        when(tecnicoRepository.existsByUsuarioIdAndBaseOperacionalContratoId(usuarioId, contratoId))
                .thenReturn(true);

        StatusChamadoRequest request = new StatusChamadoRequest();
        request.setStatus(StatusChamado.FINALIZADO);

        // Act
        boolean resultado =
                autorizacaoService.podeAlterarStatusChamado(authentication, contratoId, request);

        // Assert
        assertFalse(resultado);
    }

    @Test
    void devePermitirStatusNaoFinalizadoParaTecnicoDoContrato() {

        // Arrange
        Long contratoId = 1L;
        Long usuarioId = 10L;

        Authentication authentication = criarAuthenticationJwt(usuarioId, "ROLE_TECNICO");

        when(tecnicoRepository.existsByUsuarioIdAndBaseOperacionalContratoId(usuarioId, contratoId))
                .thenReturn(true);

        StatusChamadoRequest request = new StatusChamadoRequest();
        request.setStatus(StatusChamado.PENDENTE);

        // Act
        boolean resultado =
                autorizacaoService.podeAlterarStatusChamado(authentication, contratoId, request);

        // Assert
        assertTrue(resultado);
    }

    // ---------------------------------------------------------------
    // tecnicoPertenceAoContrato()
    // ---------------------------------------------------------------

    @Test
    void deveRetornarFalseQuandoAuthenticationNaoForJwt() {

        // Arrange
        Authentication authentication = new TestingAuthenticationToken("usuario", null);

        // Act
        boolean resultado = autorizacaoService.tecnicoPertenceAoContrato(authentication, 1L);

        // Assert
        assertFalse(resultado);

        verifyNoInteractions(tecnicoRepository);
    }

    @Test
    void deveRetornarFalseQuandoJwtNaoPossuirUsuarioId() {

        // Arrange
        Authentication authentication = criarAuthenticationJwt(null, "ROLE_TECNICO");

        // Act
        boolean resultado = autorizacaoService.tecnicoPertenceAoContrato(authentication, 1L);

        // Assert
        assertFalse(resultado);

        verifyNoInteractions(tecnicoRepository);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void deveConsultarVinculoDoTecnicoComContratoERepassarResultado(boolean vinculado) {

        // Arrange
        Long contratoId = 1L;
        Long usuarioId = 10L;

        Authentication authentication = criarAuthenticationJwt(usuarioId, "ROLE_TECNICO");

        when(tecnicoRepository.existsByUsuarioIdAndBaseOperacionalContratoId(usuarioId, contratoId))
                .thenReturn(vinculado);

        // Act
        boolean resultado = autorizacaoService.tecnicoPertenceAoContrato(authentication, contratoId);

        // Assert
        assertEquals(vinculado, resultado);

        verify(tecnicoRepository).existsByUsuarioIdAndBaseOperacionalContratoId(usuarioId, contratoId);
    }

    // ---------------------------------------------------------------
    // tecnicoAtribuidoAOrdemServico()
    // ---------------------------------------------------------------

    @Test
    void deveRetornarFalseParaOrdemQuandoAuthenticationNaoForJwt() {

        // Arrange
        Authentication authentication = new TestingAuthenticationToken("usuario", null);

        // Act
        boolean resultado =
                autorizacaoService.tecnicoAtribuidoAOrdemServico(authentication, 1L, 2L, 3L);

        // Assert
        assertFalse(resultado);

        verifyNoInteractions(ordemServicoRepository);
    }

    @Test
    void deveRetornarFalseParaOrdemQuandoJwtNaoPossuirUsuarioId() {

        // Arrange
        Authentication authentication = criarAuthenticationJwt(null, "ROLE_TECNICO");

        // Act
        boolean resultado =
                autorizacaoService.tecnicoAtribuidoAOrdemServico(authentication, 1L, 2L, 3L);

        // Assert
        assertFalse(resultado);

        verifyNoInteractions(ordemServicoRepository);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void deveConsultarAtribuicaoDaOrdemERepassarResultado(boolean atribuido) {

        // Arrange
        Long usuarioId = 10L;
        Long contratoId = 1L;
        Long chamadoId = 2L;
        Long ordemServicoId = 3L;

        Authentication authentication = criarAuthenticationJwt(usuarioId, "ROLE_TECNICO");

        when(
                ordemServicoRepository
                        .existsByIdAndTecnicoUsuarioIdAndChamadoIdAndChamadoUnidadeContratoIdAndChamadoStatusNot(
                                ordemServicoId, usuarioId, chamadoId, contratoId, StatusChamado.FINALIZADO
                        )
        ).thenReturn(atribuido);

        // Act
        boolean resultado = autorizacaoService.tecnicoAtribuidoAOrdemServico(
                authentication, contratoId, chamadoId, ordemServicoId
        );

        // Assert
        assertEquals(atribuido, resultado);

        verify(ordemServicoRepository)
                .existsByIdAndTecnicoUsuarioIdAndChamadoIdAndChamadoUnidadeContratoIdAndChamadoStatusNot(
                        ordemServicoId, usuarioId, chamadoId, contratoId, StatusChamado.FINALIZADO
                );
    }

    // ---------------------------------------------------------------
    // obterContratoIdTecnico()
    // ---------------------------------------------------------------

    @Test
    void deveRetornarNullAoObterContratoQuandoAuthenticationInvalidaOuSemUsuarioId() {

        // Arrange
        Authentication authenticationNaoJwt = new TestingAuthenticationToken("usuario", null);
        Authentication authenticationSemUsuarioId = criarAuthenticationJwt(null, "ROLE_TECNICO");

        // Act
        Long resultadoNaoJwt = autorizacaoService.obterContratoIdTecnico(authenticationNaoJwt);
        Long resultadoSemUsuarioId = autorizacaoService.obterContratoIdTecnico(authenticationSemUsuarioId);

        // Assert
        assertNull(resultadoNaoJwt);
        assertNull(resultadoSemUsuarioId);

        verifyNoInteractions(tecnicoRepository);
    }

    @Test
    void deveRetornarContratoDoTecnicoAutenticado() {

        // Arrange
        Long usuarioId = 10L;
        Long contratoId = 5L;

        Contrato contrato = criarContrato(contratoId, "Cidade Teste");
        BaseOperacional base = criarBaseOperacional(6L, contrato, "Base Central");
        Tecnico tecnico = criarTecnico(20L, base, true);

        Authentication authentication = criarAuthenticationJwt(usuarioId, "ROLE_TECNICO");

        when(tecnicoRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(tecnico));

        // Act
        Long resultado = autorizacaoService.obterContratoIdTecnico(authentication);

        // Assert
        assertEquals(contratoId, resultado);

        verify(tecnicoRepository).findByUsuarioId(usuarioId);
    }

    @Test
    void deveRetornarNullQuandoUsuarioNaoPossuirTecnicoVinculado() {

        // Arrange
        Long usuarioId = 10L;

        Authentication authentication = criarAuthenticationJwt(usuarioId, "ROLE_TECNICO");

        when(tecnicoRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.empty());

        // Act
        Long resultado = autorizacaoService.obterContratoIdTecnico(authentication);

        // Assert
        assertNull(resultado);
    }

    // ---------------------------------------------------------------
    // resolverContratoPermitido()
    // ---------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_ADMIN", "ROLE_CTO"})
    void deveRetornarContratoSolicitadoParaGestor(String role) {

        // Arrange
        Long contratoIdSolicitado = 7L;

        Authentication authentication = new TestingAuthenticationToken(
                "usuario", null, List.of(new SimpleGrantedAuthority(role))
        );

        // Act
        Long resultado =
                autorizacaoService.resolverContratoPermitido(authentication, contratoIdSolicitado);

        // Assert
        assertEquals(contratoIdSolicitado, resultado);

        verifyNoInteractions(tecnicoRepository);
    }

    @Test
    void deveLancarForbiddenQuandoUsuarioNaoPossuirContratoVinculado() {

        // Arrange
        Long usuarioId = 10L;

        Authentication authentication = criarAuthenticationJwt(usuarioId, "ROLE_TECNICO");

        when(tecnicoRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.empty());

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> autorizacaoService.resolverContratoPermitido(authentication, null)
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals(
                "Usuário não possui vínculo com um contrato",
                exception.getReason()
        );
    }

    @Test
    void deveResolverContratoDoTecnicoQuandoContratoNaoForInformado() {

        // Arrange
        Long usuarioId = 10L;
        Long contratoIdTecnico = 10L;

        Contrato contrato = criarContrato(contratoIdTecnico, "Cidade Teste");
        BaseOperacional base = criarBaseOperacional(6L, contrato, "Base Central");
        Tecnico tecnico = criarTecnico(20L, base, true);

        Authentication authentication = criarAuthenticationJwt(usuarioId, "ROLE_TECNICO");

        when(tecnicoRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(tecnico));

        // Act
        Long resultado = autorizacaoService.resolverContratoPermitido(authentication, null);

        // Assert
        assertEquals(contratoIdTecnico, resultado);
    }

    @Test
    void devePermitirContratoSolicitadoQuandoForOMesmoDoTecnico() {

        // Arrange
        Long usuarioId = 10L;
        Long contratoIdTecnico = 10L;

        Contrato contrato = criarContrato(contratoIdTecnico, "Cidade Teste");
        BaseOperacional base = criarBaseOperacional(6L, contrato, "Base Central");
        Tecnico tecnico = criarTecnico(20L, base, true);

        Authentication authentication = criarAuthenticationJwt(usuarioId, "ROLE_TECNICO");

        when(tecnicoRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(tecnico));

        // Act
        Long resultado =
                autorizacaoService.resolverContratoPermitido(authentication, contratoIdTecnico);

        // Assert
        assertEquals(contratoIdTecnico, resultado);
    }

    @Test
    void deveLancarForbiddenQuandoTecnicoSolicitarOutroContrato() {

        // Arrange
        Long usuarioId = 10L;
        Long contratoIdTecnico = 10L;
        Long contratoIdSolicitado = 20L;

        Contrato contrato = criarContrato(contratoIdTecnico, "Cidade Teste");
        BaseOperacional base = criarBaseOperacional(6L, contrato, "Base Central");
        Tecnico tecnico = criarTecnico(20L, base, true);

        Authentication authentication = criarAuthenticationJwt(usuarioId, "ROLE_TECNICO");

        when(tecnicoRepository.findByUsuarioId(usuarioId))
                .thenReturn(Optional.of(tecnico));

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> autorizacaoService.resolverContratoPermitido(authentication, contratoIdSolicitado)
        );

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals(
                "Usuário não possui acesso a este contrato",
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

    private BaseOperacional criarBaseOperacional(Long id, Contrato contrato, String nome) {
        BaseOperacional base = new BaseOperacional();
        base.setId(id);
        base.setNome(nome);
        base.setContrato(contrato);
        return base;
    }

    private Tecnico criarTecnico(Long id, BaseOperacional base, boolean ativo) {
        Tecnico tecnico = new Tecnico();
        tecnico.setId(id);
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

    private JwtAuthenticationToken criarAuthenticationJwt(Long usuarioId, String... authorities) {
        List<GrantedAuthority> grantedAuthorities = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new JwtAuthenticationToken(criarJwt(usuarioId), grantedAuthorities);
    }
}
